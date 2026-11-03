package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.DecisionOrganizacionRequestDTO;
import com.resq.backend.dto.OrganizacionAdminDTO;
import com.resq.backend.dto.RevisionOrganizacionDTO;
import com.resq.backend.exception.OrganizacionException;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.service.AdminOrganizacionService;
import com.resq.backend.service.DocumentoStorageService.DocumentoAlmacenado;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
 * - GET /api/admin/organizaciones/{id}/documento                  documento de verificacion (HU-32)
 * - GET /api/admin/organizaciones/{id}/revisiones                 historial de verificacion (HU-32)
 * - PUT /api/admin/organizaciones/{id}/aprobar   {nota?}          pasa a VERIFICADA
 * - PUT /api/admin/organizaciones/{id}/rechazar  {nota}           pasa a RECHAZADA (motivo obligatorio)
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

    /**
     * HU-32: descarga el documento de verificacion. Va "inline" para que el navegador lo
     * muestre (PDF o imagen) y sin cache publica, porque es un documento privado.
     */
    @GetMapping("/{idOrganizacion}/documento")
    public ResponseEntity<Resource> documento(@PathVariable Long idOrganizacion) {
        DocumentoAlmacenado documento = adminOrganizacionService.documento(idOrganizacion);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(documento.contentType()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(documento.nombre()).build().toString())
                .body(documento.recurso());
    }

    @GetMapping("/{idOrganizacion}/revisiones")
    public ResponseEntity<List<RevisionOrganizacionDTO>> historial(@PathVariable Long idOrganizacion) {
        return ResponseEntity.ok(adminOrganizacionService.historial(idOrganizacion));
    }

    @PutMapping("/{idOrganizacion}/aprobar")
    public ResponseEntity<OrganizacionAdminDTO> aprobar(
            @PathVariable Long idOrganizacion,
            @Valid @RequestBody(required = false) DecisionOrganizacionRequestDTO decision) {
        Long idAdmin = idAdministrador();
        logger.info("Administrador ID {} aprueba la organizacion ID {}", idAdmin, idOrganizacion);
        return ResponseEntity.ok(adminOrganizacionService.aprobar(idOrganizacion, idAdmin, notaDe(decision)));
    }

    @PutMapping("/{idOrganizacion}/rechazar")
    public ResponseEntity<OrganizacionAdminDTO> rechazar(
            @PathVariable Long idOrganizacion,
            @Valid @RequestBody(required = false) DecisionOrganizacionRequestDTO decision) {
        Long idAdmin = idAdministrador();
        logger.info("Administrador ID {} rechaza la organizacion ID {}", idAdmin, idOrganizacion);
        return ResponseEntity.ok(adminOrganizacionService.rechazar(idOrganizacion, idAdmin, notaDe(decision)));
    }

    private static String notaDe(DecisionOrganizacionRequestDTO decision) {
        return decision == null ? null : decision.nota();
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
