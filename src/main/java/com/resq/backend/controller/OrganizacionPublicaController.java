package com.resq.backend.controller;

import com.resq.backend.dto.OrganizacionPublicaDTO;
import com.resq.backend.service.OrganizacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rediseño: organizaciones verificadas para quien no inicio sesion.
 *
 * - GET /api/organizaciones/publicas: la red que se muestra en el Inicio.
 * - GET /api/organizaciones/publicas/{id}: "Caso gestionado por" en el detalle.
 *
 * Solo datos publicos (OrganizacionPublicaDTO). El directorio completo y la ficha
 * con contacto siguen exigiendo sesion (HU-29).
 */
@RestController
@RequestMapping("/api/organizaciones/publicas")
public class OrganizacionPublicaController {

    private final OrganizacionService organizacionService;

    public OrganizacionPublicaController(OrganizacionService organizacionService) {
        this.organizacionService = organizacionService;
    }

    @GetMapping
    public ResponseEntity<List<OrganizacionPublicaDTO>> listar() {
        return ResponseEntity.ok(organizacionService.consultarDirectorio(null, null).stream()
                .map(OrganizacionPublicaDTO::de)
                .toList());
    }

    @GetMapping("/{idOrganizacion}")
    public ResponseEntity<OrganizacionPublicaDTO> obtener(@PathVariable Long idOrganizacion) {
        return organizacionService.obtenerFicha(idOrganizacion)
                .map(OrganizacionPublicaDTO::de)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
