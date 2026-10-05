package com.resq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReporteEstadoDTO(

        @NotBlank(message = "estado es obligatorio")
        @Pattern(regexp = "PENDIENTE|EN_PROCESO|RESUELTO|CANCELADO",
                message = "estado debe ser PENDIENTE, EN_PROCESO, RESUELTO o CANCELADO")
        String estado) {
}