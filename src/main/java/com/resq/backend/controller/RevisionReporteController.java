package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.DecisionRevisionDTO;
import com.resq.backend.entity.EstadoRevision;
import com.resq.backend.entity.Notificacion;
import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.NotificacionRepository;
import com.resq.backend.repository.ReporteRepository;
import com.resq.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * HU-19: un voluntario (o admin) valida los casos reportados.
 *
 * - GET  /api/reportes/revision/pendientes?idRevisor=  cola de casos por revisar
 * - POST /api/reportes/revision/{idReporte}/decision   aprobar o rechazar con nota
 *
 * Al decidir se notifica al dueño del reporte. Los reportes rechazados dejan de
 * aparecer en el listado público (ver ReporteController).
 *
 * Nota: el proyecto aún no tiene autenticación por token, así que el revisor se
 * identifica con idRevisor y se valida su rol contra la base de datos.
 */
@RestController
@RequestMapping("/api/reportes/revision")
public class RevisionReporteController {

    static final int NOTA_MAXIMA = 500;

    private final ReporteRepository reporteRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;

    public RevisionReporteController(ReporteRepository reporteRepository,
                                     UsuarioRepository usuarioRepository,
                                     NotificacionRepository notificacionRepository) {
        this.reporteRepository = reporteRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionRepository = notificacionRepository;
    }

    @GetMapping("/pendientes")
    public ResponseEntity<?> listarPendientes(@RequestParam("idRevisor") Long idRevisor) {
        if (!puedeRevisar(idRevisor)) {
            return sinPermiso();
        }
        return ResponseEntity.ok(reporteRepository.findPendientesDeRevision());
    }

    @PostMapping("/{idReporte}/decision")
    @Transactional
    public ResponseEntity<?> decidir(@PathVariable Long idReporte, @RequestBody DecisionRevisionDTO decision) {
        if (!puedeRevisar(decision.idRevisor())) {
            return sinPermiso();
        }

        String accion = decision.decision() == null ? "" : decision.decision().trim().toUpperCase();
        boolean aprobar = accion.equals("APROBAR");
        boolean rechazar = accion.equals("RECHAZAR");
        if (!aprobar && !rechazar) {
            return error(HttpStatus.BAD_REQUEST, "decision debe ser APROBAR o RECHAZAR", "decision");
        }

        String nota = decision.nota() == null ? "" : decision.nota().trim();
        if (rechazar && nota.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Debes indicar el motivo del rechazo", "nota");
        }
        if (nota.length() > NOTA_MAXIMA) {
            return error(HttpStatus.BAD_REQUEST,
                    "La nota no puede superar los " + NOTA_MAXIMA + " caracteres", "nota");
        }

        Optional<Reporte> encontrado = reporteRepository.findById(idReporte);
        if (encontrado.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Reporte no encontrado", "idReporte");
        }

        Reporte reporte = encontrado.get();
        String actual = reporte.getEstadoRevision();
        if (actual != null && !EstadoRevision.PENDIENTE.equals(actual)) {
            return error(HttpStatus.CONFLICT, "La solicitud ya fue revisada: " + actual, "estadoRevision");
        }

        reporte.setEstadoRevision(aprobar ? EstadoRevision.APROBADO : EstadoRevision.RECHAZADO);
        reporte.setNotaRevision(nota.isEmpty() ? null : nota);
        reporte.setIdRevisor(decision.idRevisor());
        reporte.setFechaRevision(LocalDateTime.now());
        reporteRepository.save(reporte);

        notificacionRepository.save(crearNotificacion(reporte, aprobar, nota));

        return ResponseEntity.ok(reporte);
    }

    private Notificacion crearNotificacion(Reporte reporte, boolean aprobada, String nota) {
        String mensaje;
        if (aprobada) {
            mensaje = "Tu reporte #" + reporte.getIdReporte() + " fue aprobado por un voluntario."
                    + (nota.isEmpty() ? "" : " Nota: " + nota);
        } else {
            mensaje = "Tu reporte #" + reporte.getIdReporte() + " fue rechazado. Motivo: " + nota;
        }
        return new Notificacion(
                reporte.getIdUsuario(),
                reporte.getIdReporte(),
                aprobada ? Notificacion.SOLICITUD_APROBADA : Notificacion.SOLICITUD_RECHAZADA,
                mensaje);
    }

    private boolean puedeRevisar(Long idUsuario) {
        if (idUsuario == null) {
            return false;
        }
        return usuarioRepository.findById(idUsuario)
                .map(usuario -> "VOLUNTARIO".equalsIgnoreCase(usuario.getRol())
                        || "ADMIN".equalsIgnoreCase(usuario.getRol()))
                .orElse(false);
    }

    private ResponseEntity<ApiError> sinPermiso() {
        return error(HttpStatus.FORBIDDEN, "Solo voluntarios y administradores pueden revisar solicitudes", "idRevisor");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String mensaje, String campo) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), mensaje, Map.of(campo, mensaje)));
    }
}
