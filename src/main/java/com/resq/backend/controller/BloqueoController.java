package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.BloqueoDTO;
import com.resq.backend.entity.BloqueoUsuario;
import com.resq.backend.exception.ChatException;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.service.BloqueoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat/bloqueos")
public class BloqueoController {

    private static final Logger logger = LoggerFactory.getLogger(BloqueoController.class);

    private final BloqueoService bloqueoService;

    public BloqueoController(BloqueoService bloqueoService) {
        this.bloqueoService = bloqueoService;
    }

    @PostMapping("/{idBloqueado}")
    public ResponseEntity<?> bloquear(@PathVariable Long idBloqueado) {
        Optional<Long> idToken = UsuarioAutenticado.idActual();
        if (idToken.isEmpty()) {
            return sinSesion();
        }
        logger.info("Solicitud de bloqueo: usuario ID {} bloquea a usuario ID {}", idToken.get(), idBloqueado);
        BloqueoUsuario bloqueo = bloqueoService.bloquear(idToken.get(), idBloqueado);
        return ResponseEntity.status(HttpStatus.CREATED).body(bloqueo);
    }

    @DeleteMapping("/{idBloqueado}")
    public ResponseEntity<?> desbloquear(@PathVariable Long idBloqueado) {
        Optional<Long> idToken = UsuarioAutenticado.idActual();
        if (idToken.isEmpty()) {
            return sinSesion();
        }
        logger.info("Solicitud de desbloqueo: usuario ID {} desbloquea a usuario ID {}", idToken.get(), idBloqueado);
        bloqueoService.desbloquear(idToken.get(), idBloqueado);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        Optional<Long> idToken = UsuarioAutenticado.idActual();
        if (idToken.isEmpty()) {
            return sinSesion();
        }
        logger.info("Listando bloqueos del usuario ID {}", idToken.get());
        List<BloqueoDTO> lista = bloqueoService.listarBloqueados(idToken.get());
        return ResponseEntity.ok(lista);
    }

    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ApiError> manejarError(ChatException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiError(ex.getStatus().value(), ex.getMessage(), ex.getErrores()));
    }

    private ResponseEntity<ApiError> sinSesion() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(HttpStatus.UNAUTHORIZED.value(),
                        "Debes iniciar sesion para realizar esta accion", null));
    }
}
