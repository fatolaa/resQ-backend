package com.resq.backend.controller;

import com.resq.backend.entity.Notificacion;
import com.resq.backend.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private static final Logger logger = LoggerFactory.getLogger(NotificacionController.class);

    private final NotificacionRepository notificacionRepository;

    public NotificacionController(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Notificacion>> obtenerPorUsuario(@PathVariable Long idUsuario) {
        logger.info("Listando notificaciones del usuario ID: {}", idUsuario);
        return ResponseEntity.ok(notificacionRepository.findByIdUsuarioOrderByFechaCreacionDesc(idUsuario));
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<Notificacion> marcarComoLeida(@PathVariable Long id) {
        logger.info("Marcando notificación ID: {} como leída", id);
        return notificacionRepository.findById(id)
                .map(notificacion -> {
                    notificacion.setLeida(true);
                    Notificacion guardada = notificacionRepository.save(notificacion);
                    logger.info("Notificación ID: {} marcada como leída", id);
                    return ResponseEntity.ok(guardada);
                })
                .orElseGet(() -> {
                    logger.warn("Notificación ID: {} no encontrada", id);
                    return ResponseEntity.notFound().build();
                });
    }
}