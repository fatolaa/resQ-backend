package com.resq.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioRequestDTO(
        @NotBlank(message = "nombre es obligatorio") String nombre,
        @NotBlank(message = "email es obligatorio") @Email(message = "email inválido") String email,
        @NotBlank(message = "password es obligatorio") String password,
        String telefono,
        @NotBlank(message = "rol es obligatorio") String rol) {
}