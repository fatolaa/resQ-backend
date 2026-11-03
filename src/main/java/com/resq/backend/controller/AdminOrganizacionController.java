package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.OrganizacionAdminDTO;
import com.resq.backend.exception.OrganizacionException;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.service.AdminOrganizacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * HU-25: administracion de organizaciones. Todo /api/admin/** es solo del administrador
 * (lo exige SecurityConfig).
 *
 * - GET /api/admin/organizaciones?estado=PENDIENTE_VERIFICACION   listado (estado opcional)
 * - GET /api/admin/organizaciones/{id}                            detalle en cualquier estado
 * - PUT /api/admin/organizaciones/{id}/aprobar                    pasa a VERIFICADA
 * - PUT /api/admin/organizaciones/{id}/rechazar                   pasa a RECHAZADA
 */
@RestController
@RequestMapping("/api/admin/organizaciones")
public class AdminOrganizacionController {

    private static final Logger logger = LoggerFactory.getLogger(AdminOrganizacionController.class);

    private final AdminOrganizacionService adminOrganizacionService;

    public AdminOrganizacionController(AdminOrganizacionService adminOrganizacionService) {
        this.adminOrganizacionService = adminOrganizacionService;
    }

    @GetMapping
    public ResponseEntity<List<OrganizacionAdminDTO>> listar(@RequestParam(required = false) String estado) {
        return ResponseEntity.ok(adminOrganizacionService.listar(estado));
    }

    @GetMapping("/{idOrganizacion}")
    public ResponseEntity<OrganizacionAdminDTO> detalle(@PathVariable Long idOrganizacion) {
        return ResponseEntity.ok(adminOrganizacionService.detalle(idOrganizacion));
    }

    @PutMapping("/{idOrganizacion}/aprobar")
    public ResponseEntity<OrganizacionAdminDTO> aprobar(@PathVariable Long idOrganizacion) {
        Long idAdmin = idAdministrador();
        logger.info("Administrador ID {} aprueba la organizacion ID {}", idAdmin, idOrganizacion);
        return ResponseEntity.ok(adminOrganizacionService.aprobar(idOrganizacion, idAdmin));
    }

    @PutMapping("/{idOrganizacion}/rechazar")
    public ResponseEntity<OrganizacionAdminDTO> rechazar(@PathVariable Long idOrganizacion) {
        Long idAdmin = idAdministrador();
        logger.info("Administrador ID {} rechaza la organizacion ID {}", idAdmin, idOrganizacion);
        return ResponseEntity.ok(adminOrganizacionService.rechazar(idOrganizacion, idAdmin));
    }

    /** SecurityConfig ya exige sesion de ADMIN, asi que el id siempre esta. */
    private static Long idAdministrador() {
        return UsuarioAutenticado.idActual().orElseThrow(() -> new OrganizacionException(
                HttpStatus.UNAUTHORIZED, "Debes iniciar sesión", Map.of()));
    }

    // Los errores se responden aca mismo con su estado: si se escaparan a /error (que no es
    // publico), el cliente recibiria 401 y el frontend cerraria la sesion del administrador.
    @ExceptionHandler(OrganizacionException.class)
    public ResponseEntity<ApiError> manejarError(OrganizacionException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiError(ex.getStatus().value(), ex.getMessage(), ex.getErrores()));
    }
}
