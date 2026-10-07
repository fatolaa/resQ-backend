package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.OrganizacionRegistradaDTO;
import com.resq.backend.dto.OrganizacionRequestDTO;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.exception.OrganizacionException;
import com.resq.backend.service.OrganizacionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OrganizacionController: registro y consulta (HU-20)")
class OrganizacionControllerTest {

    private OrganizacionService servicio;
    private OrganizacionController controller;

    @BeforeEach
    void setUp() {
        servicio = mock(OrganizacionService.class);
        controller = new OrganizacionController(servicio);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private static void autenticarComo(String id, String rol) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(id, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    private static Organizacion organizacion(String estado) {
        Organizacion organizacion = new Organizacion();
        organizacion.setIdOrganizacion(1L);
        organizacion.setIdRepresentante(7L);
        organizacion.setEstadoVerificacion(estado);
        return organizacion;
    }

    private ResponseEntity<?> registrar(Long idRepresentante, MultipartFile logo) {
        return controller.registrar(idRepresentante, "Refugio Patitas", "REFUGIO", "Calle 1", "70123456",
                "a@patitas.org", "desc", logo);
    }

    private OrganizacionRequestDTO datosEnviadosAlServicio(MultipartFile logoEsperado) {
        ArgumentCaptor<OrganizacionRequestDTO> captor = ArgumentCaptor.forClass(OrganizacionRequestDTO.class);
        verify(servicio).registrar(captor.capture(), org.mockito.ArgumentMatchers.eq(logoEsperado));
        return captor.getValue();
    }

    // ---------------------------------------------------------------- POST

    @Test
    @DisplayName("registra y responde 201 con la organización y si salió el correo")
    void registraYResponde201() {
        OrganizacionRegistradaDTO registrada = new OrganizacionRegistradaDTO(organizacion(EstadoVerificacion.PENDIENTE), true);
        when(servicio.registrar(any(), any())).thenReturn(registrada);

        ResponseEntity<?> respuesta = registrar(7L, null);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(registrada);
    }

    @Test
    @DisplayName("sin sesión con token, usa el idRepresentante que manda el cliente")
    void sinTokenUsaElIdDelCliente() {
        when(servicio.registrar(any(), any())).thenReturn(new OrganizacionRegistradaDTO(organizacion("X"), true));

        registrar(7L, null);

        assertThat(datosEnviadosAlServicio(null).idRepresentante()).isEqualTo(7L);
    }

    @Test
    @DisplayName("una petición anónima no se confunde con un usuario autenticado")
    void anonimoNoCuentaComoUsuario() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "clave", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        when(servicio.registrar(any(), any())).thenReturn(new OrganizacionRegistradaDTO(organizacion("X"), true));

        registrar(7L, null);

        assertThat(datosEnviadosAlServicio(null).idRepresentante()).isEqualTo(7L);
    }

    @Test
    @DisplayName("con token válido, el representante es el del token aunque el cliente no mande id")
    void conTokenUsaElIdDelToken() {
        autenticarComo("7", "USUARIO");
        when(servicio.registrar(any(), any())).thenReturn(new OrganizacionRegistradaDTO(organizacion("X"), true));

        registrar(null, null);

        assertThat(datosEnviadosAlServicio(null).idRepresentante()).isEqualTo(7L);
    }

    @Test
    @DisplayName("con token, no se puede registrar una organización a nombre de otro usuario (403)")
    void noSePuedeSuplantarOtroUsuario() {
        autenticarComo("7", "USUARIO");

        ResponseEntity<?> respuesta = registrar(99L, null);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(servicio, never()).registrar(any(), any());
    }

    @Test
    @DisplayName("el logo recibido se pasa al servicio junto con los datos")
    void pasaElLogoAlServicio() {
        MultipartFile logo = mock(MultipartFile.class);
        when(servicio.registrar(any(), any())).thenReturn(new OrganizacionRegistradaDTO(organizacion("X"), true));

        registrar(7L, logo);

        OrganizacionRequestDTO datos = datosEnviadosAlServicio(logo);
        assertThat(datos.nombre()).isEqualTo("Refugio Patitas");
        assertThat(datos.tipo()).isEqualTo("REFUGIO");
        assertThat(datos.email()).isEqualTo("a@patitas.org");
    }

    @Test
    @DisplayName("los errores de negocio se convierten en ApiError con su código y detalle por campo")
    void convierteLosErroresEnApiError() {
        OrganizacionException error = new OrganizacionException(HttpStatus.BAD_REQUEST,
                "Revisa los datos del formulario", Map.of("email", "El email no tiene un formato válido"));

        ResponseEntity<ApiError> respuesta = controller.manejarError(error);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().status()).isEqualTo(400);
        assertThat(respuesta.getBody().message()).isEqualTo("Revisa los datos del formulario");
        assertThat(respuesta.getBody().errors()).containsEntry("email", "El email no tiene un formato válido");
    }

    // ---------------------------------------------------------------- GET representante

    @Test
    @DisplayName("devuelve la organización del representante con su estado de verificación")
    void devuelveLaOrganizacionDelRepresentante() {
        Organizacion pendiente = organizacion(EstadoVerificacion.PENDIENTE);
        when(servicio.obtenerDeRepresentante(7L)).thenReturn(Optional.of(pendiente));

        ResponseEntity<?> respuesta = controller.obtenerDeRepresentante(7L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(pendiente);
    }

    @Test
    @DisplayName("responde 404 si el usuario no tiene organización")
    void sinOrganizacionEs404() {
        when(servicio.obtenerDeRepresentante(7L)).thenReturn(Optional.empty());

        assertThat(controller.obtenerDeRepresentante(7L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("con token, un usuario no puede ver la organización de otro (403)")
    void noVeLaOrganizacionDeOtro() {
        autenticarComo("8", "USUARIO");

        assertThat(controller.obtenerDeRepresentante(7L).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(servicio, never()).obtenerDeRepresentante(any());
    }

    @Test
    @DisplayName("con token, el dueño y el administrador sí pueden consultarla")
    void duenoYAdminPuedenConsultar() {
        when(servicio.obtenerDeRepresentante(7L)).thenReturn(Optional.of(organizacion(EstadoVerificacion.PENDIENTE)));

        autenticarComo("7", "USUARIO");
        assertThat(controller.obtenerDeRepresentante(7L).getStatusCode()).isEqualTo(HttpStatus.OK);

        autenticarComo("1", "ADMIN");
        assertThat(controller.obtenerDeRepresentante(7L).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
