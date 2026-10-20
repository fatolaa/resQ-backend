package com.resq.backend.websocket;

import com.resq.backend.websocket.EventoReplayStore.ReplayEvento;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * HU-38a: punto unico por el que se emiten eventos a las salas. Los eventos
 * personales se guardan para replay; los de caso se difunden sin mas.
 */
@Service
public class EventoWebSocketService {

    private final SimpMessagingTemplate mensajeria;
    private final EventoReplayStore replayStore;

    public EventoWebSocketService(SimpMessagingTemplate mensajeria, EventoReplayStore replayStore) {
        this.mensajeria = mensajeria;
        this.replayStore = replayStore;
    }

    /** Evento personal: se encola para replay y va a la sala del usuario. */
    public void aUsuario(Long idUsuario, EventoWebSocket evento) {
        if (idUsuario == null || evento == null) {
            return;
        }
        ReplayEvento replay = replayStore.guardar(idUsuario, evento);
        mensajeria.convertAndSend(SalaWebSocket.deUsuario(idUsuario), replay.evento().conId(replay.id()));
    }

    /** Evento de caso (chat/presencia): va a la sala del caso. */
    public void aCaso(Long idCaso, EventoWebSocket evento) {
        if (idCaso == null || evento == null) {
            return;
        }
        mensajeria.convertAndSend(SalaWebSocket.deCaso(idCaso), evento);
    }

    /**
     * Reenvia al usuario los eventos personales posteriores a ultimoId y devuelve
     * cuantos reenvio. El cliente llama a esto al (re)conectar para no perder nada.
     */
    public int reenviarPendientes(Long idUsuario, long ultimoId) {
        List<ReplayEvento> pendientes = replayStore.desde(idUsuario, ultimoId);
        for (ReplayEvento replay : pendientes) {
            mensajeria.convertAndSend(SalaWebSocket.deUsuario(idUsuario), replay.evento().conId(replay.id()));
        }
        return pendientes.size();
    }
}
