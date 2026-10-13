package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.CrearSolicitudVoluntarioRequestDTO;
import com.resq.backend.dto.RechazarSolicitudRequestDTO;
import com.resq.backend.dto.SolicitudVoluntarioDetalleDTO;
import com.resq.backend.dto.SolicitudVoluntarioResumenDTO;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.service.SolicitudVoluntarioService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/solicitudes-voluntarios")
public class SolicitudVoluntarioController {

    private static final Logger logger = LoggerFactory.getLogger(SolicitudVoluntarioController.class);

    @Autowired
    private SolicitudVoluntarioService solicitudService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<?> crearSolicitud(Authentication authentication,
                                            @Valid @RequestBody CrearSolicitudVoluntarioRequestDTO request) {
        Long idUsuario = obtenerIdUsuario(authentication);
        logger.info("Creando solicitud de voluntario. Usuario ID: {}", idUsuario);
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        try {
            SolicitudVoluntarioDetalleDTO creada = solicitudService.crearSolicitud(
                    usuario, request.tipoAyuda(), request.mensaje());
            return ResponseEntity.status(HttpStatus.CREATED).body(creada);
        } catch (IllegalStateException e) {
            logger.warn("Solicitud rechazada por regla de negocio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError(HttpStatus.CONFLICT.value(), e.getMessage(), Map.of()));
        }
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SolicitudVoluntarioResumenDTO>> listarPendientes() {
        logger.info("Obteniendo solicitudes de voluntario pendientes");
        List<SolicitudVoluntarioResumenDTO> solicitudes = solicitudService.listarPendientes();
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SolicitudVoluntarioDetalleDTO> obtenerDetalle(@PathVariable Long id) {
        logger.info("Obteniendo detalle de solicitud de voluntario ID: {}", id);
        SolicitudVoluntarioDetalleDTO detalle = solicitudService.obtenerDetalle(id);
        return ResponseEntity.ok(detalle);
    }

    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SolicitudVoluntarioDetalleDTO> aprobarSolicitud(@PathVariable Long id, Authentication authentication) {
        logger.info("Aprobando solicitud de voluntario ID: {}", id);
        Long adminId = obtenerIdUsuario(authentication);
        SolicitudVoluntarioDetalleDTO detalle = solicitudService.aprobarSolicitud(id, adminId);
        return ResponseEntity.ok(detalle);
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SolicitudVoluntarioDetalleDTO> rechazarSolicitud(@PathVariable Long id,
                                                                            @Valid @RequestBody RechazarSolicitudRequestDTO request,
                                                                            Authentication authentication) {
        logger.info("Rechazando solicitud de voluntario ID: {}", id);
        Long adminId = obtenerIdUsuario(authentication);
        SolicitudVoluntarioDetalleDTO detalle = solicitudService.rechazarSolicitud(id, adminId, request);
        return ResponseEntity.ok(detalle);
    }

    private Long obtenerIdUsuario(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails = (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            return Long.parseLong(userDetails.getUsername());
        }
        throw new RuntimeException("No se pudo obtener el ID del usuario autenticado");
    }
}