package com.resq.backend.service;

import com.resq.backend.dto.AdopcionRequestDTO;
import com.resq.backend.entity.Adopcion;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.Reporte;
import com.resq.backend.exception.AdopcionException;
import com.resq.backend.repository.AdopcionRepository;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.ReporteRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AdopcionService: registrar y consultar adopciones (HU-30)")
class AdopcionServiceTest {

    private static final long REPRESENTANTE = 10L;
    private static final long ORG_ID = 5L;
    private static final long REPORTE_ID = 20L;
    private static final long REPORTANTE = 30L;
    private static final long AJENO = 99L;

    private AdopcionRepository adopcionRepository;
    private ReporteRepository reporteRepository;
    private OrganizacionRepository organizacionRepository;
    private AdopcionService servicio;

    @BeforeEach
    void setUp() {
        adopcionRepository = mock(AdopcionRepository.class);
        reporteRepository = mock(ReporteRepository.class);
        organizacionRepository = mock(OrganizacionRepository.class);
        when(adopcionRepository.save(any(Adopcion.class))).thenAnswer(invocacion -> {
            Adopcion adopcion = invocacion.getArgument(0);
            adopcion.setIdAdopcion(1L);
            adopcion.prePersist();
            return adopcion;
        });
        servicio = new AdopcionService(
                adopcionRepository,
                reporteRepository,
                organizacionRepository,
                Validation.buildDefaultValidatorFactory().getValidator());
    }

    private static Organizacion organizacion(String estado) {
        Organizacion o = new Organizacion();
        o.setIdOrganizacion(ORG_ID);
        o.setIdRepresentante(REPRESENTANTE);
        o.setEstadoVerificacion(estado);
        return o;
    }

    private static Reporte reporte(String estado) {
        Reporte r = new Reporte();
        r.setIdReporte(REPORTE_ID);
        r.setIdUsuario(REPORTANTE);
        r.setEstado(estado);
        return r;
    }

    private static AdopcionRequestDTO datos() {
        return new AdopcionRequestDTO(REPORTE_ID, "Ana Pérez", "70123456",
                "ana@correo.com", LocalDate.now().minusDays(2), "Hogar con jardín");
    }

    private void organizacionVerificada() {
        when(organizacionRepository.findByIdRepresentante(REPRESENTANTE))
                .thenReturn(Optional.of(organizacion(EstadoVerificacion.VERIFICADA)));
    }

    private void casoResuelto() {
        when(reporteRepository.findById(REPORTE_ID)).thenReturn(Optional.of(reporte("RESUELTO")));
    }

    private Adopcion adopcionExistente() {
        Adopcion a = new Adopcion();
        a.setIdAdopcion(1L);
        a.setIdReporte(REPORTE_ID);
        a.setIdOrganizacion(ORG_ID);
        return a;
    }

    // ---------------------------------------------------------------- registro correcto

    @Test
    @DisplayName("registra la adopción con los datos del adoptante y la fecha")
    void registraLaAdopcion() {
        organizacionVerificada();
        casoResuelto();
        when(adopcionRepository.existsByIdReporte(REPORTE_ID)).thenReturn(false);

        Adopcion adopcion = servicio.registrar(REPRESENTANTE, datos());

        assertThat(adopcion.getIdAdopcion()).isEqualTo(1L);
        assertThat(adopcion.getIdReporte()).isEqualTo(REPORTE_ID);
        assertThat(adopcion.getNombreAdoptante()).isEqualTo("Ana Pérez");
        assertThat(adopcion.getTelefonoAdoptante()).isEqualTo("70123456");
        assertThat(adopcion.getEmailAdoptante()).isEqualTo("ana@correo.com");
        assertThat(adopcion.getFechaAdopcion()).isEqualTo(LocalDate.now().minusDays(2));
        assertThat(adopcion.getObservaciones()).isEqualTo("Hogar con jardín");
    }

    @Test
    @DisplayName("asocia la adopción a la organización del representante autenticado")
    void asociaLaOrganizacionDelRepresentante() {
        organizacionVerificada();
        casoResuelto();
        when(adopcionRepository.existsByIdReporte(REPORTE_ID)).thenReturn(false);

        Adopcion adopcion = servicio.registrar(REPRESENTANTE, datos());

        assertThat(adopcion.getIdOrganizacion()).isEqualTo(ORG_ID);
    }

    @Test
    @DisplayName("deja la fecha de registro de la adopción")
    void dejaFechaDeRegistro() {
        organizacionVerificada();
        casoResuelto();
        when(adopcionRepository.existsByIdReporte(REPORTE_ID)).thenReturn(false);

        assertThat(servicio.registrar(REPRESENTANTE, datos()).getFechaRegistro()).isNotNull();
    }

