package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        logger.info("Intento de inicio de sesión para el correo: {}", email);

        String password = credentials.get("password");

        Usuario usuario = email == null ? null : usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null || password == null || !passwordEncoder.matches(password, usuario.getPasswordHash())) {
            logger.warn("Credenciales inválidas para el correo: {}", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError(HttpStatus.UNAUTHORIZED.value(), "Credenciales inválidas", null));
        }

        logger.info("Login exitoso. Usuario ID: {} - Rol: {}", usuario.getIdUsuario(), usuario.getRol());

        return ResponseEntity.ok(Map.of(
                "message", "Login exitoso de " + usuario.getNombre(),
                "token", jwtService.generarToken(usuario),
                "idUsuario", usuario.getIdUsuario(),
                "email", usuario.getEmail(),
                "rol", usuario.getRol()));
    }
}