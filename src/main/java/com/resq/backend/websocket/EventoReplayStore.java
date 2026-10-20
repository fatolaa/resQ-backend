package com.resq.backend.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HU-38a: buzon en memoria con los ultimos eventos personales de cada usuario, para
 * reenviarlos al reconectar. Es acotado y efimero: es un buffer de replay, no el
 * historial persistido (eso lo dan el endpoint de notificaciones y el chat).
 */
@Component
public class EventoReplayStore {

    private static final class Buzon {
        private final Deque<ReplayEvento> eventos = new ArrayDeque<>();
        private long ultimoId = 0L;
    }

    private final int capacidad;
    private final Map<Long, Buzon> buzones = new ConcurrentHashMap<>();

    public EventoReplayStore(@Value("${resq.ws.replay-max:50}") int capacidad) {
        this.capacidad = capacidad;
    }

    /** Guarda el evento y devuelve su entrada con el id de replay asignado. */
    public ReplayEvento guardar(Long idUsuario, EventoWebSocket evento) {
        Buzon buzon = buzones.computeIfAbsent(idUsuario, id -> new Buzon());
        synchronized (buzon) {
            ReplayEvento nuevo = new ReplayEvento(++buzon.ultimoId, evento);
            buzon.eventos.addLast(nuevo);
            while (buzon.eventos.size() > capacidad) {
                buzon.eventos.removeFirst();
            }
            return nuevo;
        }
    }

    /** Eventos guardados con id mayor a ultimoId, en el orden en que se generaron. */
    public List<ReplayEvento> desde(Long idUsuario, long ultimoId) {
        Buzon buzon = buzones.get(idUsuario);
        if (buzon == null) {
            return List.of();
        }
        synchronized (buzon) {
            List<ReplayEvento> pendientes = new ArrayList<>();
            for (ReplayEvento evento : buzon.eventos) {
                if (evento.id() > ultimoId) {
                    pendientes.add(evento);
                }
            }
            return pendientes;
        }
    }

    /** Ultimo id de replay del usuario (0 si nunca recibio eventos personales). */
    public long ultimoId(Long idUsuario) {
        Buzon buzon = buzones.get(idUsuario);
        if (buzon == null) {
            return 0L;
        }
        synchronized (buzon) {
            return buzon.ultimoId;
        }
    }

    public record ReplayEvento(long id, EventoWebSocket evento) {
    }
}
