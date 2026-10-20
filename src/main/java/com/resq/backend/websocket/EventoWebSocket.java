package com.resq.backend.websocket;

import java.time.Instant;

/**
 * HU-38a: sobre comun de todos los eventos del WebSocket. El contrato es estable:
 * "tipo" (ver {@link TipoEventoWebSocket}), "emitidoEn" (ISO-8601 UTC) y "datos"
 * (payload propio de cada tipo, documentado en docs/websocket-contract.md).
 */
public record EventoWebSocket(String tipo, Instant emitidoEn, Object datos) {

    public static EventoWebSocket de(String tipo, Object datos) {
        return new EventoWebSocket(tipo, Instant.now(), datos);
    }
}
