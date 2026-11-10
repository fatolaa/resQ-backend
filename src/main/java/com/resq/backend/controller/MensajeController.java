package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.service.ConversacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/mensajes")
public class MensajeController {

    private final ConversacionService conversacionService;

    public MensajeController(ConversacionService conversacionService) {
        this.conversacionService = conversacionService;
    }

    @DeleteMapping("/{idMensaje}")
    public ResponseEntity<?> eliminar(@PathVariable Long idMensaje) {
        Optional<Long> idUsuario = UsuarioAutenticado.idActual();
        if (idUsuario.isEmpty()) {
            return sinSesion();
        }
        conversacionService.eliminarMensaje(idUsuario.get(), idMensaje);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<ApiError> sinSesion() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(HttpStatus.UNAUTHORIZED.value(),
                        "Debes iniciar sesion para usar los mensajes", null));
    }
}
