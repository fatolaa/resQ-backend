package com.resq.backend.controller;

import com.resq.backend.dto.AdopcionRequestDTO;
import com.resq.backend.dto.ApiError;
import com.resq.backend.entity.Adopcion;
import com.resq.backend.exception.AdopcionException;
import com.resq.backend.service.AdopcionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AdopcionController: registrar y consultar adopciones (HU-30)")
class AdopcionControllerTest {

    private AdopcionService servicio;
    private AdopcionController controller;

    @BeforeEach
    void setUp() {
        servicio = mock(AdopcionService.class);
        controller = new AdopcionController(servicio);
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

    private static AdopcionRequestDTO datos() {
        return new AdopcionRequestDTO(20L, "Ana Pérez", "70123456", "ana@correo.com",
                LocalDate.now().minusDays(1), "Hogar con jardín");
    }

    private static Adopcion adopcion() {
        Adopcion a = new Adopcion();
        a.setIdAdopcion(1L);
        a.setIdReporte(20L);
        a.setIdOrganizacion(5L);
        return a;
    }

    // ---------------------------------------------------------------- POST

    @Test
    @DisplayName("registra y responde 201 con la adopción")
    void registraYResponde201() {
        autenticarComo("10", "USUARIO");
        Adopcion adoptada = adopcion();
        when(servicio.registrar(eq(10L), any())).thenReturn(adoptada);

        ResponseEntity<?> respuesta = controller.registrar(datos());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isSameAs(adoptada);
    }

    @Test
    @DisplayName("sin sesión responde 401 y no llama al servicio")
    void sinSesionEs401() {
        ResponseEntity<?> respuesta = controller.registrar(datos());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(servicio, never()).registrar(any(), any());
    }

    @Test
    @DisplayName("el representante es el id del token, no del cuerpo")
    void usaElIdDelToken() {
        autenticarComo("10", "USUARIO");
        when(servicio.registrar(any(), any())).thenReturn(adopcion());

        controller.registrar(datos());

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(servicio).registrar(captor.capture(), any());
        assertThat(captor.getValue()).isEqualTo(10L);
    }

    // ---------------------------------------------------------------- GET

    @Test
    @DisplayName("devuelve 200 con la adopción visible")
    void obtenerDevuelve200() {
        autenticarComo("30", "USUARIO");
        Adopcion adoptada = adopcion();
        when(servicio.obtenerVisible(1L, 30L)).thenReturn(Optional.of(adoptada));

        ResponseEntity<?> respuesta = controller.obtener(1L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(adoptada);
    }

    @Test
    @DisplayName("responde 404 si la adopción no existe")
    void obtenerInexistenteEs404() {
        autenticarComo("30", "USUARIO");
        when(servicio.obtenerVisible(1L, 30L)).thenReturn(Optional.empty());

        assertThat(controller.obtener(1L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("devuelve 200 con la adopción de un caso")
    void obtenerDeReporteDevuelve200() {
        autenticarComo("30", "USUARIO");
        Adopcion adoptada = adopcion();
        when(servicio.obtenerDeReporteVisible(20L, 30L)).thenReturn(Optional.of(adoptada));

        ResponseEntity<?> respuesta = controller.obtenerDeReporte(20L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(adoptada);
    }

    @Test
    @DisplayName("responde 404 si el caso no tiene adopción")
    void reporteSinAdopcionEs404() {
        autenticarComo("30", "USUARIO");
        when(servicio.obtenerDeReporteVisible(20L, 30L)).thenReturn(Optional.empty());

        assertThat(controller.obtenerDeReporte(20L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------------------------------------------------------------- errores

    @Test
    @DisplayName("un error de negocio se convierte en ApiError")
    void errorSeConvierteEnApiError() {
        AdopcionException error = new AdopcionException(HttpStatus.CONFLICT,
                "Este caso ya tiene una adopción registrada",
                Map.of("idReporte", "El caso ya fue adoptado"));

        ResponseEntity<ApiError> respuesta = controller.manejarError(error);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().status()).isEqualTo(409);
        assertThat(respuesta.getBody().errors()).containsKey("idReporte");
    }

    @Test
    @DisplayName("el error de visibilidad del servicio se propaga")
    void elErrorDelServicioSePropaga() {
        autenticarComo("99", "USUARIO");
        when(servicio.obtenerVisible(1L, 99L))
                .thenThrow(new AdopcionException(HttpStatus.FORBIDDEN, "No puedes consultar", Map.of("adopcion", "x")));

        assertThatThrownBy(() -> controller.obtener(1L)).isInstanceOf(AdopcionException.class);
    }
}
