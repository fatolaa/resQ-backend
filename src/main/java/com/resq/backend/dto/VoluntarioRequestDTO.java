package com.resq.backend.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record VoluntarioRequestDTO(
        @NotEmpty(message = "Debes seleccionar al menos un tipo de ayuda") List<String> tiposAyuda) {
}