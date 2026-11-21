package com.resq.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * Lo que cada usuario puede cambiar de su propia cuenta para que lo contacten.
 *
 * @param nombre   null para no cambiarlo; no puede quedar vacio.
 * @param telefono null para no cambiarlo; "" para quitarlo.
 */
public record MiContactoDTO(
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres") String nombre,
        @Size(max = 25, message = "El teléfono debe tener entre 7 y 15 dígitos") String telefono) {
}
