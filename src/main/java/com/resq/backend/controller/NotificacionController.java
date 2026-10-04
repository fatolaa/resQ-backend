package com.resq.backend.controller;

import com.resq.backend.entity.Notificacion;
import com.resq.backend.repository.NotificacionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** HU-19: bandeja mínima de notificaciones del usuario. */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;

    public NotificacionController(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Notificacion>> obtenerPorUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(notificacionRepository.findByIdUsuarioOrderByFechaCreacionDesc(idUsuario));
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<Notificacion> marcarComoLeida(@PathVariable Long id) {
        return notificacionRepository.findById(id)
                .map(notificacion -> {
                    notificacion.setLeida(true);
                    return ResponseEntity.ok(notificacionRepository.save(notificacion));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
