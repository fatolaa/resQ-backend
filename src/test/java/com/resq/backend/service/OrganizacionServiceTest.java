package com.resq.backend.service;

import com.resq.backend.dto.OrganizacionRegistradaDTO;
import com.resq.backend.dto.OrganizacionRequestDTO;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.exception.OrganizacionException;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.UsuarioRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OrganizacionService: registrar organización (HU-20)")
class OrganizacionServiceTest {

    private static final long REPRESENTANTE = 10L;
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0, 0, 0};

    @TempDir
    Path carpetaLogos;

    private OrganizacionRepository organizacionRepository;
    private UsuarioRepository usuarioRepository;
    private EmailService emailService;
    private OrganizacionService servicio;

    @BeforeEach
    void setUp() {
        organizacionRepository = mock(OrganizacionRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        emailService = mock(EmailService.class);

        when(usuarioRepository.existsById(REPRESENTANTE)).thenReturn(true);
        when(emailService.enviar(anyString(), anyString(), anyString())).thenReturn(true);
        when(organizacionRepository.save(any(Organizacion.class))).thenAnswer(invocacion -> {
            Organizacion organizacion = invocacion.getArgument(0);
            organizacion.setIdOrganizacion(1L);
            organizacion.prePersist();
            return organizacion;
        });

        servicio = new OrganizacionService(
                organizacionRepository,
                usuarioRepository,
                new FotoStorageService(carpetaLogos.toString(), 1024),
                emailService,
                Validation.buildDefaultValidatorFactory().getValidator());
    }

    private static OrganizacionRequestDTO datosValidos() {
        return new OrganizacionRequestDTO(REPRESENTANTE, "Refugio Patitas", "REFUGIO",
                "Av. América 123, Cochabamba", "+591 70123456", "contacto@patitas.org",
                "Refugio de perros y gatos rescatados", null, null);
    }

    // ---------------------------------------------------------------- registro correcto

    @Test
    @DisplayName("registra la organización en estado pendiente de verificación")
    void registraComoPendienteDeVerificacion() {
        OrganizacionRegistradaDTO resultado = servicio.registrar(datosValidos(), null);

        Organizacion organizacion = resultado.organizacion();
        assertThat(organizacion.getIdOrganizacion()).isEqualTo(1L);
        assertThat(organizacion.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE);
        assertThat(organizacion.getIdRepresentante()).isEqualTo(REPRESENTANTE);
        assertThat(organizacion.getNombre()).isEqualTo("Refugio Patitas");
        assertThat(organizacion.getTipo()).isEqualTo("REFUGIO");
        assertThat(organizacion.getDireccion()).isEqualTo("Av. América 123, Cochabamba");
        assertThat(organizacion.getTelefono()).isEqualTo("+591 70123456");
        assertThat(organizacion.getEmail()).isEqualTo("contacto@patitas.org");
        assertThat(organizacion.getDescripcion()).isEqualTo("Refugio de perros y gatos rescatados");
        assertThat(organizacion.getLogoUrl()).isNull();
    }

    @Test
    @DisplayName("acepta los tres tipos de organización")
    void aceptaLosTresTipos() {
        for (String tipo : new String[]{"REFUGIO", "VETERINARIA", "RESCATISTA_INDEPENDIENTE"}) {
            OrganizacionRequestDTO d = datosValidos();
            OrganizacionRequestDTO conTipo = new OrganizacionRequestDTO(d.idRepresentante(), d.nombre(), tipo,
                    d.direccion(), d.telefono(), d.email(), d.descripcion(), null, null);

            assertThat(servicio.registrar(conTipo, null).organizacion().getTipo()).isEqualTo(tipo);
        }
    }

    @Test
    @DisplayName("normaliza espacios y mayúsculas del tipo, y la descripción es opcional")
    void normalizaYDescripcionOpcional() {
        OrganizacionRequestDTO d = new OrganizacionRequestDTO(REPRESENTANTE, "  Vet Central  ", " veterinaria ",
                "  Calle 1  ", " 70123456 ", "  vet@central.com  ", "   ", "  ", "  ");

        Organizacion organizacion = servicio.registrar(d, null).organizacion();

        assertThat(organizacion.getNombre()).isEqualTo("Vet Central");
        assertThat(organizacion.getTipo()).isEqualTo("VETERINARIA");
        assertThat(organizacion.getEmail()).isEqualTo("vet@central.com");
        assertThat(organizacion.getDescripcion()).isNull();
    }

    // ---------------------------------------------------------------- logo

    @Test
    @DisplayName("el logo subido se guarda y queda asociado a la organización")
    void guardaYAsociaElLogo() throws Exception {
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", PNG);

        Organizacion organizacion = servicio.registrar(datosValidos(), logo).organizacion();

        assertThat(organizacion.getLogoUrl()).startsWith("/api/reportes/fotos/").endsWith(".png");
        try (var archivos = Files.list(carpetaLogos)) {
            assertThat(archivos.count()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("un logo en formato no permitido se rechaza (415) y no se guarda la organización")
    void rechazaLogoConFormatoInvalido() {
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.gif", "image/gif", GIF);

        assertThatThrownBy(() -> servicio.registrar(datosValidos(), logo))
                .isInstanceOfSatisfying(OrganizacionException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                    assertThat(e.getErrores()).containsKey("logo");
                });
        verify(organizacionRepository, never()).save(any());
        verify(emailService, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("un logo que supera el tamaño máximo se rechaza (413)")
    void rechazaLogoDemasiadoGrande() {
        byte[] grande = new byte[2048];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", grande);

        assertThatThrownBy(() -> servicio.registrar(datosValidos(), logo))
                .isInstanceOfSatisfying(OrganizacionException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    // ---------------------------------------------------------------- validaciones

    @Test
    @DisplayName("los campos obligatorios faltantes se reportan todos juntos (400)")
    void reportaTodosLosCamposObligatorios() {
        OrganizacionRequestDTO vacio = new OrganizacionRequestDTO(REPRESENTANTE, null, null, "  ", "", null, null, null, null);

        assertThatThrownBy(() -> servicio.registrar(vacio, null))
                .isInstanceOfSatisfying(OrganizacionException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrores()).containsOnlyKeys("nombre", "tipo", "direccion", "telefono", "email");
                });
        verify(organizacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("falta el representante")
    void exigeRepresentante() {
        OrganizacionRequestDTO d = datosValidos();
        OrganizacionRequestDTO sinRepresentante = new OrganizacionRequestDTO(null, d.nombre(), d.tipo(),
                d.direccion(), d.telefono(), d.email(), d.descripcion(), null, null);

        assertThatThrownBy(() -> servicio.registrar(sinRepresentante, null))
                .isInstanceOfSatisfying(OrganizacionException.class,
                        e -> assertThat(e.getErrores()).containsKey("idRepresentante"));
    }

    @Test
    @DisplayName("rechaza un email con formato inválido")
    void rechazaEmailInvalido() {
        OrganizacionRequestDTO d = datosValidos();
        OrganizacionRequestDTO conEmail = new OrganizacionRequestDTO(d.idRepresentante(), d.nombre(), d.tipo(),
                d.direccion(), d.telefono(), "esto-no-es-un-email", d.descripcion(), null, null);

        assertThatThrownBy(() -> servicio.registrar(conEmail, null))
                .isInstanceOfSatisfying(OrganizacionException.class,
                        e -> assertThat(e.getErrores()).containsOnlyKeys("email"));
    }

    @Test
    @DisplayName("rechaza un teléfono con formato inválido")
    void rechazaTelefonoInvalido() {
        OrganizacionRequestDTO d = datosValidos();
        OrganizacionRequestDTO conTelefono = new OrganizacionRequestDTO(d.idRepresentante(), d.nombre(), d.tipo(),
                d.direccion(), "abc123", d.email(), d.descripcion(), null, null);

        assertThatThrownBy(() -> servicio.registrar(conTelefono, null))
                .isInstanceOfSatisfying(OrganizacionException.class,
                        e -> assertThat(e.getErrores()).containsOnlyKeys("telefono"));
    }

    @Test
    @DisplayName("rechaza un tipo que no es refugio, veterinaria ni rescatista independiente")
    void rechazaTipoDesconocido() {
        OrganizacionRequestDTO d = datosValidos();
        OrganizacionRequestDTO conTipo = new OrganizacionRequestDTO(d.idRepresentante(), d.nombre(), "TIENDA",
                d.direccion(), d.telefono(), d.email(), d.descripcion(), null, null);

        assertThatThrownBy(() -> servicio.registrar(conTipo, null))
                .isInstanceOfSatisfying(OrganizacionException.class,
                        e -> assertThat(e.getErrores()).containsOnlyKeys("tipo"));
    }

    @Test
    @DisplayName("rechaza una descripción de más de 500 caracteres")
    void rechazaDescripcionLarga() {
        OrganizacionRequestDTO d = datosValidos();
        OrganizacionRequestDTO conDescripcion = new OrganizacionRequestDTO(d.idRepresentante(), d.nombre(), d.tipo(),
                d.direccion(), d.telefono(), d.email(), "x".repeat(501), null, null);

        assertThatThrownBy(() -> servicio.registrar(conDescripcion, null))
                .isInstanceOfSatisfying(OrganizacionException.class,
                        e -> assertThat(e.getErrores()).containsOnlyKeys("descripcion"));
    }

    // ---------------------------------------------------------------- reglas de negocio

    @Test
    @DisplayName("el usuario representante debe existir (404)")
    void representanteInexistente() {
        when(usuarioRepository.existsById(REPRESENTANTE)).thenReturn(false);

        assertThatThrownBy(() -> servicio.registrar(datosValidos(), null))
                .isInstanceOfSatisfying(OrganizacionException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("un usuario no puede registrar dos organizaciones (409)")
    void unaOrganizacionPorRepresentante() {
        when(organizacionRepository.existsByIdRepresentante(REPRESENTANTE)).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(datosValidos(), null))
                .isInstanceOfSatisfying(OrganizacionException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getErrores()).containsKey("idRepresentante");
                });
        verify(organizacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("no se repite el email de una organización ya registrada (409)")
    void emailUnico() {
        when(organizacionRepository.existsByEmailIgnoreCase("contacto@patitas.org")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(datosValidos(), null))
                .isInstanceOfSatisfying(OrganizacionException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getErrores()).containsKey("email");
                });
    }

    // ---------------------------------------------------------------- confirmación por email

    @Test
    @DisplayName("envía el correo de confirmación al email de la organización")
    void enviaCorreoDeConfirmacion() {
        OrganizacionRegistradaDTO resultado = servicio.registrar(datosValidos(), null);

        ArgumentCaptor<String> destinatario = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> asunto = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviar(destinatario.capture(), asunto.capture(), cuerpo.capture());

        assertThat(destinatario.getValue()).isEqualTo("contacto@patitas.org");
        assertThat(asunto.getValue()).contains("registro");
        assertThat(cuerpo.getValue()).contains("Refugio Patitas").contains("pendiente de verificación");
        assertThat(resultado.confirmacionEnviada()).isTrue();
    }

    @Test
    @DisplayName("si el correo no se puede enviar el registro se conserva y se avisa")
    void elRegistroSobreviveAlFalloDelCorreo() {
        when(emailService.enviar(anyString(), anyString(), anyString())).thenReturn(false);

        OrganizacionRegistradaDTO resultado = servicio.registrar(datosValidos(), null);

        assertThat(resultado.confirmacionEnviada()).isFalse();
        assertThat(resultado.organizacion().getIdOrganizacion()).isEqualTo(1L);
        verify(organizacionRepository).save(any(Organizacion.class));
    }

    // ---------------------------------------------------------------- edición (HU-28)

    private static final String LOGO_VIEJO = "550e8400-e29b-41d4-a716-446655440000.png";

    private Organizacion organizacionExistente() throws Exception {
        Organizacion o = new Organizacion();
        o.setIdOrganizacion(1L);
        o.setIdRepresentante(REPRESENTANTE);
        o.setNombre("Patitas Viejas");
        o.setTipo("REFUGIO");
        o.setDireccion("Calle vieja 1");
        o.setTelefono("70123456");
        o.setEmail("viejo@patitas.org");
        o.setDescripcion("Descripción anterior");
        o.setHorarios("Lun a Vie 8-17");
        o.setZonasCobertura("Cochabamba");
        o.setLogoUrl(null);
        o.setEstadoVerificacion(EstadoVerificacion.VERIFICADA);
        when(organizacionRepository.findById(1L)).thenReturn(Optional.of(o));
        when(organizacionRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        return o;
    }

    private Organizacion conLogoViejo() throws Exception {
        Files.write(carpetaLogos.resolve(LOGO_VIEJO), PNG);
        Organizacion o = organizacionExistente();
        o.setLogoUrl("/api/reportes/fotos/" + LOGO_VIEJO);
        return o;
    }

    @Test
    @DisplayName("edita los datos y conserva representante, estado y fecha de registro")
    void editaLosDatosConservandoLoQueNoSeEdita() throws Exception {
        Organizacion existente = organizacionExistente();
        existente.setFechaRegistro(java.time.LocalDateTime.of(2024, 1, 1, 10, 0));

        Organizacion actualizada = servicio.actualizar(1L, datosValidos(), null, false);

        assertThat(actualizada.getNombre()).isEqualTo("Refugio Patitas");
        assertThat(actualizada.getDireccion()).isEqualTo("Av. América 123, Cochabamba");
        assertThat(actualizada.getTelefono()).isEqualTo("+591 70123456");
        assertThat(actualizada.getEmail()).isEqualTo("contacto@patitas.org");
        assertThat(actualizada.getTipo()).isEqualTo("REFUGIO");
        assertThat(actualizada.getIdRepresentante()).isEqualTo(REPRESENTANTE);
        assertThat(actualizada.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.VERIFICADA);
        assertThat(actualizada.getFechaRegistro()).isEqualTo(existente.getFechaRegistro());
        verify(organizacionRepository).save(actualizada);
    }

    @Test
    @DisplayName("guarda horarios y zonas de cobertura al editar")
    void guardaHorariosYZonas() throws Exception {
        organizacionExistente();
        OrganizacionRequestDTO d = datosValidos();
        OrganizacionRequestDTO conExtras = new OrganizacionRequestDTO(d.idRepresentante(), d.nombre(), d.tipo(),
                d.direccion(), d.telefono(), d.email(), d.descripcion(), "Lun-Sáb 9-18", "Cochabamba, Quillacollo");

        Organizacion actualizada = servicio.actualizar(1L, conExtras, null, false);

        assertThat(actualizada.getHorarios()).isEqualTo("Lun-Sáb 9-18");
        assertThat(actualizada.getZonasCobertura()).isEqualTo("Cochabamba, Quillacollo");
    }

    @Test
    @DisplayName("valida antes de guardar (400) y no toca la organización")
    void validaAntesDeGuardar() throws Exception {
        organizacionExistente();
        OrganizacionRequestDTO d = datosValidos();
        OrganizacionRequestDTO conEmail = new OrganizacionRequestDTO(d.idRepresentante(), d.nombre(), d.tipo(),
                d.direccion(), d.telefono(), "email-invalido", d.descripcion(), null, null);

        assertThatThrownBy(() -> servicio.actualizar(1L, conEmail, null, false))
                .isInstanceOfSatisfying(OrganizacionException.class,
                        e -> assertThat(e.getErrores()).containsOnlyKeys("email"));
        verify(organizacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("responde 404 si la organización no existe")
    void organizacionInexistente() {
        when(organizacionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(1L, datosValidos(), null, false))
                .isInstanceOfSatisfying(OrganizacionException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("solo el representante puede editar su propia organización (403)")
    void soloElRepresentanteEdita() throws Exception {
        Organizacion o = organizacionExistente();
        o.setIdRepresentante(99L);

        assertThatThrownBy(() -> servicio.actualizar(1L, datosValidos(), null, false))
                .isInstanceOfSatisfying(OrganizacionException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(organizacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("no permite usar el email de otra organización (409)")
    void emailDeOtraOrganizacionEsConflicto() throws Exception {
        organizacionExistente();
        Organizacion otra = new Organizacion();
        otra.setIdOrganizacion(2L);
        when(organizacionRepository.findByEmailIgnoreCase("contacto@patitas.org")).thenReturn(Optional.of(otra));

        assertThatThrownBy(() -> servicio.actualizar(1L, datosValidos(), null, false))
                .isInstanceOfSatisfying(OrganizacionException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(organizacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("mantener el propio email no se considera conflicto")
    void mantieneSuPropioEmail() throws Exception {
        Organizacion existente = organizacionExistente();
        when(organizacionRepository.findByEmailIgnoreCase("contacto@patitas.org")).thenReturn(Optional.of(existente));

        Organizacion actualizada = servicio.actualizar(1L, datosValidos(), null, false);

        assertThat(actualizada.getEmail()).isEqualTo("contacto@patitas.org");
        verify(organizacionRepository).save(any());
    }

    @Test
    @DisplayName("quitarLogo=true borra el archivo y deja el logo en null")
    void quitarLogoBorraElArchivo() throws Exception {
        Organizacion existente = conLogoViejo();

        Organizacion actualizada = servicio.actualizar(1L, datosValidos(), null, true);

        assertThat(actualizada.getLogoUrl()).isNull();
        assertThat(existente.getLogoUrl()).isNull();
        try (var archivos = Files.list(carpetaLogos)) {
            assertThat(archivos.count()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("subir un logo nuevo reemplaza el anterior y borra el archivo viejo")
    void reemplazaLogoYBorraElViejo() throws Exception {
        conLogoViejo();
        MockMultipartFile nuevo = new MockMultipartFile("logo", "nuevo.png", "image/png", PNG);

        Organizacion actualizada = servicio.actualizar(1L, datosValidos(), nuevo, false);

        assertThat(actualizada.getLogoUrl()).startsWith("/api/reportes/fotos/").endsWith(".png");
        assertThat(actualizada.getLogoUrl()).doesNotContain(LOGO_VIEJO);
        try (var archivos = Files.list(carpetaLogos)) {
            assertThat(archivos.count()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("sin logo nuevo ni quitarLogo, el logo actual se conserva")
    void conservaElLogoActual() throws Exception {
        conLogoViejo();

        Organizacion actualizada = servicio.actualizar(1L, datosValidos(), null, false);

        assertThat(actualizada.getLogoUrl()).isEqualTo("/api/reportes/fotos/" + LOGO_VIEJO);
        try (var archivos = Files.list(carpetaLogos)) {
            assertThat(archivos.count()).isEqualTo(1);
        }
    }

    // ---------------------------------------------------------------- no gestiona casos hasta verificar

    private void organizacionEn(String estado) {
        Organizacion organizacion = new Organizacion();
        organizacion.setIdRepresentante(REPRESENTANTE);
        organizacion.setEstadoVerificacion(estado);
        when(organizacionRepository.findByIdRepresentante(REPRESENTANTE)).thenReturn(Optional.of(organizacion));
    }

    @Test
    @DisplayName("una organización pendiente de verificación NO puede gestionar casos")
    void pendienteNoGestionaCasos() {
        organizacionEn(EstadoVerificacion.PENDIENTE);

        assertThat(servicio.puedeGestionarCasos(REPRESENTANTE)).isFalse();
    }

    @Test
    @DisplayName("una organización rechazada NO puede gestionar casos")
    void rechazadaNoGestionaCasos() {
        organizacionEn(EstadoVerificacion.RECHAZADA);

        assertThat(servicio.puedeGestionarCasos(REPRESENTANTE)).isFalse();
    }

    @Test
    @DisplayName("una organización verificada SÍ puede gestionar casos")
    void verificadaGestionaCasos() {
        organizacionEn(EstadoVerificacion.VERIFICADA);

        assertThat(servicio.puedeGestionarCasos(REPRESENTANTE)).isTrue();
    }

    @Test
    @DisplayName("un usuario sin organización, o sin id, no puede gestionar casos")
    void sinOrganizacionNoGestionaCasos() {
        when(organizacionRepository.findByIdRepresentante(99L)).thenReturn(Optional.empty());

        assertThat(servicio.puedeGestionarCasos(99L)).isFalse();
        assertThat(servicio.puedeGestionarCasos(null)).isFalse();
    }
}
