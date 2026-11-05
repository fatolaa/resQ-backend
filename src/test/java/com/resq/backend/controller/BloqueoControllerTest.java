package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.BloqueoDTO;
import com.resq.backend.entity.BloqueoUsuario;
import com.resq.backend.exception.ChatException;
import com.resq.backend.service.BloqueoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("BloqueoController: endpoints de bloqueo y desbloqueo (HU-40)")
class BloqueoControllerTest {

    private BloqueoService servicio;
    private BloqueoController controller;

    @BeforeEach
    void setUp() {
        servicio = mock(BloqueoService.class);
        controller = new BloqueoController(servicio);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void autenticarComo(String id, String rol) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(id, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    @Test
    @DisplayName("bloquear sin sesion responde 401")
    void bloquearSinSesionEs401() {
        ResponseEntity<?> respuesta = controller.bloquear(2L);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(servicio, never()).bloquear(any(), any());
    }

    @Test
    @DisplayName("bloquear con sesion exitoso responde 201 CREATED")
    void bloquearExitosoDevuelve201() {
        autenticarComo("1", "USUARIO");
        BloqueoUsuario b = new BloqueoUsuario(1L, 2L);
        b.setIdBloqueo(10L);

        when(servicio.bloquear(1L, 2L)).thenReturn(b);

        ResponseEntity<?> respuesta = controller.bloquear(2L);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(servicio).bloquear(1L, 2L);
    }

    @Test
    @DisplayName("desbloquear responde 204 NO_CONTENT")
    void desbloquearDevuelve204() {
        autenticarComo("1", "USUARIO");
        doNothing().when(servicio).desbloquear(1L, 2L);

        ResponseEntity<?> respuesta = controller.desbloquear(2L);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(servicio).desbloquear(1L, 2L);
    }

    @Test
    @DisplayName("listar bloqueos del usuario autenticado responde 200 OK")
    void listarBloqueosDevuelve200() {
        autenticarComo("1", "USUARIO");
        BloqueoDTO dto = new BloqueoDTO(1L, 2L, "Carlos", LocalDateTime.now());
        when(servicio.listarBloqueados(1L)).thenReturn(List.of(dto));

        ResponseEntity<?> respuesta = controller.listar();
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) respuesta.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("el manejador de excepciones traduce ChatException a ApiError")
    void manejaChatException() {
        ChatException ex = new ChatException(HttpStatus.BAD_REQUEST, "No puedes bloquearte", Map.of());
        ResponseEntity<ApiError> respuesta = controller.manejarError(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().message()).isEqualTo("No puedes bloquearte");
    }
}

