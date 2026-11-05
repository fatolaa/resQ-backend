package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.DenunciaRequestDTO;
import com.resq.backend.dto.ResolucionDenunciaDTO;
import com.resq.backend.entity.DenunciaMensaje;
import com.resq.backend.entity.EstadoDenuncia;
import com.resq.backend.exception.ChatException;
import com.resq.backend.service.DenunciaService;
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
import static org.mockito.Mockito.*;

@DisplayName("DenunciaController: endpoints de denuncia de mensajes y resolucion admin (HU-40)")
class DenunciaControllerTest {

    private DenunciaService servicio;
    private DenunciaController controller;

    @BeforeEach
    void setUp() {
        servicio = mock(DenunciaService.class);
        controller = new DenunciaController(servicio);
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
    @DisplayName("denunciar sin sesion responde 401")
    void denunciarSinSesionEs401() {
        DenunciaRequestDTO dto = new DenunciaRequestDTO(100L, "Spam");
        ResponseEntity<?> respuesta = controller.denunciar(dto);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(servicio, never()).denunciar(any(), any());
    }

    @Test
    @DisplayName("denunciar con sesion exitoso responde 201 CREATED")
    void denunciarExitosoDevuelve201() {
        autenticarComo("1", "USUARIO");
        DenunciaRequestDTO dto = new DenunciaRequestDTO(100L, "Lenguaje ofensivo");
        DenunciaMensaje d = new DenunciaMensaje(100L, 1L, 2L, "Lenguaje ofensivo");
        d.setIdDenuncia(10L);

        when(servicio.denunciar(1L, dto)).thenReturn(d);

        ResponseEntity<?> respuesta = controller.denunciar(dto);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(servicio).denunciar(1L, dto);
    }

    @Test
    @DisplayName("usuario no admin no puede listar denuncias (403 FORBIDDEN)")
    void noAdminNoPuedeListarDenuncias() {
        autenticarComo("1", "USUARIO");

        ResponseEntity<?> respuesta = controller.listar(null);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(servicio, never()).listarDenuncias(any());
    }

    @Test
    @DisplayName("admin lista denuncias exitosamente (200 OK)")
    void adminListaDenunciasExitosamente() {
        autenticarComo("99", "ADMIN");
        when(servicio.listarDenuncias(EstadoDenuncia.PENDIENTE))
                .thenReturn(List.of(new DenunciaMensaje(100L, 1L, 2L, "Spam")));

        ResponseEntity<?> respuesta = controller.listar("PENDIENTE");
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(servicio).listarDenuncias(EstadoDenuncia.PENDIENTE);
    }

    @Test
    @DisplayName("usuario no admin no puede resolver denuncias (403 FORBIDDEN)")
    void noAdminNoPuedeResolverDenuncias() {
        autenticarComo("1", "USUARIO");
        ResolucionDenunciaDTO dto = new ResolucionDenunciaDTO("REVISADA", "Nota");

        ResponseEntity<?> respuesta = controller.resolver(10L, dto);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(servicio, never()).resolver(any(), any(), any());
    }

    @Test
    @DisplayName("admin resuelve denuncia exitosamente (200 OK)")
    void adminResuelveDenunciaExitosamente() {
        autenticarComo("99", "ADMIN");
        ResolucionDenunciaDTO dto = new ResolucionDenunciaDTO("REVISADA", "Usuario advertido");
        DenunciaMensaje d = new DenunciaMensaje(100L, 1L, 2L, "Spam");
        d.setIdDenuncia(10L);
        d.setEstado(EstadoDenuncia.REVISADA);

        when(servicio.resolver(10L, 99L, dto)).thenReturn(d);

        ResponseEntity<?> respuesta = controller.resolver(10L, dto);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(servicio).resolver(10L, 99L, dto);
    }

    @Test
    @DisplayName("manejo de excepciones de negocio en DenunciaController")
    void manejaChatException() {
        ChatException ex = new ChatException(HttpStatus.NOT_FOUND, "Denuncia no encontrada", Map.of());
        ResponseEntity<ApiError> respuesta = controller.manejarError(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().message()).isEqualTo("Denuncia no encontrada");
    }
}

