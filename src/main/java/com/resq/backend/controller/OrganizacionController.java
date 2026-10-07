package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.OrganizacionRegistradaDTO;
import com.resq.backend.dto.OrganizacionRequestDTO;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.exception.OrganizacionException;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.service.OrganizacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

/**
 * HU-20: registro de organizaciones.
 *
 * - POST /api/organizaciones                         registra (multipart: campos + logo opcional)
 * - GET  /api/organizaciones/representante/{id}      organización de un usuario (con su estado)
 * El logo se guarda con el servicio de fotos de HU-10 y se ve en GET /api/reportes/fotos/{nombre}.
 */
@RestController
@RequestMapping("/api/organizaciones")
public class OrganizacionController {

    private final OrganizacionService organizacionService;

    public OrganizacionController(OrganizacionService organizacionService) {
        this.organizacionService = organizacionService;
    }

    /**
     * Todos los parámetros son opcionales a nivel HTTP a propósito: así un campo faltante
     * produce el mismo error de validación (400 con el detalle por campo) que uno inválido.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registrar(
            @RequestParam(name = "idRepresentante", required = false) Long idRepresentante,
            @RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "direccion", required = false) String direccion,
            @RequestParam(name = "telefono", required = false) String telefono,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "descripcion", required = false) String descripcion,
            @RequestParam(name = "logo", required = false) MultipartFile logo) {

        Optional<Long> idToken = UsuarioAutenticado.idActual();
        if (idToken.isPresent() && idRepresentante != null && !idToken.get().equals(idRepresentante)) {
            return prohibido("No puedes registrar una organización a nombre de otro usuario");
        }
        // Con token, manda la identidad del token; sin él (backend sin JWT), la del cliente.
        Long representante = idToken.orElse(idRepresentante);

        OrganizacionRegistradaDTO registrada = organizacionService.registrar(
                new OrganizacionRequestDTO(representante, nombre, tipo, direccion, telefono, email, descripcion),
                logo);
        return ResponseEntity.status(HttpStatus.CREATED).body(registrada);
    }

    @GetMapping("/representante/{idUsuario}")
    public ResponseEntity<?> obtenerDeRepresentante(@PathVariable Long idUsuario) {
        Optional<Long> idToken = UsuarioAutenticado.idActual();
        if (idToken.isPresent() && !idToken.get().equals(idUsuario) && !UsuarioAutenticado.esAdmin()) {
            return prohibido("Solo puedes consultar tu propia organización");
        }

        Optional<Organizacion> organizacion = organizacionService.obtenerDeRepresentante(idUsuario);
        return organizacion.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ExceptionHandler(OrganizacionException.class)
    public ResponseEntity<ApiError> manejarError(OrganizacionException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiError(ex.getStatus().value(), ex.getMessage(), ex.getErrores()));
    }

    private ResponseEntity<ApiError> prohibido(String mensaje) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(HttpStatus.FORBIDDEN.value(), mensaje, Map.of("idRepresentante", mensaje)));
    }
}
