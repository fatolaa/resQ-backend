package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.ReporteRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private static final Set<String> ESTADOS_PERMITIDOS =
            Set.of("PENDIENTE", "EN_PROCESO", "RESUELTO", "CANCELADO");

    private final ReporteRepository reporteRepository;

    public ReporteController(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    @GetMapping
    public ResponseEntity<?> obtenerReportes(
            @RequestParam(name = "estado", required = false) String estado) {
        List<String> estados = parsearEstados(estado);

        if (estados.isEmpty()) {
            return ResponseEntity.ok(reporteRepository.findAll());
        }

        List<String> invalidos = estados.stream()
                .filter(valor -> !ESTADOS_PERMITIDOS.contains(valor))
                .toList();

        if (!invalidos.isEmpty()) {
            String detalle = String.join(", ", invalidos);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError(HttpStatus.BAD_REQUEST.value(),
                            "estado invalido: " + detalle + ". Permitidos: " + String.join(", ", ESTADOS_PERMITIDOS),
                            Map.of("estado", detalle)));
        }

        return ResponseEntity.ok(reporteRepository.findByEstadoIn(estados));
    }

    private List<String> parsearEstados(String estado) {
        if (estado == null || estado.isBlank()) {
            return List.of();
        }
        return Arrays.stream(estado.split(","))
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Reporte>> obtenerReportesPorUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(reporteRepository.findByIdUsuario(idUsuario));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reporte> obtenerReportePorId(@PathVariable Long id) {
        return reporteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Reporte> crearReporte(@Valid @RequestBody Reporte reporte) {
        Reporte guardado = reporteRepository.save(reporte);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reporte> actualizarReporte(@PathVariable Long id, @Valid @RequestBody Reporte datosReporte) {
        return reporteRepository.findById(id)
                .map(reporte -> {
                    reporte.setIdUsuario(datosReporte.getIdUsuario());
                    reporte.setTipoCaso(datosReporte.getTipoCaso());
                    reporte.setDescripcion(datosReporte.getDescripcion());
                    reporte.setEstado(datosReporte.getEstado());
                    reporte.setFotoUrl(datosReporte.getFotoUrl());
                    reporte.setLatitud(datosReporte.getLatitud());
                    reporte.setLongitud(datosReporte.getLongitud());
                    return ResponseEntity.ok(reporteRepository.save(reporte));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarReporte(@PathVariable Long id) {
        if (!reporteRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        reporteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
