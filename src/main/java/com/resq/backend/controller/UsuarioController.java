package com.resq.backend.controller;

import com.resq.backend.dto.UsuarioDTO;
import com.resq.backend.dto.UsuarioRequestDTO;
import com.resq.backend.dto.UsuarioUpdateDTO;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> obtenerUsuarios() {
        List<UsuarioDTO> usuarios = usuarioRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> crearUsuario(@Valid @RequestBody UsuarioRequestDTO request) {
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setTelefono(request.telefono());
        usuario.setRol(request.rol());

        Usuario guardado = usuarioRepository.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizarUsuario(@PathVariable Long id,
@Valid @RequestBody UsuarioRequestDTO request) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        usuario.setTelefono(request.telefono());
        usuario.setRol(request.rol());

        Usuario guardado = usuarioRepository.save(usuario);
        return ResponseEntity.ok(toDTO(guardado));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizarParcialmente(@PathVariable Long id,
                                                            @Valid @RequestBody UsuarioUpdateDTO updates) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        if (updates.nombre() != null) {
            usuario.setNombre(updates.nombre());
        }
        if (updates.email() != null) {
            usuario.setEmail(updates.email());
        }
        if (updates.password() != null && !updates.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(updates.password()));
        }
        if (updates.telefono() != null) {
            usuario.setTelefono(updates.telefono());
        }
        if (updates.rol() != null) {
            usuario.setRol(updates.rol());
        }

        Usuario guardado = usuarioRepository.save(usuario);
        return ResponseEntity.ok(toDTO(guardado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        if (!usuarioRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        usuarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
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