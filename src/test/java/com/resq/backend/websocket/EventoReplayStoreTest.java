package com.resq.backend.websocket;

import com.resq.backend.websocket.EventoReplayStore.ReplayEvento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventoReplayStore: buffer de replay por usuario (HU-38a)")
class EventoReplayStoreTest {

    private EventoWebSocket evento(String tipo) {
        return EventoWebSocket.de(tipo, null);
    }

    @Test
    @DisplayName("numera los eventos y los devuelve posteriores a un id dado, en orden")
    void debeNumerarYDevolverLosPosteriores() {
        EventoReplayStore store = new EventoReplayStore(50);

        store.guardar(7L, evento("a"));
        store.guardar(7L, evento("b"));
        ReplayEvento tercero = store.guardar(7L, evento("c"));

        assertThat(tercero.id()).isEqualTo(3L);
        List<ReplayEvento> pendientes = store.desde(7L, 1L);
        assertThat(pendientes).extracting(r -> r.evento().tipo()).containsExactly("b", "c");
    }

    @Test
    @DisplayName("respeta la capacidad: descarta los mas viejos")
    void debeDescartarLosMasViejos() {
        EventoReplayStore store = new EventoReplayStore(2);

        store.guardar(7L, evento("a"));
        store.guardar(7L, evento("b"));
        store.guardar(7L, evento("c"));

        assertThat(store.desde(7L, 0L)).extracting(r -> r.evento().tipo()).containsExactly("b", "c");
        assertThat(store.ultimoId(7L)).isEqualTo(3L);
    }

    @Test
    @DisplayName("un usuario sin eventos no tiene nada para reenviar")
    void unUsuarioSinEventosNoTienePendientes() {
        EventoReplayStore store = new EventoReplayStore(50);

        assertThat(store.desde(99L, 0L)).isEmpty();
        assertThat(store.ultimoId(99L)).isZero();
    }

    @Test
    @DisplayName("los buzones de distintos usuarios no se mezclan")
    void losBuzonesNoSeMezclan() {
        EventoReplayStore store = new EventoReplayStore(50);

        store.guardar(7L, evento("de-7"));
        store.guardar(9L, evento("de-9"));

        assertThat(store.desde(7L, 0L)).extracting(r -> r.evento().tipo()).containsExactly("de-7");
        assertThat(store.desde(9L, 0L)).extracting(r -> r.evento().tipo()).containsExactly("de-9");
    }
}
