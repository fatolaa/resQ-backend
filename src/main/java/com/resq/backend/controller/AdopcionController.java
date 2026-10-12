package com.resq.backend.controller;

import com.resq.backend.dto.AdopcionRequestDTO;
import com.resq.backend.dto.ApiError;
import com.resq.backend.entity.Adopcion;
import com.resq.backend.exception.AdopcionException;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.service.AdopcionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * HU-30: registro y consulta de adopciones de animales rescatados.
 *
 * - POST /api/adopciones                     registra la adopción (organización con sesión)
 * - GET  /api/adopciones/{id}                ficha de la adopción
 * - GET  /api/adopciones/reporte/{idReporte} adopción del caso
 * La consulta solo la permite el servicio a la organización y a quien reportó el caso.
 */
@RestController
@RequestMapping("/api/adopciones")
public class AdopcionController {

    private final AdopcionService adopcionService;

    public AdopcionController(AdopcionService adopcionService) {
        this.adopcionService = adopcionService;
    }

    @PostMapping
    public ResponseEntity<?> registrar(@Valid @RequestBody AdopcionRequestDTO solicitud) {
        Optional<Long> idToken = UsuarioAutenticado.idActual();
        if (idToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError(HttpStatus.UNAUTHORIZED.value(),
                            "Debes iniciar sesión para registrar una adopción", null));
        }
        Adopcion adopcion = adopcionService.registrar(idToken.get(), solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(adopcion);
    }

    @GetMapping("/{idAdopcion}")
    public ResponseEntity<?> obtener(@PathVariable Long idAdopcion) {
        return adopcionService.obtenerVisible(idAdopcion, UsuarioAutenticado.idActual().orElse(null))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/reporte/{idReporte}")
    public ResponseEntity<?> obtenerDeReporte(@PathVariable Long idReporte) {
        return adopcionService.obtenerDeReporteVisible(idReporte, UsuarioAutenticado.idActual().orElse(null))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ExceptionHandler(AdopcionException.class)
    public ResponseEntity<ApiError> manejarError(AdopcionException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiError(ex.getStatus().value(), ex.getMessage(), ex.getErrores()));
    }
}
