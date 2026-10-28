package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.service.MetricasCasosService;
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
 * HU-33: indicadores de casos y reportes para el panel de administracion.
 *
 * GET /api/metricas/casos?desde=2026-10-01&amp;hasta=2026-10-31
 *
 * Vive bajo /api/metricas, asi que SecurityConfig ya lo deja solo para el administrador
 * (la misma regla de HU-34). Responde los periodos invalidos con el mismo ApiError que
 * {@link MetricasController}.
 */
@RestController
@RequestMapping("/api/metricas/casos")
public class MetricasCasosController {

    private static final Logger logger = LoggerFactory.getLogger(MetricasCasosController.class);

    private final MetricasCasosService metricasCasosService;

    public MetricasCasosController(MetricasCasosService metricasCasosService) {
        this.metricasCasosService = metricasCasosService;
    }

    @GetMapping
    public ResponseEntity<?> obtenerMetricas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        logger.info("Consultando metricas de casos del periodo {} a {}", desde, hasta);
        try {
            return ResponseEntity.ok(metricasCasosService.calcular(desde, hasta));
        } catch (IllegalArgumentException periodoInvalido) {
            logger.warn("Periodo de metricas de casos invalido: {}", periodoInvalido.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiError(HttpStatus.BAD_REQUEST.value(), periodoInvalido.getMessage(),
                            Map.of("periodo", periodoInvalido.getMessage())));
        }
    }
}
