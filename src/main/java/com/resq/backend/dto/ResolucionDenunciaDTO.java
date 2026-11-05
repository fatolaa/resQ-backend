package com.resq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResolucionDenunciaDTO(
    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "REVISADA|DESCARTADA",
             message = "El estado debe ser REVISADA o DESCARTADA")
    String estado,

    @Size(max = 500, message = "La nota no puede superar los 500 caracteres")
    String notaAdmin
) {}
