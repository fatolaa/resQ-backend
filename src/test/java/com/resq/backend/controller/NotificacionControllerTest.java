package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.entity.Notificacion;
import com.resq.backend.exception.NotificacionException;
import com.resq.backend.service.NotificacionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("NotificacionController: bandeja del usuario autenticado (HU-37)")
class NotificacionControllerTest {

    private NotificacionService servicio;
    private NotificacionController controller;

    @BeforeEach
    void setUp() {
        servicio = mock(NotificacionService.class);
        controller = new NotificacionController(servicio);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private static void autenticarComo(String id, String rol) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(id, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    private static Notificacion notificacion() {
        return new Notificacion(10L, 55L, null, Notificacion.SOLICITUD_APROBADA, "Aprobado");
    }

    @Test
    @DisplayName("devuelve la bandeja del propio usuario")
    void bandejaPropia() {
        autenticarComo("10", "USUARIO");
        when(servicio.listar(10L)).thenReturn(List.of(notificacion()));

        ResponseEntity<?> respuesta = controller.obtenerPorUsuario(10L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) respuesta.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("no deja espiar la bandeja de otro usuario")
    void bandejaAjenaEs403() {
        autenticarComo("10", "USUARIO");

        ResponseEntity<?> respuesta = controller.obtenerPorUsuario(99L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(servicio, never()).listar(99L);
    }

    @Test
    @DisplayName("sin sesion responde 401")
    void sinSesionEs401() {
        ResponseEntity<?> respuesta = controller.obtenerPorUsuario(10L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(servicio, never()).listar(10L);
    }

    @Test
    @DisplayName("marca como leida usando el id del token")
    void marcaLeida() {
        autenticarComo("10", "USUARIO");
        when(servicio.marcarLeida(7L, 10L, true)).thenReturn(notificacion());

        ResponseEntity<?> respuesta = controller.marcarComoLeida(7L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(servicio).marcarLeida(7L, 10L, true);
    }

    @Test
    @DisplayName("marca como no leida usando el id del token")
    void marcaNoLeida() {
        autenticarComo("10", "USUARIO");
        when(servicio.marcarLeida(7L, 10L, false)).thenReturn(notificacion());

        ResponseEntity<?> respuesta = controller.marcarComoNoLeida(7L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(servicio).marcarLeida(7L, 10L, false);
    }

    @Test
    @DisplayName("archiva usando el id del token")
    void archiva() {
        autenticarComo("10", "USUARIO");
        when(servicio.archivar(7L, 10L)).thenReturn(notificacion());

        ResponseEntity<?> respuesta = controller.archivar(7L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(servicio).archivar(7L, 10L);
    }

    @Test
    @DisplayName("sin sesion no deja marcar ni archivar")
    void accionesSinSesionSon401() {
        assertThat(controller.marcarComoLeida(7L).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.marcarComoNoLeida(7L).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.archivar(7L).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(servicio, never()).marcarLeida(7L, 10L, true);
        verify(servicio, never()).archivar(7L, 10L);
    }

    @Test
    @DisplayName("el manejador traduce el error de negocio a ApiError")
    void manejaErrorDeNegocio() {
        NotificacionException ex = new NotificacionException(HttpStatus.FORBIDDEN,
                "No puedes gestionar las notificaciones de otro usuario", Map.of());

        ResponseEntity<ApiError> respuesta = controller.manejarError(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().status()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }
}
