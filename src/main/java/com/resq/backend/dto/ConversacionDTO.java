package com.resq.backend.dto;

import java.time.LocalDateTime;

public record ConversacionDTO(
    Long idUsuario,       // ID de la otra persona
    String nombre,        // Nombre de la otra persona
    String ultimoMensaje, // El texto del último mensaje
    LocalDateTime fechaUltimoMensaje,
    boolean archivada
) {}