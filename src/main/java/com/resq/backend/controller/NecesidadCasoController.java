package com.resq.backend.controller;

import com.resq.backend.dto.AgregarNecesidadDTO;
import com.resq.backend.dto.NecesidadCasoDTO;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.service.NecesidadCasoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Rediseño: "Que necesita" un caso.
 *
 * GET es publico (como el resto de GET /api/reportes/**); lo demas exige sesion
 * (anyRequest().authenticated() en SecurityConfig). Cada accion responde con la lista
 * actualizada de necesidades del caso.
 */
@RestController
@RequestMapping("/api/reportes/{idReporte}/necesidades")
public class NecesidadCasoController {

    private final NecesidadCasoService necesidadService;

    public NecesidadCasoController(NecesidadCasoService necesidadService) {
        this.necesidadService = necesidadService;
    }

    @GetMapping
    public ResponseEntity<List<NecesidadCasoDTO>> listar(@PathVariable Long idReporte) {
        return ResponseEntity.ok(necesidadService.listar(idReporte));
    }

    @PostMapping
    public ResponseEntity<List<NecesidadCasoDTO>> agregar(@PathVariable Long idReporte,
                                                          @Valid @RequestBody AgregarNecesidadDTO datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(necesidadService.agregar(idReporte, datos.tipo(), usuario()));
    }

    @DeleteMapping("/{idNecesidad}")
    public ResponseEntity<List<NecesidadCasoDTO>> quitar(@PathVariable Long idReporte, @PathVariable Long idNecesidad) {
        return ResponseEntity.ok(necesidadService.quitar(idReporte, idNecesidad, usuario()));
    }

    @PostMapping("/{idNecesidad}/ofrecer")
    public ResponseEntity<List<NecesidadCasoDTO>> ofrecer(@PathVariable Long idReporte, @PathVariable Long idNecesidad) {
        return ResponseEntity.ok(necesidadService.ofrecer(idReporte, idNecesidad, usuario()));
    }

    @PostMapping("/{idNecesidad}/confirmar")
    public ResponseEntity<List<NecesidadCasoDTO>> confirmar(@PathVariable Long idReporte, @PathVariable Long idNecesidad) {
        return ResponseEntity.ok(necesidadService.confirmar(idReporte, idNecesidad, usuario()));
    }

    @PostMapping("/{idNecesidad}/liberar")
    public ResponseEntity<List<NecesidadCasoDTO>> liberar(@PathVariable Long idReporte, @PathVariable Long idNecesidad) {
        return ResponseEntity.ok(necesidadService.liberar(idReporte, idNecesidad, usuario()));
    }

    private static Long usuario() {
        return UsuarioAutenticado.idActual()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar"));
    }
}