    @Test
    @DisplayName("guarda la adopción una sola vez")
    void guardaUnaSolaVez() {
        organizacionVerificada();
        casoResuelto();
        when(adopcionRepository.existsByIdReporte(REPORTE_ID)).thenReturn(false);

        servicio.registrar(REPRESENTANTE, datos());

        ArgumentCaptor<Adopcion> captor = ArgumentCaptor.forClass(Adopcion.class);
        verify(adopcionRepository).save(captor.capture());
        assertThat(captor.getValue().getIdOrganizacion()).isEqualTo(ORG_ID);
    }

    @Test
    @DisplayName("normaliza espacios del formulario")
    void normalizaEspacios() {
        organizacionVerificada();
        casoResuelto();
        when(adopcionRepository.existsByIdReporte(REPORTE_ID)).thenReturn(false);
        AdopcionRequestDTO conEspacios = new AdopcionRequestDTO(REPORTE_ID, "  Ana Pérez  ",
                " 70123456 ", " ana@correo.com ", LocalDate.now(), "  lista  ");

        Adopcion adopcion = servicio.registrar(REPRESENTANTE, conEspacios);

        assertThat(adopcion.getNombreAdoptante()).isEqualTo("Ana Pérez");
        assertThat(adopcion.getTelefonoAdoptante()).isEqualTo("70123456");
        assertThat(adopcion.getEmailAdoptante()).isEqualTo("ana@correo.com");
        assertThat(adopcion.getObservaciones()).isEqualTo("lista");
    }

    @Test
    @DisplayName("el email y las observaciones son opcionales")
    void emailYObservacionesOpcionales() {
        organizacionVerificada();
        casoResuelto();
        when(adopcionRepository.existsByIdReporte(REPORTE_ID)).thenReturn(false);
        AdopcionRequestDTO sinOpcionales = new AdopcionRequestDTO(REPORTE_ID, "Ana", "70123456",
                "  ", LocalDate.now(), null);

        Adopcion adopcion = servicio.registrar(REPRESENTANTE, sinOpcionales);

        assertThat(adopcion.getEmailAdoptante()).isNull();
        assertThat(adopcion.getObservaciones()).isNull();
    }

    // ---------------------------------------------------------------- reglas de negocio

