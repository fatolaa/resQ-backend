package com.resq.backend.controller;

import com.resq.backend.dto.UsuarioDTO;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> obtenerUsuarios() {
        List<UsuarioDTO> usuarios = usuarioRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    private UsuarioDTO toDTO(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getRol(),
                usuario.getFechaRegistro());
    }
}