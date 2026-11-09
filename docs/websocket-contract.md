# Contrato WebSocket (STOMP) — HU-38a

Base para notificaciones y chat en tiempo real. El cliente usa **STOMP sobre WebSocket
nativo** (sin SockJS), por ejemplo con `@stomp/stompjs`.

## Conexion

- **Endpoint (handshake)**: `GET /ws` (HTTP upgrade). Es publico a nivel HTTP: la
  autenticacion ocurre en el frame STOMP `CONNECT`.
- **Autenticacion**: en el frame `CONNECT` va la cabecera nativa
  `Authorization: Bearer <jwt>`. Sin token valido la conexion se rechaza.
- **Prefijos**:
  - `@MessageMapping` de la app: `/app/...`
  - Salas que difunde el servidor: `/topic/...`

## Salas (suscripciones)

| Sala                    | Destino STOMP              | Quien puede suscribirse                      |
|-------------------------|----------------------------|----------------------------------------------|
| Personal del usuario    | `/topic/usuarios/{idUsuario}` | Solo ese usuario                          |
| Chat de un caso         | `/topic/casos/{idCaso}`    | Solo participantes del caso (reportante, representante de la organizacion asignada o voluntario que reviso el reporte) |

Suscribirse a una sala sin permiso cierra la conexion con error de acceso.

## Eventos que emite el servidor

Sobre comun:

```json
{
  "id": 12,
  "tipo": "notification.new",
  "emitidoEn": "2026-10-19T15:30:00Z",
  "datos": { }
}
```

`id` es la secuencia de replay del usuario: solo viene en los eventos **personales**
(los que se guardan para reconexion). En los eventos de caso se omite. El cliente
guarda el mayor `id` que proceso y lo manda en `/app/replay` al reconectar; tambien
lo usa para descartar duplicados.

### `notification.new` — sala personal
```json
{
  "tipo": "notification.new",
  "emitidoEn": "2026-10-19T15:30:00Z",
  "datos": {
    "idNotificacion": 42,
    "idUsuario": 7,
    "idReporte": 101,
    "tipo": "SISTEMA",
    "mensaje": "Tu reporte fue aprobado",
    "leida": false,
    "fechaCreacion": "2026-10-19T15:30:00"
  }
}
```

### `chat.typing` — sala del caso
```json
{
  "tipo": "chat.typing",
  "emitidoEn": "2026-10-19T15:30:05Z",
  "datos": { "idCaso": 101, "idUsuario": 9 }
}
```

### `chat.message` — sala del caso
HU-22 (#41): cada mensaje del chat del caso se guarda y se difunde con el mismo formato
que devuelve el historial (`GET /api/casos/{idCaso}/chat/mensajes`). `tipo` es `TEXTO`,
`UBICACION` (con `latitud` y `longitud`) o `FOTO` (con `fotoUrl`):
```json
{
  "tipo": "chat.message",
  "emitidoEn": "2026-10-19T15:30:10Z",
  "datos": {
    "idMensaje": 500,
    "idCaso": 101,
    "idEmisor": 9,
    "nombreEmisor": "Ana Quiroga",
    "tipo": "TEXTO",
    "contenido": "Voy en camino",
    "latitud": null,
    "longitud": null,
    "fotoUrl": null,
    "fechaEnvio": "2026-10-19T11:30:10"
  }
}
```

Los mensajes se envian por REST (no por STOMP), asi pasan por la misma validacion y
seguridad que el resto de la API:

| Accion | Endpoint |
|---|---|
| Historial | `GET /api/casos/{idCaso}/chat/mensajes` |
| Texto o ubicacion | `POST /api/casos/{idCaso}/chat/mensajes` con `{ "contenido" }` o `{ "latitud", "longitud", "contenido"? }` |
| Foto | `POST /api/casos/{idCaso}/chat/fotos` (multipart: `foto`, `comentario` opcional) |
| Participantes y presencia | `GET /api/casos/{idCaso}/chat/participantes` |

Participan el reportante, el representante de la organizacion que tomo el caso y el
voluntario que reviso el reporte.

### `user.online` — sala personal y salas de los casos del usuario
```json
{
  "tipo": "user.online",
  "emitidoEn": "2026-10-19T15:31:00Z",
  "datos": { "idUsuario": 9, "enLinea": true }
}
```

## Mensajes que envia el cliente

| Envio                                  | Destino STOMP                       |
|----------------------------------------|-------------------------------------|
| Aviso de "escribiendo" en un caso      | `/app/caso/{idCaso}/escribiendo`    |
| Pedir eventos personales perdidos       | `/app/replay` con `{ "ultimoId": 12 }` |

## Replay al reconectar

El servidor guarda los ultimos `resq.ws.replay-max` (por defecto 50) eventos
**personales** de cada usuario. Tras reconectar, el cliente suscribe su sala personal
y manda `/app/replay` con el `ultimoId` que ya proceso; el servidor reenvia los eventos
posteriores en orden. El `id` de cada evento personal viaja en el campo `id` del sobre,
tanto en los eventos en vivo como en los reenviados, asi que el cliente puede
actualizar su posicion sin ambiguedad y descartar los que ya tenia.

> Nota: el replay es un buffer en memoria y acotado; no reemplaza al historial
> persistido (endpoint de notificaciones y, en el chat, el historial del caso).
