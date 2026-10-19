package com.resq.backend.dto;

/**
 * HU-36a: cuerpo del PUT de preferencias de notificacion.
 *
 * Los campos son Boolean (no boolean) a proposito: un campo ausente o null
 * significa "no cambiar esta preferencia", y solo se actualizan los que vienen
 * con valor. Asi el frontend puede mandar un cambio puntual sin reenviar todo.
 */
public record PreferenciaNotificacionRequestDTO(
        Boolean notificarSolicitudAprobada,
        Boolean notificarSolicitudRechazada,
        Boolean notificarReporteCerca,
        Boolean notificarChatMensaje) {
}
