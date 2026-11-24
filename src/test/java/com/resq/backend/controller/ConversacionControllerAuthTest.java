package com.resq.backend.controller;

import com.resq.backend.config.SecurityConfig;
import com.resq.backend.dto.MensajeDTO;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.MensajeRepository;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import com.resq.backend.security.RestAuthenticationEntryPoint;
import com.resq.backend.service.AdjuntoMensajeStorageService;
import com.resq.backend.service.ConversacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Reproduce autenticacion HTTP del envio multipart de mensajes con el filtro JWT real. */
@WebMvcTest(ConversacionController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class ConversacionControllerAuthTest {

    private static final String TOKEN = "jwt-prueba-conversacion";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConversacionService conversacionService;

    @MockBean
    private AdjuntoMensajeStorageService adjuntoStorage;

    @MockBean
    private MensajeRepository mensajeRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void prepararSesion() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7L);
        usuario.setRol("USUARIO");
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN)).thenReturn(7L);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
    }

    @Test
    void envioMultipartConBearerValidoLlegaAlControladorAutenticado() throws Exception {
        String claveEnvio = "12345678-1234-1234-1234-123456789abc";
        when(conversacionService.enviarMensaje(7L, 2L, "Hola", null, claveEnvio))
                .thenReturn(new MensajeDTO(15L, 2L, 7L, "TEXTO", "Hola", null, null,
                        LocalDateTime.of(2026, 11, 23, 12, 0), false));

        mockMvc.perform(multipart("/api/conversaciones/2/mensajes")
                        .param("contenido", "Hola")
                        .param("claveEnvio", claveEnvio)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idMensaje").value(15))
                .andExpect(jsonPath("$.idConversacion").value(2));

        verify(conversacionService).enviarMensaje(7L, 2L, "Hola", null, claveEnvio);
    }

    @Test
    void envioMultipartSinBearerResponde401AntesDeLlamarAlServicio() throws Exception {
        mockMvc.perform(multipart("/api/conversaciones/2/mensajes")
                        .param("contenido", "Hola"))
                .andExpect(status().isUnauthorized());

        verify(conversacionService, never()).enviarMensaje(eq(7L), eq(2L), eq("Hola"), isNull(), eq(null));
    }
}
