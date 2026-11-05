package com.resq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnviarMensajeRequestDTO(
    @NotNull(message = "El id del receptor es obligatorio")
    Long idReceptor,

    @NotBlank(message = "El contenido del mensaje no puede estar vacío")
    @Size(max = 2000, message = "El contenido no puede superar los 2000 caracteres")
    String contenido
) {}
