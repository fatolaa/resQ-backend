package com.resq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DenunciaRequestDTO(
    @NotNull(message = "El id del mensaje es obligatorio")
    Long idMensaje,

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    String motivo
) {}
