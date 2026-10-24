package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.entity.Notificacion;
import com.resq.backend.exception.NotificacionException;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * HU-37: bandeja de notificaciones del usuario autenticado.
 *
 * - GET   /api/notificaciones/usuario/{idUsuario}  bandeja sin archivadas
 * - PATCH /api/notificaciones/{id}/leida           marca como leida
 * - PATCH /api/notificaciones/{id}/no-leida        marca como no leida
 * - PATCH /api/notificaciones/{id}/archivar        la saca de la bandeja
 *
 * El id del usuario siempre sale del token: no se confia en el que manda el cliente.
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private static final Logger logger = LoggerFactory.getLogger(NotificacionController.class);

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<?> obtenerPorUsuario(@PathVariable Long idUsuario) {
        Optional<Long> idToken = UsuarioAutenticado.idActual();
        if (idToken.isEmpty()) {
            return sinSesion();
        }
        if (!idToken.get().equals(idUsuario)) {
            logger.warn("Acceso denegado a la bandeja del usuario ID: {}", idUsuario);
            return sinPermiso();
        }
        logger.info("Listando notificaciones del usuario ID: {}", idUsuario);
        List<Notificacion> notificaciones = notificacionService.listar(idUsuario);
        return ResponseEntity.ok(notificaciones);
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<?> marcarComoLeida(@PathVariable Long id) {
        Optional<Long> idToken = UsuarioAutenticado.idActual();
        if (idToken.isEmpty()) {
            return sinSesion();
        }
        logger.info("Marcando notificacion ID: {} como leida", id);
        return ResponseEntity.ok(notificacionService.marcarLeida(id, idToken.get(), true));
    }

    @PatchMapping("/{id}/no-leida")
    public ResponseEntity<?> marcarComoNoLeida(@PathVariable Long id) {
        Optional<Long> idToken = UsuarioAutenticado.idActual();
        if (idToken.isEmpty()) {
            return sinSesion();
        }
        logger.info("Marcando notificacion ID: {} como no leida", id);
        return ResponseEntity.ok(notificacionService.marcarLeida(id, idToken.get(), false));
    }

    @PatchMapping("/{id}/archivar")
    public ResponseEntity<?> archivar(@PathVariable Long id) {
        Optional<Long> idToken = UsuarioAutenticado.idActual();
        if (idToken.isEmpty()) {
            return sinSesion();
        }
        logger.info("Archivando notificacion ID: {}", id);
        return ResponseEntity.ok(notificacionService.archivar(id, idToken.get()));
    }

    @ExceptionHandler(NotificacionException.class)
    public ResponseEntity<ApiError> manejarError(NotificacionException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiError(ex.getStatus().value(), ex.getMessage(), ex.getErrores()));
    }

    private ResponseEntity<ApiError> sinSesion() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(HttpStatus.UNAUTHORIZED.value(),
                        "Debes iniciar sesion para gestionar tus notificaciones", null));
    }

    private ResponseEntity<ApiError> sinPermiso() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(HttpStatus.FORBIDDEN.value(),
                        "No puedes gestionar las notificaciones de otro usuario", null));
    }
}
