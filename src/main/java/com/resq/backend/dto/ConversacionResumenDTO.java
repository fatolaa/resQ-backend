package com.resq.backend.dto;

import java.time.LocalDateTime;

public record ConversacionResumenDTO(
        Long idConversacion,
        Long idOtroUsuario,
        String nombre,
        boolean esOrganizacion,
        String ultimoMensaje,
        LocalDateTime fechaUltimoMensaje) {
}
