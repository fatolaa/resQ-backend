package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.PreferenciaNotificacionDTO;
import com.resq.backend.dto.PreferenciaNotificacionRequestDTO;
import com.resq.backend.entity.PreferenciaNotificacion;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.service.PreferenciaNotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * HU-36a: endpoints para que el usuario gestione sus propias preferencias de
 * notificacion.
 *
 * La ruta cuelga de /api/usuarios/me a proposito: el "me" se resuelve con el id
 * del token (UsuarioAutenticado), nunca con un id que mande el cliente. Un
 * usuario no puede leer ni editar las preferencias de otra cuenta.
 */
@RestController
@RequestMapping("/api/usuarios/me/preferencias-notificacion")
public class PreferenciaNotificacionController {

    private static final Logger logger = LoggerFactory.getLogger(PreferenciaNotificacionController.class);

    private final PreferenciaNotificacionService preferenciaService;

    public PreferenciaNotificacionController(PreferenciaNotificacionService preferenciaService) {
        this.preferenciaService = preferenciaService;
    }

    @GetMapping
    public ResponseEntity<?> obtenerMisPreferencias() {
        Optional<Long> idUsuario = UsuarioAutenticado.idActual();
        if (idUsuario.isEmpty()) {
            return noAutenticado();
        }
        logger.info("Consultando preferencias de notificacion del usuario ID: {}", idUsuario.get());
        return ResponseEntity.ok(toDTO(preferenciaService.obtenerOCrear(idUsuario.get())));
    }

    @PutMapping
    public ResponseEntity<?> actualizarMisPreferencias(@RequestBody PreferenciaNotificacionRequestDTO cambios) {
        Optional<Long> idUsuario = UsuarioAutenticado.idActual();
        if (idUsuario.isEmpty()) {
            return noAutenticado();
        }
        logger.info("Actualizando preferencias de notificacion del usuario ID: {}", idUsuario.get());
        return ResponseEntity.ok(toDTO(preferenciaService.actualizar(idUsuario.get(), cambios)));
    }

    private ResponseEntity<ApiError> noAutenticado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(HttpStatus.UNAUTHORIZED.value(), "Debes iniciar sesion", null));
    }

    private PreferenciaNotificacionDTO toDTO(PreferenciaNotificacion preferencia) {
        return new PreferenciaNotificacionDTO(
                preferencia.isNotificarSolicitudAprobada(),
                preferencia.isNotificarSolicitudRechazada(),
                preferencia.isNotificarReporteCerca(),
                preferencia.isNotificarChatMensaje());
    }
}
