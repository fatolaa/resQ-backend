package net.javaguides.springboot.controller;

import net.javaguides.springboot.exception.ResourceNotFoundException;
import net.javaguides.springboot.model.Reporte;
import net.javaguides.springboot.repository.ReporteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "http://localhost:4200")
public class ReporteController {

    @Autowired
    private ReporteRepository reporteRepository;

    @GetMapping
    public List<Reporte> obtenerReportes() {
        return reporteRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reporte> obtenerReportePorId(@PathVariable Long id) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reporte no encontrado con id: " + id));

        return ResponseEntity.ok(reporte);
    }

    @PostMapping
    public Reporte crearReporte(@RequestBody Reporte reporte) {
        return reporteRepository.save(reporte);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reporte> actualizarReporte(
            @PathVariable Long id,
            @RequestBody Reporte datosReporte) {

        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reporte no encontrado con id: " + id));

        reporte.setIdUsuario(datosReporte.getIdUsuario());
        reporte.setTipoCaso(datosReporte.getTipoCaso());
        reporte.setDescripcion(datosReporte.getDescripcion());
        reporte.setEstado(datosReporte.getEstado());
        reporte.setFotoUrl(datosReporte.getFotoUrl());

        return ResponseEntity.ok(reporteRepository.save(reporte));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> eliminarReporte(@PathVariable Long id) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reporte no encontrado con id: " + id));

        reporteRepository.delete(reporte);

        Map<String, Boolean> respuesta = new HashMap<>();
        respuesta.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(respuesta);
    }
}