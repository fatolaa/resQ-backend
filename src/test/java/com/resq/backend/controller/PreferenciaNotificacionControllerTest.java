package com.resq.backend.controller;

import com.resq.backend.config.SecurityConfig;
import com.resq.backend.entity.PreferenciaNotificacion;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import com.resq.backend.security.RestAuthenticationEntryPoint;
import com.resq.backend.service.PreferenciaNotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-36a: los endpoints de preferencias de notificacion son del propio usuario
 * y NO requieren rol de administrador, aunque cuelguen de /api/usuarios.
 */
@WebMvcTest(PreferenciaNotificacionController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
@DisplayName("PreferenciaNotificacionController API (HU-36a)")
class PreferenciaNotificacionControllerTest {

    private static final String TOKEN_USUARIO = "token-usuario";
    private static final long ID_USUARIO = 2L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PreferenciaNotificacionService preferenciaService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN_USUARIO)).thenReturn(ID_USUARIO);
        when(usuarioRepository.findById(ID_USUARIO))
                .thenReturn(Optional.of(usuario(ID_USUARIO, "USUARIO")));
    }

    private static Usuario usuario(Long id, String rol) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombre("Luis Usuario");
        usuario.setEmail("luis@resq.co");
        usuario.setPasswordHash("hash");
        usuario.setRol(rol);
        return usuario;
    }

    private static PreferenciaNotificacion preferencias(boolean aprobada, boolean rechazada,
                                                        boolean cerca, boolean chat) {
        PreferenciaNotificacion preferencia = new PreferenciaNotificacion(ID_USUARIO);
        preferencia.setIdPreferencia(1L);
        preferencia.setNotificarSolicitudAprobada(aprobada);
        preferencia.setNotificarSolicitudRechazada(rechazada);
        preferencia.setNotificarReporteCerca(cerca);
        preferencia.setNotificarChatMensaje(chat);
        return preferencia;
    }

    // ---------------------------------------------------------------- autenticacion

    @Test
    @DisplayName("rechaza la consulta sin sesion iniciada (401)")
    void rechazaSinSesion() throws Exception {
        mockMvc.perform(get("/api/usuarios/me/preferencias-notificacion"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rechaza la actualizacion sin sesion iniciada (401)")
    void rechazaActualizarSinSesion() throws Exception {
        mockMvc.perform(put("/api/usuarios/me/preferencias-notificacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "notificarChatMensaje": false }
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- consulta

    @Test
    @DisplayName("devuelve las preferencias del usuario autenticado")
    void devuelveLasPreferenciasDelUsuario() throws Exception {
        when(preferenciaService.obtenerOCrear(ID_USUARIO))
                .thenReturn(preferencias(true, true, true, true));

        mockMvc.perform(get("/api/usuarios/me/preferencias-notificacion")
                        .header("Authorization", "Bearer " + TOKEN_USUARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificarSolicitudAprobada").value(true))
                .andExpect(jsonPath("$.notificarSolicitudRechazada").value(true))
                .andExpect(jsonPath("$.notificarReporteCerca").value(true))
                .andExpect(jsonPath("$.notificarChatMensaje").value(true));

        verify(preferenciaService).obtenerOCrear(ID_USUARIO);
    }

    // ---------------------------------------------------------------- actualizacion

    @Test
    @DisplayName("un usuario comun (no admin) puede guardar sus preferencias")
    void unUsuarioComunPuedeGuardar() throws Exception {
        when(preferenciaService.actualizar(eq(ID_USUARIO), any()))
                .thenReturn(preferencias(true, true, true, false));

        mockMvc.perform(put("/api/usuarios/me/preferencias-notificacion")
                        .header("Authorization", "Bearer " + TOKEN_USUARIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "notificarChatMensaje": false }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificarChatMensaje").value(false));

        verify(preferenciaService).actualizar(eq(ID_USUARIO), any());
    }
}
