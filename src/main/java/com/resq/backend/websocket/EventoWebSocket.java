package com.resq.backend.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * HU-38a: sobre comun de todos los eventos del WebSocket. El contrato es estable:
 * "id" (secuencia de replay; solo en eventos personales, para que el cliente sepa
 * hasta donde proceso), "tipo" (ver {@link TipoEventoWebSocket}), "emitidoEn"
 * (ISO-8601 UTC) y "datos" (payload propio de cada tipo, documentado en
 * docs/websocket-contract.md).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventoWebSocket(Long id, String tipo, Instant emitidoEn, Object datos) {

    public static EventoWebSocket de(String tipo, Object datos) {
        return new EventoWebSocket(null, tipo, Instant.now(), datos);
    }

    /** Copia del evento con el id de replay que le asigna el buzon del usuario. */
    public EventoWebSocket conId(long id) {
        return new EventoWebSocket(id, tipo, emitidoEn, datos);
    }
}
