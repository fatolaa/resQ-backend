package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.ReporteEstadoDTO;
import com.resq.backend.dto.SeguimientoCasoRequestDTO;
import com.resq.backend.entity.EstadoRevision;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.Reporte;
import com.resq.backend.entity.SeguimientoCaso;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.ReporteRepository;
import com.resq.backend.repository.SeguimientoCasoRepository;
import com.resq.backend.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private static final Logger logger = LoggerFactory.getLogger(ReporteController.class);

    private static final Set<String> ESTADOS_PERMITIDOS =
            Set.of("PENDIENTE", "EN_PROCESO", "RESUELTO", "CANCELADO");

    private static final Set<String> ORDENES_PERMITIDOS = Set.of("RECIENTES", "ANTIGUOS");

    private final ReporteRepository reporteRepository;
    private final OrganizacionRepository organizacionRepository; // HU-21
    private final SeguimientoCasoRepository seguimientoCasoRepository; // HU-21

    public ReporteController(ReporteRepository reporteRepository,
                              OrganizacionRepository organizacionRepository,
                              SeguimientoCasoRepository seguimientoCasoRepository) {
        this.reporteRepository = reporteRepository;
        this.organizacionRepository = organizacionRepository;
        this.seguimientoCasoRepository = seguimientoCasoRepository;
    }

    public ResponseEntity<?> obtenerReportes(String estado) {
        return obtenerReportes(estado, null, null);
    }

    @GetMapping
    public ResponseEntity<?> obtenerReportes(
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "busqueda", required = false) String busqueda,
            @RequestParam(name = "orden", required = false) String orden) {
        logger.info("Listando reportes. estado={}, busqueda={}, orden={}", estado, busqueda, orden);

        List<String> estados = parsearEstados(estado);

        ResponseEntity<?> error = validarEstados(estados);
        if (error != null) {
            logger.warn("Estados inválidos solicitados: {}", estados);
            return error;
        }

        ResponseEntity<?> errorOrden = validarOrden(orden);
        if (errorOrden != null) {
            logger.warn("Orden inválido solicitado: {}", orden);
            return errorOrden;
        }

        String texto = busqueda == null ? "" : busqueda.trim();
        Sort sort = toSort(orden);

        if (!texto.isEmpty()) {
            return ResponseEntity.ok(estados.isEmpty()
                    ? soloVisibles(reporteRepository.buscar(texto, sort))
                    : soloVisibles(reporteRepository.buscarPorEstados(texto, estados, sort)));
        }

        if (estados.isEmpty()) {
            return ResponseEntity.ok(sort == null
                    ? soloVisibles(reporteRepository.findAll())
                    : soloVisibles(reporteRepository.findAll(sort)));
        }

        return ResponseEntity.ok(sort == null
                ? soloVisibles(reporteRepository.findByEstadoIn(estados))
                : soloVisibles(reporteRepository.findByEstadoIn(estados, sort)));
    }

    public ResponseEntity<List<Reporte>> obtenerReportes() {
        logger.info("Listando todos los reportes visibles");
        return ResponseEntity.ok(soloVisibles(reporteRepository.findAll()));
    }

    private static List<Reporte> soloVisibles(List<Reporte> reportes) {
        if (reportes == null) {
            return List.of();
        }
        return reportes.stream()
                .filter(r -> !EstadoRevision.RECHAZADO.equals(r.getEstadoRevision()))
                .toList();
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
        logger.info("Listando reportes del usuario ID: {}", idUsuario);
        return ResponseEntity.ok(reporteRepository.findByIdUsuario(idUsuario));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reporte> obtenerReportePorId(@PathVariable Long id) {
        logger.info("Consultando reporte ID: {}", id);
        return reporteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    logger.warn("Reporte ID: {} no encontrado", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<Reporte> crearReporte(@Valid @RequestBody Reporte reporte) {
        logger.info("Iniciando creación de reporte. Usuario ID: {}, tipo: {}",
                reporte.getIdUsuario(), reporte.getTipoCaso());
        Reporte guardado = reporteRepository.save(reporte);
        logger.info("Reporte creado correctamente con ID: {}", guardado.getIdReporte());
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reporte> actualizarReporte(@PathVariable Long id, @Valid @RequestBody Reporte datosReporte) {
        logger.info("Actualizando reporte ID: {}", id);
        return reporteRepository.findById(id)
                .map(reporte -> {
                    reporte.setIdUsuario(datosReporte.getIdUsuario());
                    reporte.setTipoCaso(datosReporte.getTipoCaso());
                    reporte.setDescripcion(datosReporte.getDescripcion());
                    reporte.setEstado(datosReporte.getEstado());
                    reporte.setFotoUrl(datosReporte.getFotoUrl());
                    reporte.setLatitud(datosReporte.getLatitud());
                    reporte.setLongitud(datosReporte.getLongitud());
                    Reporte guardado = reporteRepository.save(reporte);
                    logger.info("Reporte ID: {} actualizado correctamente", id);
                    return ResponseEntity.ok(guardado);
                })
                .orElseGet(() -> {
                    logger.warn("Reporte ID: {} no encontrado para actualizar", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarReporte(@PathVariable Long id) {
        logger.info("Solicitud de eliminación del reporte ID: {}", id);
        if (!reporteRepository.existsById(id)) {
            logger.warn("Reporte ID: {} no encontrado para eliminar", id);
            return ResponseEntity.notFound().build();
        }
        reporteRepository.deleteById(id);
        logger.info("Reporte ID: {} eliminado correctamente", id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id,
                                           @Valid @RequestBody ReporteEstadoDTO datos) {
        logger.info("Cambiando estado del reporte ID: {} a {}", id, datos.estado());

        Reporte reporte = reporteRepository.findById(id).orElse(null);
        if (reporte == null) {
            logger.warn("Reporte ID: {} no encontrado para cambio de estado", id);
            return ResponseEntity.notFound().build();
        }

        // HU-21: si el caso ya fue tomado por una organización, solo esa organización
        // (o un admin) puede cambiarle el estado. No afecta a admin ni a voluntarios
        // en casos sin organización asignada.
        if (reporte.getIdOrganizacion() != null
                && !UsuarioAutenticado.esAdmin()
                && !puedeVerOrganizacion(reporte.getIdOrganizacion())) {
            logger.warn("Usuario sin permiso intentó cambiar estado del caso ID: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError(HttpStatus.FORBIDDEN.value(),
                            "Solo la organización asignada puede editar este caso",
                            Map.of("idReporte", String.valueOf(id))));
        }

        String destino = datos.estado().trim().toUpperCase();

        if (!ESTADOS_PERMITIDOS.contains(destino)) {
            logger.warn("Estado inválido solicitado: {}", datos.estado());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError(HttpStatus.BAD_REQUEST.value(),
                            "estado invalido: " + datos.estado() + ". Permitidos: "
                                    + String.join(", ", ESTADOS_PERMITIDOS),
                            Map.of("estado", datos.estado())));
        }

        if (destino.equals(reporte.getEstado())) {
            logger.warn("El reporte ID: {} ya está en estado {}", id, destino);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError(HttpStatus.CONFLICT.value(),
                            "el reporte ya se encuentra en " + destino,
                            Map.of("estado", destino)));
        }

        Set<String> permitidos = transicionesPermitidas(reporte.getEstado());
        if (!permitidos.contains(destino)) {
            logger.warn("Transición no permitida para reporte ID: {} de {} a {}",
                    id, reporte.getEstado(), destino);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError(HttpStatus.CONFLICT.value(),
                            "no se puede pasar de " + reporte.getEstado() + " a " + destino
                                    + ". Desde " + reporte.getEstado() + " se permite: "
                                    + String.join(", ", permitidos),
                            Map.of("estado", destino)));
        }

        reporte.setEstado(destino);
        Reporte guardado = reporteRepository.save(reporte);
        logger.info("Estado del reporte ID: {} actualizado a {}", id, destino);
        return ResponseEntity.ok(guardado);
    }

    private Set<String> transicionesPermitidas(String estadoActual) {
        return switch (estadoActual) {
            case "PENDIENTE" -> Set.of("EN_PROCESO", "CANCELADO");
            case "EN_PROCESO" -> Set.of("RESUELTO", "CANCELADO", "PENDIENTE");
            case "RESUELTO" -> Set.of("EN_PROCESO", "PENDIENTE");
            case "CANCELADO" -> Set.of("PENDIENTE");
            default -> Set.of();
        };
    }

    // ============================================================
    // HU-21: gestión de casos por una organización
    // ============================================================

    /**
     * true si el usuario autenticado puede ver/gestionar los casos de
     * idOrganizacion: es admin, o es el representante de esa organización.
     */
    private boolean puedeVerOrganizacion(Long idOrganizacion) {
        if (UsuarioAutenticado.esAdmin()) {
            return true;
        }
        Optional<Long> idActual = UsuarioAutenticado.idActual();
        if (idActual.isEmpty()) {
            return false;
        }
        return organizacionRepository.findByIdRepresentante(idActual.get())
                .map(org -> org.getIdOrganizacion().equals(idOrganizacion))
                .orElse(false);
    }

    /**
     * HU-21: casos que ninguna organización ha tomado todavía, para que una
     * organización verificada pueda elegir cuáles atender.
     */
    @GetMapping("/disponibles")
    public ResponseEntity<List<Reporte>> obtenerDisponibles() {
        logger.info("Listando casos disponibles (sin organización asignada)");
        return ResponseEntity.ok(soloVisibles(reporteRepository.findByIdOrganizacionIsNull()));
    }

    /**
     * HU-21: panel de "mis casos" de una organización. estados= es opcional,
     * mismo formato que en GET /api/reportes (coma-separado).
     */
    @GetMapping("/organizacion/{idOrganizacion}")
    public ResponseEntity<?> obtenerPorOrganizacion(
            @PathVariable Long idOrganizacion,
            @RequestParam(name = "estados", required = false) String estado,
            @RequestParam(name = "orden", required = false) String orden) {

        if (!puedeVerOrganizacion(idOrganizacion)) {
            logger.warn("Usuario sin permiso intentó ver casos de organización ID: {}", idOrganizacion);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError(HttpStatus.FORBIDDEN.value(),
                            "No puedes ver los casos de esta organización",
                            Map.of("idOrganizacion", String.valueOf(idOrganizacion))));
        }

        List<String> estados = parsearEstados(estado);
        ResponseEntity<?> error = validarEstados(estados);
        if (error != null) {
            return error;
        }
        ResponseEntity<?> errorOrden = validarOrden(orden);
        if (errorOrden != null) {
            return errorOrden;
        }

        Sort sort = toSort(orden);
        List<Reporte> reportes;
        if (!estados.isEmpty()) {
            reportes = sort == null
                    ? reporteRepository.findByIdOrganizacionAndEstadoIn(idOrganizacion, estados)
                    : reporteRepository.findByIdOrganizacionAndEstadoIn(idOrganizacion, estados, sort);
        } else {
            reportes = sort == null
                    ? reporteRepository.findByIdOrganizacion(idOrganizacion)
                    : reporteRepository.findByIdOrganizacion(idOrganizacion, sort);
        }

        return ResponseEntity.ok(reportes);
    }

    /**
     * HU-21: una organización verificada toma un caso disponible.
     * Solo se puede aceptar un caso que no tenga organización asignada.
     */
    @PatchMapping("/{id}/aceptar")
    public ResponseEntity<?> aceptarCaso(@PathVariable Long id) {
        Optional<Long> idActual = UsuarioAutenticado.idActual();
        if (idActual.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError(HttpStatus.UNAUTHORIZED.value(), "No autenticado", Map.of()));
        }

        Optional<Organizacion> organizacion = organizacionRepository.findByIdRepresentante(idActual.get());
        if (organizacion.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError(HttpStatus.FORBIDDEN.value(),
                            "Solo una organización puede aceptar casos", Map.of()));
        }
        if (!organizacion.get().puedeGestionarCasos()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError(HttpStatus.FORBIDDEN.value(),
                            "Tu organización todavía no está verificada", Map.of()));
        }

        Reporte reporte = reporteRepository.findById(id).orElse(null);
        if (reporte == null) {
            logger.warn("Reporte ID: {} no encontrado para aceptar caso", id);
            return ResponseEntity.notFound().build();
        }
        if (reporte.getIdOrganizacion() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError(HttpStatus.CONFLICT.value(),
                            "Este caso ya fue tomado por otra organización", Map.of("idReporte", String.valueOf(id))));
        }

        reporte.setIdOrganizacion(organizacion.get().getIdOrganizacion());
        Reporte guardado = reporteRepository.save(reporte);
        logger.info("Organización ID: {} aceptó el caso ID: {}", organizacion.get().getIdOrganizacion(), id);
        return ResponseEntity.ok(guardado);
    }

    /**
     * HU-21: historial de seguimiento de un caso (notas + fotos), más reciente primero.
     */
    @GetMapping("/{id}/seguimientos")
    public ResponseEntity<?> obtenerSeguimientos(@PathVariable Long id) {
        Reporte reporte = reporteRepository.findById(id).orElse(null);
        if (reporte == null) {
            return ResponseEntity.notFound().build();
        }
        Long idOrganizacionCaso = reporte.getIdOrganizacion();
        if (idOrganizacionCaso == null || !puedeVerOrganizacion(idOrganizacionCaso)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError(HttpStatus.FORBIDDEN.value(),
                            "No puedes ver el seguimiento de este caso", Map.of("idReporte", String.valueOf(id))));
        }

        return ResponseEntity.ok(seguimientoCasoRepository.findByIdReporteOrderByFechaCreacionDesc(id));
    }

    /**
     * HU-21: agrega una entrada de seguimiento (nota + foto opcional) a un caso.
     * Solo la organización asignada al caso puede hacerlo.
     */
    @PostMapping("/{id}/seguimientos")
    public ResponseEntity<?> agregarSeguimiento(@PathVariable Long id,
                                                 @Valid @RequestBody SeguimientoCasoRequestDTO body) {
        Reporte reporte = reporteRepository.findById(id).orElse(null);
        if (reporte == null) {
            return ResponseEntity.notFound().build();
        }
        Long idOrganizacionCaso = reporte.getIdOrganizacion();
        if (idOrganizacionCaso == null || !puedeVerOrganizacion(idOrganizacionCaso)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError(HttpStatus.FORBIDDEN.value(),
                            "Solo la organización asignada puede agregar seguimiento a este caso",
                            Map.of("idReporte", String.valueOf(id))));
        }

        SeguimientoCaso seguimiento = new SeguimientoCaso();
        seguimiento.setIdReporte(id);
        seguimiento.setIdOrganizacion(idOrganizacionCaso);
        seguimiento.setNota(body.nota());
        seguimiento.setFotoUrl(body.fotoUrl());

        SeguimientoCaso guardado = seguimientoCasoRepository.save(seguimiento);
        logger.info("Seguimiento agregado al caso ID: {} por organización ID: {}", id, idOrganizacionCaso);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }
}