package com.resq.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UsuarioDTO(
        Long idUsuario,
        String nombre,
        String email,
        String telefono,
        String rol,
        LocalDateTime fechaRegistro,
        List<String> tiposAyuda) {
}