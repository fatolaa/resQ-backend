package com.resq.backend.dto;

/**
 * HU-36a: preferencias de notificacion que se devuelven al usuario.
 * Cada campo indica si el usuario quiere recibir ese tipo de evento.
 */
public record PreferenciaNotificacionDTO(
        boolean notificarSolicitudAprobada,
        boolean notificarSolicitudRechazada,
        boolean notificarReporteCerca,
        boolean notificarChatMensaje) {
}
