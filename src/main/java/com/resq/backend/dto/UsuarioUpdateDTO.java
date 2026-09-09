package com.resq.backend.dto;

import jakarta.validation.constraints.Email;

public record UsuarioUpdateDTO(
        String nombre,
        @Email(message = "email inválido") String email,
        String password,
        String telefono,
        String rol) {
}