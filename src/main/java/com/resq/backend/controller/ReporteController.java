package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.ReporteRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
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

    /**
     * HU-24: ordenes que el administrador puede pedir al listar los casos. Sin el
     * parametro la consulta conserva su orden natural, que es lo que espera el resto
     * de consumidores de este endpoint (mapa, perfil); el panel siempre lo manda.
     */
    private static final Set<String> ORDENES_PERMITIDOS = Set.of("RECIENTES", "ANTIGUOS");

    private final ReporteRepository reporteRepository;

    public ReporteController(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    /**
     * Atajo para las llamadas que solo filtran por estado: no lleva anotacion de
     * mapeo porque la ruta GET /api/reportes es la de arriba.
     */
    public ResponseEntity<?> obtenerReportes(String estado) {
        return obtenerReportes(estado, null, null);
    }

    @GetMapping
    public ResponseEntity<?> obtenerReportes(
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "busqueda", required = false) String busqueda,
            @RequestParam(name = "orden", required = false) String orden) {
        List<String> estados = parsearEstados(estado);

        ResponseEntity<?> error = validarEstados(estados);
        if (error != null) {
            return error;
        }

        ResponseEntity<?> errorOrden = validarOrden(orden);
        if (errorOrden != null) {
            return errorOrden;
        }

        String texto = busqueda == null ? "" : busqueda.trim();
        Sort sort = toSort(orden);

        if (!texto.isEmpty()) {
            return ResponseEntity.ok(estados.isEmpty()
                    ? reporteRepository.buscar(texto, sort)
                    : reporteRepository.buscarPorEstados(texto, estados, sort));
        }

        if (estados.isEmpty()) {
            return ResponseEntity.ok(sort == null
                    ? reporteRepository.findAll()
                    : reporteRepository.findAll(sort));
        }

        return ResponseEntity.ok(sort == null
                ? reporteRepository.findByEstadoIn(estados)
                : reporteRepository.findByEstadoIn(estados, sort));
    }

    public ResponseEntity<List<Reporte>> obtenerReportes() {
        return ResponseEntity.ok(reporteRepository.findAll());
    }

    private ResponseEntity<?> validarEstados(List<String> estados) {
        List<String> invalidos = estados.stream()
                .filter(valor -> !ESTADOS_PERMITIDOS.contains(valor))
                .toList();

        if (invalidos.isEmpty()) {
            return null;
        }

        String detalle = String.join(", ", invalidos);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(),
                        "estado invalido: " + detalle + ". Permitidos: " + String.join(", ", ESTADOS_PERMITIDOS),
                        Map.of("estado", detalle)));
    }

    private ResponseEntity<?> validarOrden(String orden) {
        if (orden == null || orden.isBlank()) {
            return null;
        }

        String normalizado = orden.trim().toUpperCase();
        if (ORDENES_PERMITIDOS.contains(normalizado)) {
            return null;
        }

        String detalle = orden.trim();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(),
                        "orden invalido: " + detalle + ". Permitidos: " + String.join(", ", ORDENES_PERMITIDOS),
                        Map.of("orden", detalle)));
    }

    private Sort toSort(String orden) {
        if (orden == null || orden.isBlank()) {
            return null;
        }

        Sort.Direction direccion = orden.trim().equalsIgnoreCase("ANTIGUOS")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direccion, "fechaCreacion");
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