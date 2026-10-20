package com.resq.backend.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventoWebSocketService: publicacion y replay (HU-38a)")
class EventoWebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate mensajeria;

    private EventoReplayStore replayStore;
    private EventoWebSocketService servicio;

    @BeforeEach
    void setUp() {
        replayStore = new EventoReplayStore(50);
        servicio = new EventoWebSocketService(mensajeria, replayStore);
    }

    @Test
    @DisplayName("un evento de usuario se envia a su sala y queda en el replay")
    void eventoDeUsuarioVaASuSalaYAlReplay() {
        EventoWebSocket evento = EventoWebSocket.de(TipoEventoWebSocket.NOTIFICACION_NUEVA, "x");

        servicio.aUsuario(7L, evento);

        ArgumentCaptor<EventoWebSocket> captor = ArgumentCaptor.forClass(EventoWebSocket.class);
        verify(mensajeria).convertAndSend(eq("/topic/usuarios/7"), captor.capture());
        EventoWebSocket enviado = captor.getValue();
        assertThat(enviado.id()).isEqualTo(1L);
        assertThat(enviado.tipo()).isEqualTo(TipoEventoWebSocket.NOTIFICACION_NUEVA);
        assertThat(enviado.datos()).isEqualTo("x");
        assertThat(replayStore.ultimoId(7L)).isEqualTo(1L);
    }

    @Test
    @DisplayName("un evento de caso va a la sala del caso y no al replay personal")
    void eventoDeCasoVaALaSalaDelCaso() {
        EventoWebSocket evento = EventoWebSocket.de(TipoEventoWebSocket.CHAT_MENSAJE, "x");

        servicio.aCaso(101L, evento);

        verify(mensajeria).convertAndSend("/topic/casos/101", evento);
        assertThat(replayStore.ultimoId(101L)).isZero();
    }

    @Test
    @DisplayName("reenviarPendientes manda solo los eventos posteriores al ultimoId")
    void reenviarPendientesMandaLosPosteriores() {
        servicio.aUsuario(7L, EventoWebSocket.de("a", null));
        servicio.aUsuario(7L, EventoWebSocket.de("b", null));
        servicio.aUsuario(7L, EventoWebSocket.de("c", null));

        int reenviados = servicio.reenviarPendientes(7L, 1L);

        assertThat(reenviados).isEqualTo(2);

        ArgumentCaptor<EventoWebSocket> captor = ArgumentCaptor.forClass(EventoWebSocket.class);
        verify(mensajeria, times(5)).convertAndSend(eq("/topic/usuarios/7"), captor.capture());
        assertThat(captor.getAllValues())
                .extracting(EventoWebSocket::id)
                .containsExactly(1L, 2L, 3L, 2L, 3L);
    }
}
