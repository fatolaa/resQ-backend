package com.resq.backend.websocket;

/**
 * HU-38a: catalogo de tipos de evento que el servidor WebSocket emite. El valor es
 * el que viaja en el campo "tipo" del sobre y el que el frontend usa para enrutar.
 */
public final class TipoEventoWebSocket {

    public static final String NOTIFICACION_NUEVA = "notification.new";
    public static final String CHAT_MENSAJE = "chat.message";
    public static final String CHAT_ESCRIBIENDO = "chat.typing";
    public static final String USUARIO_EN_LINEA = "user.online";

    private TipoEventoWebSocket() {
    }
}
