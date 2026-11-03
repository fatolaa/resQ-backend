package com.resq.backend.controller;

import com.resq.backend.config.SecurityConfig;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import com.resq.backend.security.RestAuthenticationEntryPoint;
import com.resq.backend.service.SolicitudVoluntarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-35: solicitudes para ser voluntario, probadas con el filtro JWT real.
 *
 * El filtro deja como principal el id del usuario (un String). Los tests de servicio no
 * pasaban por aca, y por eso no detectaron que el controlador esperaba un UserDetails.
 */
@WebMvcTest(SolicitudVoluntarioController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
@DisplayName("SolicitudVoluntarioController API (HU-35)")
class SolicitudVoluntarioControllerTest {

    private static final String URL = "/api/solicitudes-voluntarios";
    private static final String TOKEN_ADMIN = "token-admin";
    private static final String TOKEN_USUARIO = "token-usuario";
    private static final String CUERPO_SOLICITUD = "{\"tipoAyuda\":\"TRANSPORTE,ALIMENTO\"}";
    private static final String CUERPO_RECHAZO = "{\"motivo\":\"Faltan datos de contacto\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SolicitudVoluntarioService solicitudService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN_ADMIN)).thenReturn(1L);
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN_USUARIO)).thenReturn(2L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "ADMIN")));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "USUARIO")));
        when(solicitudService.listarPendientes()).thenReturn(List.of());
    }

    private static Usuario usuario(Long id, String rol) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombre("Cuenta " + id);
        usuario.setEmail("cuenta" + id + "@resq.com");
        usuario.setPasswordHash("hash");
        usuario.setRol(rol);
        return usuario;
    }

    // ---------------------------------------------------------------- solicitar

    @Test
    @DisplayName("un usuario con sesion crea su solicitud con su propio id (201)")
    void usuarioCreaSuSolicitud() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + TOKEN_USUARIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_SOLICITUD))
                .andExpect(status().isCreated());

        verify(solicitudService).crearSolicitud(
                argThat(usuario -> usuario.getIdUsuario().equals(2L)), eq("TRANSPORTE,ALIMENTO"), any());
    }

    @Test
    @DisplayName("sin sesion no se puede crear una solicitud (401)")
    void crearSinSesion() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO_SOLICITUD))
                .andExpect(status().isUnauthorized());

        verify(solicitudService, never()).crearSolicitud(any(), any(), any());
    }

    @Test
    @DisplayName("si la solicitud viola una regla (p. ej. el bloqueo de 30 dias) responde 409 con el motivo")
    void crearBloqueado() throws Exception {
        when(solicitudService.crearSolicitud(any(), any(), any()))
                .thenThrow(new IllegalStateException("Debes esperar 30 dias desde el ultimo rechazo"));

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + TOKEN_USUARIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_SOLICITUD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Debes esperar 30 dias desde el ultimo rechazo"));
    }

    // ---------------------------------------------------------------- revisar

    @Test
    @DisplayName("el administrador lista las solicitudes pendientes y ve el detalle")
    void adminListaYVeDetalle() throws Exception {
        mockMvc.perform(get(URL + "/pendientes").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isOk());
        mockMvc.perform(get(URL + "/7").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isOk());

        verify(solicitudService).listarPendientes();
        verify(solicitudService).obtenerDetalle(7L);
    }

    @Test
    @DisplayName("el administrador aprueba una solicitud y queda registrado su id")
    void adminAprueba() throws Exception {
        mockMvc.perform(put(URL + "/7/aprobar").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isOk());

        verify(solicitudService).aprobarSolicitud(7L, 1L);
    }

    @Test
    @DisplayName("el administrador rechaza una solicitud con motivo y queda registrado su id")
    void adminRechaza() throws Exception {
        mockMvc.perform(put(URL + "/7/rechazar")
                        .header("Authorization", "Bearer " + TOKEN_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_RECHAZO))
                .andExpect(status().isOk());

        verify(solicitudService).rechazarSolicitud(eq(7L), eq(1L), any());
    }

    @Test
    @DisplayName("aprobar una solicitud ya revisada responde 409 y no 401 (no cierra la sesion)")
    void aprobarYaRevisada() throws Exception {
        when(solicitudService.aprobarSolicitud(7L, 1L)).thenThrow(new ResponseStatusException(
                HttpStatus.CONFLICT, "La solicitud ya fue revisada: no está en estado PENDIENTE"));

        mockMvc.perform(put(URL + "/7/aprobar").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("La solicitud ya fue revisada: no está en estado PENDIENTE"));
    }

    @Test
    @DisplayName("una solicitud inexistente responde 404 y no 401")
    void solicitudInexistente() throws Exception {
        when(solicitudService.obtenerDetalle(99L)).thenThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Solicitud de voluntario no encontrada"));

        mockMvc.perform(get(URL + "/99").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Solicitud de voluntario no encontrada"));
    }
}
