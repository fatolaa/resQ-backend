package com.resq.backend.dto;

public record UsuarioUpdateDTO(
        String nombre,
        String email,
        String password,
        String telefono,
        String rol) {
}