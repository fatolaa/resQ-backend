package com.resq.backend.controller;

import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.ReporteRepository;
import com.resq.backend.service.FotoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteRepository reporteRepository;
    private final FotoService fotoService;

    public ReporteController(ReporteRepository reporteRepository, FotoService fotoService) {
        this.reporteRepository = reporteRepository;
        this.fotoService = fotoService;
    }

    @GetMapping
    public ResponseEntity<List<Reporte>> obtenerReportes() {
        return ResponseEntity.ok(reporteRepository.findAll());
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

    @PostMapping("/{id}/foto")
public ResponseEntity<?> adjuntarFoto(
        @PathVariable Long id,
        @RequestParam("foto") MultipartFile foto) {

    return reporteRepository.findById(id)
            .map(reporte -> {
                try {
                    String nombreArchivo = fotoService.guardarFoto(foto);
                    reporte.setFotoUrl(nombreArchivo);
                    reporteRepository.save(reporte);

                    return ResponseEntity.ok(reporte);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error al guardar la fotografía");
                }
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
}
}