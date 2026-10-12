package com.resq.backend.dto;

import jakarta.validation.constraints.NotBlank;


public record SeguimientoCasoRequestDTO(
        @NotBlank(message = "La nota es obligatoria") String nota,
        String fotoUrl
) {
}