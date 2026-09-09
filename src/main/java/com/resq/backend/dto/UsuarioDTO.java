package com.resq.backend.dto;

import java.time.LocalDateTime;

public record UsuarioDTO(
        Long idUsuario,
        String nombre,
        String email,
        String telefono,
        String rol,
        LocalDateTime fechaRegistro) {
}