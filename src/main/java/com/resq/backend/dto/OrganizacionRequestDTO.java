package com.resq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * HU-20: datos del formulario de registro de organización.
 * Obligatorios: nombre, tipo, dirección, teléfono y email. La descripción es opcional.
 */
public record OrganizacionRequestDTO(

        @NotNull(message = "idRepresentante es obligatorio")
        Long idRepresentante,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
        String nombre,

        @NotBlank(message = "El tipo es obligatorio")
        @Pattern(regexp = "REFUGIO|VETERINARIA|RESCATISTA_INDEPENDIENTE",
                message = "El tipo debe ser REFUGIO, VETERINARIA o RESCATISTA_INDEPENDIENTE")
        String tipo,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
        String direccion,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
        @Pattern(regexp = ValidacionOrganizacion.TELEFONO,
                message = "El teléfono no tiene un formato válido (7 a 15 dígitos)")
        String telefono,

        @NotBlank(message = "El email es obligatorio")
        @Size(max = 150, message = "El email no puede superar los 150 caracteres")
        @Pattern(regexp = ValidacionOrganizacion.EMAIL, message = "El email no tiene un formato válido")
        String email,

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion,

        @Size(max = 500, message = "Los horarios no pueden superar los 500 caracteres")
        String horarios,

        @Size(max = 500, message = "Las zonas de cobertura no pueden superar los 500 caracteres")
        String zonasCobertura) {
}
