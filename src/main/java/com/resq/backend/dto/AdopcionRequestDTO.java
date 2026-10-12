package com.resq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * HU-30: datos del formulario de adopción. Obligatorios: el caso, el nombre y el
 * teléfono del adoptante y la fecha. El email y las observaciones son opcionales.
 */
public record AdopcionRequestDTO(

        @NotNull(message = "idReporte es obligatorio")
        Long idReporte,

        @NotBlank(message = "El nombre del adoptante es obligatorio")
        @Size(max = 150, message = "El nombre del adoptante no puede superar los 150 caracteres")
        String nombreAdoptante,

        @NotBlank(message = "El teléfono del adoptante es obligatorio")
        @Size(max = 30, message = "El teléfono del adoptante no puede superar los 30 caracteres")
        @Pattern(regexp = ValidacionOrganizacion.TELEFONO,
                message = "El teléfono no tiene un formato válido (7 a 15 dígitos)")
        String telefonoAdoptante,

        @Size(max = 150, message = "El email del adoptante no puede superar los 150 caracteres")
        @Pattern(regexp = ValidacionOrganizacion.EMAIL, message = "El email no tiene un formato válido")
        String emailAdoptante,

        @NotNull(message = "La fecha de la adopción es obligatoria")
        @PastOrPresent(message = "La fecha de la adopción no puede ser futura")
        LocalDate fechaAdopcion,

        @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
        String observaciones) {
}
