package com.resq.backend.dto;

import com.resq.backend.entity.Mensaje;

import java.time.LocalDateTime;

public record MensajeDTO(
        Long idMensaje,
        Long idConversacion,
        Long idEmisor,
        String tipo,
        String contenido,
        String urlAdjunto,
        String nombreAdjunto,
        LocalDateTime fechaEnvio,
        boolean eliminado) {

    public static MensajeDTO de(Mensaje mensaje, Long idUsuario) {
        Long idConversacion = mensaje.getIdEmisor().equals(idUsuario)
                ? mensaje.getIdReceptor()
                : mensaje.getIdEmisor();
        boolean eliminado = mensaje.isEliminado();
        return new MensajeDTO(
                mensaje.getIdMensaje(),
                idConversacion,
                mensaje.getIdEmisor(),
                mensaje.getTipo(),
                eliminado ? "" : mensaje.getContenido(),
                eliminado ? null : mensaje.getUrlAdjunto(),
                eliminado ? null : mensaje.getNombreAdjunto(),
                mensaje.getFechaEnvio(),
                eliminado);
    }
}
