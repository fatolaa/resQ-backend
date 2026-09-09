package com.resq.backend.dto;

public record UsuarioRequestDTO(
        String nombre,
        String email,
        String password,
        String telefono,
        String rol) {
}