    @Test
    @DisplayName("un usuario sin organización no puede registrar (403)")
    void exigeOrganizacion() {
        when(organizacionRepository.findByIdRepresentante(AJENO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(AJENO, datos()))
                .isInstanceOfSatisfying(AdopcionException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(adopcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("una organización no verificada no puede registrar (403)")
    void exigeOrganizacionVerificada() {
        when(organizacionRepository.findByIdRepresentante(REPRESENTANTE))
                .thenReturn(Optional.of(organizacion(EstadoVerificacion.PENDIENTE)));

        assertThatThrownBy(() -> servicio.registrar(REPRESENTANTE, datos()))
                .isInstanceOfSatisfying(AdopcionException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(adopcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("el caso debe existir (404)")
    void exigeCasoExistente() {
        organizacionVerificada();
        when(reporteRepository.findById(REPORTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(REPRESENTANTE, datos()))
                .isInstanceOfSatisfying(AdopcionException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getErrores()).containsKey("idReporte");
                });
    }

    @Test
    @DisplayName("solo se adopta un caso cerrado (409)")
    void exigeCasoCerrado() {
        organizacionVerificada();
        when(reporteRepository.findById(REPORTE_ID)).thenReturn(Optional.of(reporte("EN_PROCESO")));

        assertThatThrownBy(() -> servicio.registrar(REPRESENTANTE, datos()))
                .isInstanceOfSatisfying(AdopcionException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getErrores()).containsKey("idReporte");
                });
        verify(adopcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("un caso ya adoptado no se puede adoptar de nuevo (409)")
    void noPermiteDosAdopcionesDelMismoCaso() {
        organizacionVerificada();
        casoResuelto();
        when(adopcionRepository.existsByIdReporte(REPORTE_ID)).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(REPRESENTANTE, datos()))
                .isInstanceOfSatisfying(AdopcionException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(adopcionRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- validación

    @Test
    @DisplayName("sin datos obligatorios responde 400 con los campos")
    void validaCamposObligatorios() {
        AdopcionRequestDTO vacio = new AdopcionRequestDTO(null, "  ", null, null, null, null);

        assertThatThrownBy(() -> servicio.registrar(REPRESENTANTE, vacio))
                .isInstanceOfSatisfying(AdopcionException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrores()).containsKeys(
                            "idReporte", "nombreAdoptante", "telefonoAdoptante", "fechaAdopcion");
                });
        verify(adopcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("una fecha de adopción futura es inválida (400)")
    void fechaFuturaEsInvalida() {
        AdopcionRequestDTO futura = new AdopcionRequestDTO(REPORTE_ID, "Ana", "70123456",
                null, LocalDate.now().plusDays(1), null);

        assertThatThrownBy(() -> servicio.registrar(REPRESENTANTE, futura))
                .isInstanceOfSatisfying(AdopcionException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrores()).containsKey("fechaAdopcion");
                });
    }

    @Test
    @DisplayName("un email con formato inválido es rechazado (400)")
    void emailInvalidoEsRechazado() {
        AdopcionRequestDTO conEmail = new AdopcionRequestDTO(REPORTE_ID, "Ana", "70123456",
                "correo-invalido", LocalDate.now(), null);

        assertThatThrownBy(() -> servicio.registrar(REPRESENTANTE, conEmail))
                .isInstanceOfSatisfying(AdopcionException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrores()).containsKey("emailAdoptante");
                });
    }

    @Test
    @DisplayName("valida antes de tocar la base de datos")
    void validaAntesDeConsultar() {
        AdopcionRequestDTO invalido = new AdopcionRequestDTO(null, null, null, null, null, null);

        assertThatThrownBy(() -> servicio.registrar(REPRESENTANTE, invalido))
                .isInstanceOf(AdopcionException.class);

        verify(organizacionRepository, never()).findByIdRepresentante(any());
        verify(reporteRepository, never()).findById(any());
    }

    // ---------------------------------------------------------------- visibilidad

    @Test
    @DisplayName("quien reportó el caso puede consultar la adopción")
    void reportantePuedeConsultar() {
        when(adopcionRepository.findById(1L)).thenReturn(Optional.of(adopcionExistente()));
        when(reporteRepository.findById(REPORTE_ID)).thenReturn(Optional.of(reporte("RESUELTO")));

        assertThat(servicio.obtenerVisible(1L, REPORTANTE)).isPresent();
    }

    @Test
    @DisplayName("la organización que adoptó puede consultar la adopción")
    void organizacionPuedeConsultar() {
        when(adopcionRepository.findById(1L)).thenReturn(Optional.of(adopcionExistente()));
        when(reporteRepository.findById(REPORTE_ID)).thenReturn(Optional.of(reporte("RESUELTO")));
        when(organizacionRepository.findById(ORG_ID))
                .thenReturn(Optional.of(organizacion(EstadoVerificacion.VERIFICADA)));

        assertThat(servicio.obtenerVisible(1L, REPRESENTANTE)).isPresent();
    }

    @Test
    @DisplayName("un usuario ajeno al caso no puede consultar (403)")
    void ajenoNoPuedeConsultar() {
        when(adopcionRepository.findById(1L)).thenReturn(Optional.of(adopcionExistente()));
        when(reporteRepository.findById(REPORTE_ID)).thenReturn(Optional.of(reporte("RESUELTO")));
        when(organizacionRepository.findById(ORG_ID))
                .thenReturn(Optional.of(organizacion(EstadoVerificacion.VERIFICADA)));

        assertThatThrownBy(() -> servicio.obtenerVisible(1L, AJENO))
                .isInstanceOfSatisfying(AdopcionException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("sin sesión no se puede consultar (403)")
    void sinSesionNoPuedeConsultar() {
        when(adopcionRepository.findById(1L)).thenReturn(Optional.of(adopcionExistente()));

        assertThatThrownBy(() -> servicio.obtenerVisible(1L, null))
                .isInstanceOfSatisfying(AdopcionException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("una adopción inexistente no se encuentra (vacío)")
    void adopcionInexistenteEsVacia() {
        when(adopcionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(servicio.obtenerVisible(1L, REPORTANTE)).isEmpty();
    }

    @Test
    @DisplayName("sin id no se consulta la base de datos")
    void sinIdNoConsulta() {
        assertThat(servicio.obtenerVisible(null, REPORTANTE)).isEmpty();
        verify(adopcionRepository, never()).findById(any());
    }

    @Test
    @DisplayName("la adopción de un caso la ve quien reportó")
    void adopcionDeCasoLaVeElReportante() {
        when(adopcionRepository.findByIdReporte(REPORTE_ID)).thenReturn(Optional.of(adopcionExistente()));
        when(reporteRepository.findById(REPORTE_ID)).thenReturn(Optional.of(reporte("RESUELTO")));

        assertThat(servicio.obtenerDeReporteVisible(REPORTE_ID, REPORTANTE)).isPresent();
    }

    @Test
    @DisplayName("la adopción de un caso no la ve un ajeno (403)")
    void adopcionDeCasoAjenaEsRechazada() {
        when(adopcionRepository.findByIdReporte(REPORTE_ID)).thenReturn(Optional.of(adopcionExistente()));
        when(reporteRepository.findById(REPORTE_ID)).thenReturn(Optional.of(reporte("RESUELTO")));
        when(organizacionRepository.findById(ORG_ID))
                .thenReturn(Optional.of(organizacion(EstadoVerificacion.VERIFICADA)));

        assertThatThrownBy(() -> servicio.obtenerDeReporteVisible(REPORTE_ID, AJENO))
                .isInstanceOfSatisfying(AdopcionException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("un caso sin adopción devuelve vacío")
    void casoSinAdopcionEsVacio() {
        when(adopcionRepository.findByIdReporte(REPORTE_ID)).thenReturn(Optional.empty());

        assertThat(servicio.obtenerDeReporteVisible(REPORTE_ID, REPORTANTE)).isEmpty();
    }
}
