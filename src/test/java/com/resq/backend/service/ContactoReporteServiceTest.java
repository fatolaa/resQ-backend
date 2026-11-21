package com.resq.backend.service;

import com.resq.backend.dto.ContactoReporteDTO;
import com.resq.backend.dto.ContactoReporteDTO.Telefono;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.Reporte;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.ReporteRepository;
import com.resq.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ContactoReporteService: contacto opcional de quien reporta")
class ContactoReporteServiceTest {

    private ReporteRepository reporteRepository;
    private UsuarioRepository usuarioRepository;
    private OrganizacionRepository organizacionRepository;
    private ContactoReporteService servicio;

    @BeforeEach
    void setUp() {
        reporteRepository = mock(ReporteRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        organizacionRepository = mock(OrganizacionRepository.class);
        servicio = new ContactoReporteService(reporteRepository, usuarioRepository, organizacionRepository);
        when(organizacionRepository.findByIdRepresentante(org.mockito.ArgumentMatchers.anyLong())).thenReturn(Optional.empty());
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private static void sesionDe(long idUsuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(String.valueOf(idUsuario), null, List.of()));
    }

    private static Usuario usuario(long id, String nombre, String telefono, String rol) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setNombre(nombre);
        u.setTelefono(telefono);
        u.setRol(rol);
        return u;
    }

    private static Reporte reporte(Boolean compartir, String telefonoCaso, String estado) {
        Reporte r = new Reporte();
        r.setIdReporte(7L);
        r.setIdUsuario(2L);
        r.setTipoCaso("PERDIDA");
        r.setEstado(estado);
        r.setCompartirTelefonoCuenta(compartir);
        r.setTelefonoContacto(telefonoCaso);
        return r;
    }

    private static HttpStatus estado(Throwable e) {
        return HttpStatus.valueOf(((ResponseStatusException) e).getStatusCode().value());
    }

    // ---------------------------------------------------------------- al crear

    @Test
    @DisplayName("limpia el telefono del caso y deja la casilla en false si no se marco")
    void limpiaTelefonoDelCaso() {
        Reporte r = reporte(null, "7 123-4567", "PENDIENTE");

        servicio.prepararParaGuardar(r);

        assertThat(r.getTelefonoContacto()).isEqualTo("71234567");
        assertThat(r.getCompartirTelefonoCuenta()).isFalse();
    }

    @Test
    @DisplayName("un telefono del caso invalido responde 400")
    void telefonoInvalido() {
        assertThatThrownBy(() -> servicio.prepararParaGuardar(reporte(false, "abc1234", "PENDIENTE")))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("compartir el telefono de la cuenta exige que el reporte sea de quien tiene la sesion")
    void soloElDuenoCompartesuTelefono() {
        sesionDe(9);

        assertThatThrownBy(() -> servicio.prepararParaGuardar(reporte(true, null, "PENDIENTE")))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("compartir el telefono de una cuenta sin telefono responde 400")
    void cuentaSinTelefono() {
        sesionDe(2);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2, "Ana", null, "USUARIO")));

        assertThatThrownBy(() -> servicio.prepararParaGuardar(reporte(true, null, "PENDIENTE")))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("el dueño con telefono puede compartirlo")
    void duenoComparte() {
        sesionDe(2);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2, "Ana", "71234567", "USUARIO")));
        Reporte r = reporte(true, null, "PENDIENTE");

        servicio.prepararParaGuardar(r);

        assertThat(r.getCompartirTelefonoCuenta()).isTrue();
    }

    // ---------------------------------------------------------------- al consultar

    @Test
    @DisplayName("devuelve el telefono de la cuenta (leido al consultar) y el del caso")
    void ambosTelefonos() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte(true, "60011122", "PENDIENTE")));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2, "Ana Quiroga", "7 123 4567", "USUARIO")));

        ContactoReporteDTO contacto = servicio.contactoDe(7L, 9L).orElseThrow();

        assertThat(contacto.nombre()).isEqualTo("Ana Quiroga");
        assertThat(contacto.insignia()).isNull();
        assertThat(contacto.telefonos()).containsExactly(
                new Telefono("71234567", Telefono.CUENTA), new Telefono("60011122", Telefono.CASO));
    }

    @Test
    @DisplayName("si el telefono del caso es el mismo que el de la cuenta no se repite")
    void sinRepetir() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte(true, "71234567", "EN_PROCESO")));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2, "Ana", "71234567", "USUARIO")));

        assertThat(servicio.contactoDe(7L, 9L).orElseThrow().telefonos()).hasSize(1);
    }

    @Test
    @DisplayName("sin contacto compartido, o con el caso cerrado, no hay contacto")
    void sinContacto() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte(false, null, "PENDIENTE")));
        assertThat(servicio.contactoDe(7L, 9L)).isEmpty();

        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte(true, "60011122", "RESUELTO")));
        assertThat(servicio.contactoDe(7L, 9L)).isEmpty();
    }

    @Test
    @DisplayName("si la cuenta ya no tiene telefono y no hay otro, no hay contacto")
    void cuentaQuitoSuTelefono() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte(true, null, "PENDIENTE")));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2, "Ana", null, "USUARIO")));

        assertThat(servicio.contactoDe(7L, 9L)).isEmpty();
    }

    @Test
    @DisplayName("la insignia es VOLUNTARIO u ORGANIZACION verificada, nunca el rol interno")
    void insignias() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte(false, "60011122", "PENDIENTE")));

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2, "Sara", null, "VOLUNTARIO")));
        assertThat(servicio.contactoDe(7L, 9L).orElseThrow().insignia()).isEqualTo("VOLUNTARIO");

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2, "Admin", null, "ADMIN")));
        assertThat(servicio.contactoDe(7L, 9L).orElseThrow().insignia()).isNull();

        Organizacion org = new Organizacion();
        org.setEstadoVerificacion(EstadoVerificacion.VERIFICADA);
        when(organizacionRepository.findByIdRepresentante(2L)).thenReturn(Optional.of(org));
        assertThat(servicio.contactoDe(7L, 9L).orElseThrow().insignia()).isEqualTo("ORGANIZACION");
    }

    @Test
    @DisplayName("un caso inexistente responde 404")
    void casoInexistente() {
        when(reporteRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.contactoDe(404L, 9L))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
