package com.resq.backend.controller;

import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.ReporteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteRepository reporteRepository;

    public ReporteController(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    @GetMapping
    public ResponseEntity<List<Reporte>> obtenerReportes() {
        return ResponseEntity.ok(reporteRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reporte> obtenerReportePorId(@PathVariable Long id) {
        return reporteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Reporte> crearReporte(@RequestBody Reporte reporte) {
        Reporte guardado = reporteRepository.save(reporte);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reporte> actualizarReporte(@PathVariable Long id, @RequestBody Reporte datosReporte) {
        return reporteRepository.findById(id)
                .map(reporte -> {
                    reporte.setIdUsuario(datosReporte.getIdUsuario());
                    reporte.setTipoCaso(datosReporte.getTipoCaso());
                    reporte.setDescripcion(datosReporte.getDescripcion());
                    reporte.setEstado(datosReporte.getEstado());
                    reporte.setFotoUrl(datosReporte.getFotoUrl());
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