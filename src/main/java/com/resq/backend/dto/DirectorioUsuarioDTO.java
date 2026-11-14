package com.resq.backend.dto;

/**
 * TT-49: datos minimos de un usuario para iniciar una conversacion desde la
 * bandeja. No expone correo, telefono ni otros datos del perfil.
 */
public record DirectorioUsuarioDTO(Long idUsuario, String nombre, String rol) {
}
