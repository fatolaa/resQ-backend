package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.service.MetricasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * HU-34: indicadores de la comunidad para el panel de administracion.
 *
 * Solo lo consulta el administrador (lo restringe SecurityConfig). Las fechas van
 * en formato ISO (2026-10-01) y el periodo incluye los dos extremos.
 */
@RestController
@RequestMapping("/api/metricas")
public class MetricasController {

    private static final Logger logger = LoggerFactory.getLogger(MetricasController.class);

    private final MetricasService metricasService;

    public MetricasController(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    // Las fechas no son "required" para Spring: si falta una, el servicio responde con
    // el mismo ApiError que el resto de errores de periodo, en vez del 400 generico.
    @GetMapping
    public ResponseEntity<?> obtenerMetricas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        logger.info("Consultando metricas del periodo {} a {}", desde, hasta);
        try {
            return ResponseEntity.ok(metricasService.calcular(desde, hasta));
        } catch (IllegalArgumentException periodoInvalido) {
            logger.warn("Periodo de metricas invalido: {}", periodoInvalido.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiError(HttpStatus.BAD_REQUEST.value(), periodoInvalido.getMessage(),
                            Map.of("periodo", periodoInvalido.getMessage())));
        }
    }
}
