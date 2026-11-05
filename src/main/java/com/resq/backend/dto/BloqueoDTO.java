package com.resq.backend.dto;

import java.time.LocalDateTime;

public record BloqueoDTO(
    Long idBloqueo,
    Long idBloqueado,
    String nombreBloqueado,
    LocalDateTime fechaBloqueo
) {}
