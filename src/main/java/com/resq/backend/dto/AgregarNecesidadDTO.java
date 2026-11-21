package com.resq.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Rediseño: tipo de necesidad que se agrega a un caso. */
public record AgregarNecesidadDTO(@NotBlank(message = "El tipo de necesidad es obligatorio") String tipo) {
}
