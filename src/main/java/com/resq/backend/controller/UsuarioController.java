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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    // HU-23: los tres roles que defined data.sql. Se validan para que nadie escriba
    // un rol inventado (por ejemplo "ADMINISTRADOR") que despues no se reconozca.
    private static final Set<String> ROLES_VALIDOS = Set.of("USUARIO", "VOLUNTARIO", "ADMIN");

    // Un alta publica nunca puede crear un administrador: el rol ADMIN solo se
    // concede desde el panel, que es una operacion autenticada de administrador.
    private static final Set<String> ROLES_ASIGNABLES_EN_ALTA = Set.of("USUARIO", "VOLUNTARIO");

    private static final String ROL_ADMIN = "ADMIN";
    private static final String AUTORIDAD_ADMIN = "ROLE_ADMIN";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // HU-23: el listado completo solo lo consulta el administrador (lo restringe SecurityConfig).
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> obtenerUsuarios() {
        List<UsuarioDTO> usuarios = usuarioRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    // Ver una cuenta concreta: la puede pedir su propietario o el administrador.
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerUsuarioPorId(@PathVariable Long id) {
        if (!puedeAccederA(id)) {
            return prohibido("No puedes consultar la informacion de otra cuenta");
        }
        return usuarioRepository.findById(id)
                .map(usuario -> ResponseEntity.ok(toDTO(usuario)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crearUsuario(@Valid @RequestBody UsuarioRequestDTO request) {
        if (!ROLES_ASIGNABLES_EN_ALTA.contains(normalizarRol(request.rol()))) {
            return prohibido("El rol ADMIN solo puede concederlo un administrador");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setTelefono(request.telefono());
        usuario.setRol(normalizarRol(request.rol()));

        Usuario guardado = usuarioRepository.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id,
@Valid @RequestBody UsuarioRequestDTO request) {
        ResponseEntity<?> rolInvalido = validarRol(request.rol());
        if (rolInvalido != null) {
            return rolInvalido;
        }

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
        usuario.setRol(normalizarRol(request.rol()));

        Usuario guardado = usuarioRepository.save(usuario);
        return ResponseEntity.ok(toDTO(guardado));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizarParcialmente(@PathVariable Long id,
                                                             @Valid @RequestBody UsuarioUpdateDTO updates) {
        if (updates.rol() != null) {
            ResponseEntity<?> rolInvalido = validarRol(updates.rol());
            if (rolInvalido != null) {
                return rolInvalido;
            }
        }

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
            usuario.setRol(normalizarRol(updates.rol()));
        }

        Usuario guardado = usuarioRepository.save(usuario);
        return ResponseEntity.ok(toDTO(guardado));
    }

    // HU-17: un usuario ciudadano solicita convertirse en voluntario (primera vez)
    @PatchMapping("/{id}/voluntario")
    public ResponseEntity<?> registrarVoluntario(@PathVariable Long id,
                                                  @Valid @RequestBody VoluntarioRequestDTO request) {
        if (!puedeAccederA(id)) {
            return prohibido("No puedes Inscribirte como voluntario en otra cuenta");
        }

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

    // HU-17: un voluntario ya existente edita qué tipos de ayuda ofrece
    @PatchMapping("/{id}/voluntario/tipos-ayuda")
    public ResponseEntity<?> actualizarTiposAyuda(@PathVariable Long id,
                                                   @Valid @RequestBody VoluntarioRequestDTO request) {
        if (!puedeAccederA(id)) {
            return prohibido("No puedes editar los tipos de ayuda de otra cuenta");
        }

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
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        // Borrarse a si mismo deja la aplicacion sin ningun administrador: el panel
        // de usuarios quedaria inaccesible para siempre. Se bloquea en el servidor
        // y no solo en la interfaz.
        if (idUsuarioAutenticado() != null && idUsuarioAutenticado().equals(id)) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(HttpStatus.BAD_REQUEST.value(),
                            "No puedes eliminar tu propia cuenta administrativa", null));
        }

        if (!usuarioRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        usuarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String normalizarRol(String rol) {
        return rol == null ? null : rol.toUpperCase();
    }

    private ResponseEntity<?> validarRol(String rol) {
        String normalizado = normalizarRol(rol);
        if (normalizado != null && !ROLES_VALIDOS.contains(normalizado)) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(HttpStatus.BAD_REQUEST.value(),
                            "Rol invalido: " + rol + ". Validos: " + String.join(", ", ROLES_VALIDOS), null));
        }
        return null;
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

    private ResponseEntity<ApiError> prohibido(String mensaje) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(HttpStatus.FORBIDDEN.value(), mensaje, null));
    }

    /** El administrador puede tocar cualquier cuenta; el resto, solo la propia. */
    private boolean puedeAccederA(Long id) {
        return esAdministrador() || idUsuarioAutenticado() != null && idUsuarioAutenticado().equals(id);
    }

    private boolean esAdministrador() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> AUTORIDAD_ADMIN.equals(autoridad.getAuthority()));
    }

    private Long idUsuarioAutenticado() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || autenticacion.getPrincipal() == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(autenticacion.getPrincipal()));
        } catch (NumberFormatException principalInvalido) {
            return null;
        }
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
