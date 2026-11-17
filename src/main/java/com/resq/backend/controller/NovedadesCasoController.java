package com.resq.backend.controller;

import com.resq.backend.dto.NovedadCasoDTO;
import com.resq.backend.entity.EstadoRevision;
import com.resq.backend.repository.ReporteRepository;
import com.resq.backend.repository.SeguimientoCasoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rediseño: novedades de un caso para su linea de tiempo publica.
 *
 * Son las mismas notas de seguimiento que escribe la organizacion (HU-21), pero
 * solo con fecha, nota y foto. GET /api/reportes/{id}/seguimientos sigue siendo de
 * la organizacion del caso. Un reporte rechazado no tiene novedades publicas (404).
 */
@RestController
@RequestMapping("/api/reportes")
public class NovedadesCasoController {

    private final ReporteRepository reporteRepository;
    private final SeguimientoCasoRepository seguimientoCasoRepository;

    public NovedadesCasoController(ReporteRepository reporteRepository,
                                   SeguimientoCasoRepository seguimientoCasoRepository) {
        this.reporteRepository = reporteRepository;
        this.seguimientoCasoRepository = seguimientoCasoRepository;
    }

    @GetMapping("/{idReporte}/novedades")
    public ResponseEntity<List<NovedadCasoDTO>> novedades(@PathVariable Long idReporte) {
        return reporteRepository.findById(idReporte)
                .filter(r -> !EstadoRevision.RECHAZADO.equals(r.getEstadoRevision()))
                .map(r -> ResponseEntity.ok(seguimientoCasoRepository
                        .findByIdReporteOrderByFechaCreacionDesc(idReporte).stream()
                        .map(NovedadCasoDTO::de)
                        .toList()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
