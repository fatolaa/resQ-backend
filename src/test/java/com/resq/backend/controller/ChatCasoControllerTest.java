package com.resq.backend.controller;

import com.resq.backend.config.SecurityConfig;
import com.resq.backend.dto.MensajeCasoDTO;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import com.resq.backend.security.RestAuthenticationEntryPoint;
import com.resq.backend.service.ChatCasoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-22: endpoints del chat del caso, con el filtro JWT real. */
@WebMvcTest(ChatCasoController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
@DisplayName("ChatCasoController API (HU-22)")
class ChatCasoControllerTest {

    private static final String URL = "/api/casos/101/chat";
    private static final String TOKEN = "token-reportante";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatCasoService chatCasoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7L);
        usuario.setNombre("Ana Quiroga");
        usuario.setEmail("ana@resq.com");
        usuario.setPasswordHash("hash");
        usuario.setRol("USUARIO");
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN)).thenReturn(7L);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
    }

    private static MensajeCasoDTO mensaje(String tipo, String contenido) {
        return new MensajeCasoDTO(500L, 101L, 7L, "Ana Quiroga", tipo, contenido, null, null, null,
                LocalDateTime.of(2026, 11, 8, 21, 30));
    }

    @Test
    @DisplayName("sin sesion responde 401")
    void sinSesion() throws Exception {
        mockMvc.perform(get(URL + "/mensajes")).andExpect(status().isUnauthorized());
        verify(chatCasoService, never()).historial(anyLong(), anyLong());
    }

    @Test
    @DisplayName("devuelve el historial con el id del usuario del token")
    void historial() throws Exception {
        when(chatCasoService.historial(101L, 7L)).thenReturn(List.of(mensaje("TEXTO", "Hola")));

        mockMvc.perform(get(URL + "/mensajes").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contenido").value("Hola"))
                .andExpect(jsonPath("$[0].nombreEmisor").value("Ana Quiroga"));
    }

    @Test
    @DisplayName("enviar un texto responde 201 con el mensaje guardado")
    void enviarTexto() throws Exception {
        when(chatCasoService.enviar(eq(101L), eq(7L), any())).thenReturn(mensaje("TEXTO", "Voy en camino"));

        mockMvc.perform(post(URL + "/mensajes").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contenido\":\"Voy en camino\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idMensaje").value(500))
                .andExpect(jsonPath("$.tipo").value("TEXTO"));

        verify(chatCasoService).enviar(eq(101L), eq(7L), argThat(d -> "Voy en camino".equals(d.contenido())));
    }

    @Test
    @DisplayName("una latitud fuera de rango se rechaza antes de llegar al servicio (400)")
    void latitudInvalida() throws Exception {
        mockMvc.perform(post(URL + "/mensajes").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":120,\"longitud\":-66}"))
                .andExpect(status().isBadRequest());
        verify(chatCasoService, never()).enviar(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("enviar una foto por multipart responde 201")
    void enviarFoto() throws Exception {
        MockMultipartFile foto = new MockMultipartFile("foto", "perro.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(chatCasoService.enviarFoto(eq(101L), eq(7L), any(), eq("Así está"))).thenReturn(mensaje("FOTO", "Así está"));

        mockMvc.perform(multipart(URL + "/fotos").file(foto).param("comentario", "Así está")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("FOTO"));
    }

    @Test
    @DisplayName("quien no participa recibe 403 con el motivo y no se cierra su sesion (no 401)")
    void noParticipa() throws Exception {
        when(chatCasoService.historial(101L, 7L)).thenThrow(
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los involucrados en el caso pueden usar este chat"));

        mockMvc.perform(get(URL + "/mensajes").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Solo los involucrados en el caso pueden usar este chat"));
    }

    @Test
    @DisplayName("devuelve los participantes")
    void participantes() throws Exception {
        when(chatCasoService.participantes(101L, 7L)).thenReturn(List.of(
                new com.resq.backend.dto.ParticipanteChatDTO(20L, "Refugio Patitas", "ORGANIZACION", true, null)));

        mockMvc.perform(get(URL + "/participantes").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rol").value("ORGANIZACION"))
                .andExpect(jsonPath("$[0].enLinea").value(true));
    }
}
