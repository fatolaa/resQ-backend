package com.resq.backend.controller;

import com.resq.backend.dto.ContactoReporteDTO;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.service.ContactoReporteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Contacto de quien reporto un caso: nombre, insignia y telefonos.
 *
 * Exige sesion (regla en SecurityConfig, antes del permitAll de GET /api/reportes/**).
 * Responde 200 con el contacto, 204 si quien reporto no lo compartio o el caso ya
 * esta cerrado, y 404 si el caso no existe.
 */
@RestController
@RequestMapping("/api/reportes")
public class ContactoReporteController {

    private final ContactoReporteService contactoReporteService;

    public ContactoReporteController(ContactoReporteService contactoReporteService) {
        this.contactoReporteService = contactoReporteService;
    }

    @GetMapping("/{idReporte}/contacto")
    public ResponseEntity<ContactoReporteDTO> contacto(@PathVariable Long idReporte) {
        Long idUsuario = UsuarioAutenticado.idActual()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para ver el contacto"));
        return contactoReporteService.contactoDe(idReporte, idUsuario)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
