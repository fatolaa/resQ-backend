package com.resq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearSolicitudVoluntarioRequestDTO(
        @NotBlank(message = "El tipo de ayuda es obligatorio")
        @Size(max = 255, message = "El tipo de ayuda no puede superar los 255 caracteres")
        String tipoAyuda,

        @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres")
        String mensaje
) {
}
