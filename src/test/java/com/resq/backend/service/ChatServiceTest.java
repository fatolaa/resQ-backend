package com.resq.backend.service;

import com.resq.backend.dto.ConversacionDTO;
import com.resq.backend.dto.EnviarMensajeRequestDTO;
import com.resq.backend.entity.BloqueoUsuario;
import com.resq.backend.entity.Mensaje;
import com.resq.backend.entity.Usuario;
import com.resq.backend.exception.ChatException;
import com.resq.backend.repository.BloqueoUsuarioRepository;
import com.resq.backend.repository.MensajeRepository;
import com.resq.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("ChatService: conversaciones y envio de mensajes con bloqueo (HU-40)")
class ChatServiceTest {

    private MensajeRepository mensajeRepository;
    private UsuarioRepository usuarioRepository;
    private BloqueoUsuarioRepository bloqueoRepository;
    private ChatService servicio;

    @BeforeEach
    void setUp() {
        mensajeRepository = mock(MensajeRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        bloqueoRepository = mock(BloqueoUsuarioRepository.class);
        servicio = new ChatService(mensajeRepository, usuarioRepository, bloqueoRepository);
    }

    @Test
    @DisplayName("oculta conversaciones con usuarios que yo he bloqueado")
    void ocultaConversacionesBloqueadas() {
        Mensaje m1 = new Mensaje();
        m1.setIdMensaje(1L);
        m1.setIdEmisor(1L);
        m1.setIdReceptor(2L);
        m1.setContenido("Hola");
        m1.setFechaEnvio(LocalDateTime.now().minusMinutes(5));

        Mensaje m2 = new Mensaje();
        m2.setIdMensaje(2L);
        m2.setIdEmisor(3L);
        m2.setIdReceptor(1L);
        m2.setContenido("Buenas");
        m2.setFechaEnvio(LocalDateTime.now().minusMinutes(2));

        Usuario u3 = new Usuario();
        u3.setIdUsuario(3L);
        u3.setNombre("Ana");

        when(bloqueoRepository.findByIdBloqueadorOrderByFechaBloqueoDesc(1L))
                .thenReturn(List.of(new BloqueoUsuario(1L, 2L)));
        when(mensajeRepository.findAll()).thenReturn(List.of(m1, m2));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(u3));

        List<ConversacionDTO> resultado = servicio.obtenerConversaciones(1L, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).idUsuario()).isEqualTo(3L);
        assertThat(resultado.get(0).nombre()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("enviar mensaje exitosamente cuando no hay bloqueo")
    void enviarMensajeExitoso() {
        when(usuarioRepository.existsById(2L)).thenReturn(true);
        when(bloqueoRepository.existsByIdBloqueadorAndIdBloqueado(2L, 1L)).thenReturn(false);
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(i -> {
            Mensaje m = i.getArgument(0);
            m.setIdMensaje(10L);
            return m;
        });

        EnviarMensajeRequestDTO dto = new EnviarMensajeRequestDTO(2L, "Hola amigo");
        Mensaje enviado = servicio.enviarMensaje(1L, dto);

        assertThat(enviado).isNotNull();
        assertThat(enviado.getIdEmisor()).isEqualTo(1L);
        assertThat(enviado.getIdReceptor()).isEqualTo(2L);
        assertThat(enviado.getContenido()).isEqualTo("Hola amigo");
        verify(mensajeRepository).save(any(Mensaje.class));
    }

    @Test
    @DisplayName("usuario bloqueado recibe FORBIDDEN y el mensaje no se envia")
    void usuarioBloqueadoNoPuedeEnviarMensaje() {
        when(usuarioRepository.existsById(2L)).thenReturn(true);
        when(bloqueoRepository.existsByIdBloqueadorAndIdBloqueado(2L, 1L)).thenReturn(true);

        EnviarMensajeRequestDTO dto = new EnviarMensajeRequestDTO(2L, "Hola, desbloqueame");

        assertThatThrownBy(() -> servicio.enviarMensaje(1L, dto))
                .isInstanceOfSatisfying(ChatException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getMessage()).contains("bloqueado");
                });

        verify(mensajeRepository, never()).save(any());
    }

    @Test
    @DisplayName("no puede enviarse mensaje a si mismo (BAD_REQUEST)")
    void noPuedeEnviarseMensajeASiMismo() {
        EnviarMensajeRequestDTO dto = new EnviarMensajeRequestDTO(1L, "Hola yo");

        assertThatThrownBy(() -> servicio.enviarMensaje(1L, dto))
                .isInstanceOfSatisfying(ChatException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(mensajeRepository, never()).save(any());
    }
}
