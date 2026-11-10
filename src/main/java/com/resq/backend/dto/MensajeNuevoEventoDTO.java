package com.resq.backend.dto;

import java.time.LocalDateTime;

public record MensajeNuevoEventoDTO(
        Long idConversacion,
        Long idMensaje,
        Long idEmisor,
        String tipo,
        String contenido,
        String urlAdjunto,
        String nombreAdjunto,
        LocalDateTime fechaEnvio) {
}
