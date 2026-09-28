package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.UsuarioDTO;
import com.resq.backend.dto.UsuarioRequestDTO;
import com.resq.backend.dto.UsuarioUpdateDTO;
import com.resq.backend.dto.VoluntarioRequestDTO;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    // Tipos de ayuda válidos (mismas categorías que ya se muestran en el landing)
    private static final Set<String> TIPOS_AYUDA_VALIDOS = Set.of(
            "TRANSPORTE", "HOGAR_TEMPORAL", "ALIMENTO", "RESCATE");

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

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtenerUsuarioPorId(@PathVariable Long id) {
        return usuarioRepository.findById(id)
                .map(usuario -> ResponseEntity.ok(toDTO(usuario)))
                .orElseGet(() -> ResponseEntity.notFound().build());
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

    // HU-17: un usuario ciudadano solicita convertirse en voluntario (primera vez)
    @PatchMapping("/{id}/voluntario")
    public ResponseEntity<?> registrarVoluntario(@PathVariable Long id,
                                                  @Valid @RequestBody VoluntarioRequestDTO request) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        if ("VOLUNTARIO".equalsIgnoreCase(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError(HttpStatus.CONFLICT.value(), "Este usuario ya es voluntario", null));
        }

        List<String> tiposInvalidos = tiposInvalidos(request.tiposAyuda());
        if (!tiposInvalidos.isEmpty()) {
            return respuestaTiposInvalidos(tiposInvalidos);
        }

        usuario.setRol("VOLUNTARIO");
        usuario.setTipoAyuda(String.join(",", request.tiposAyuda()));

        Usuario guardado = usuarioRepository.save(usuario);
        return ResponseEntity.ok(toDTO(guardado));
    }

    // 🆕 HU-17: un voluntario ya existente edita qué tipos de ayuda ofrece
    @PatchMapping("/{id}/voluntario/tipos-ayuda")
    public ResponseEntity<?> actualizarTiposAyuda(@PathVariable Long id,
                                                   @Valid @RequestBody VoluntarioRequestDTO request) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"VOLUNTARIO".equalsIgnoreCase(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError(HttpStatus.CONFLICT.value(),
                            "Primero debes registrarte como voluntario", null));
        }

        List<String> tiposInvalidos = tiposInvalidos(request.tiposAyuda());
        if (!tiposInvalidos.isEmpty()) {
            return respuestaTiposInvalidos(tiposInvalidos);
        }

        usuario.setTipoAyuda(String.join(",", request.tiposAyuda()));

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

    private List<String> tiposInvalidos(List<String> tiposAyuda) {
        return tiposAyuda.stream()
                .filter(tipo -> !TIPOS_AYUDA_VALIDOS.contains(tipo))
                .toList();
    }

    private ResponseEntity<ApiError> respuestaTiposInvalidos(List<String> tiposInvalidos) {
        return ResponseEntity.badRequest()
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(),
                        "Tipo(s) de ayuda inválido(s): " + String.join(", ", tiposInvalidos), null));
    }

    private UsuarioDTO toDTO(Usuario usuario) {
        List<String> tiposAyuda = (usuario.getTipoAyuda() == null || usuario.getTipoAyuda().isBlank())
                ? List.of()
                : Arrays.asList(usuario.getTipoAyuda().split(","));

        return new UsuarioDTO(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getRol(),
                usuario.getFechaRegistro(),
                tiposAyuda);
    }
}