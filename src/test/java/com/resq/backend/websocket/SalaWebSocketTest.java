package com.resq.backend.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SalaWebSocket: nombres y parseo de destinos (HU-38a)")
class SalaWebSocketTest {

    @Test
    @DisplayName("arma el destino de una sala de usuario y de caso")
    void debeArmarLosDestinos() {
        assertThat(SalaWebSocket.deUsuario(7L)).isEqualTo("/topic/usuarios/7");
        assertThat(SalaWebSocket.deCaso(101L)).isEqualTo("/topic/casos/101");
    }

    @Test
    @DisplayName("extrae el id de usuario de su sala")
    void debeExtraerElIdUsuario() {
        assertThat(SalaWebSocket.idUsuarioDe("/topic/usuarios/7")).isEqualTo(7L);
    }

    @Test
    @DisplayName("extrae el id de caso de su sala")
    void debeExtraerElIdCaso() {
        assertThat(SalaWebSocket.idCasoDe("/topic/casos/101")).isEqualTo(101L);
    }

    @Test
    @DisplayName("un destino de caso no se confunde con una sala de usuario y viceversa")
    void noDebeConfundirLasSalas() {
        assertThat(SalaWebSocket.idUsuarioDe("/topic/casos/101")).isNull();
        assertThat(SalaWebSocket.idCasoDe("/topic/usuarios/7")).isNull();
    }

    @Test
    @DisplayName("destinos que no son sala, vacios o con id no numerico devuelven null")
    void debeRechazarDestinosInvalidos() {
        assertThat(SalaWebSocket.idUsuarioDe(null)).isNull();
        assertThat(SalaWebSocket.idUsuarioDe("/app/replay")).isNull();
        assertThat(SalaWebSocket.idUsuarioDe("/topic/usuarios/")).isNull();
        assertThat(SalaWebSocket.idUsuarioDe("/topic/usuarios/abc")).isNull();
        assertThat(SalaWebSocket.idCasoDe("/topic/casos/1/extra")).isNull();
    }
}
