package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.DenunciaRequestDTO;
import com.resq.backend.dto.ResolucionDenunciaDTO;
import com.resq.backend.entity.DenunciaMensaje;
import com.resq.backend.entity.EstadoDenuncia;
import com.resq.backend.exception.ChatException;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.service.DenunciaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat/denuncias")
public class DenunciaController {

    private static final Logger logger = LoggerFactory.getLogger(DenunciaController.class);

    private final DenunciaService denunciaService;

    public DenunciaController(DenunciaService denunciaService) {
        this.denunciaService = denunciaService;
    }

    @PostMapping
    public ResponseEntity<?> denunciar(@Valid @RequestBody DenunciaRequestDTO dto) {
        Optional<Long> idToken = UsuarioAutenticado.idActual();
        if (idToken.isEmpty()) {
            return sinSesion();
        }
        logger.info("Usuario ID {} enviando denuncia para mensaje ID {}", idToken.get(), dto.idMensaje());
        DenunciaMensaje guardada = denunciaService.denunciar(idToken.get(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String estado) {
        if (!UsuarioAutenticado.esAdmin()) {
            logger.warn("Usuario no admin intento listar denuncias");
            return sinPermisoAdmin();
        }

        EstadoDenuncia filtroEstado = null;
        if (estado != null && !estado.isBlank()) {
            try {
                filtroEstado = EstadoDenuncia.valueOf(estado.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(HttpStatus.BAD_REQUEST.value(),
                                "Estado de denuncia invalido: " + estado + ". Permitidos: PENDIENTE, REVISADA, DESCARTADA",
                                null));
            }
        }

        logger.info("Admin listando denuncias. Filtro estado: {}", filtroEstado);
        List<DenunciaMensaje> lista = denunciaService.listarDenuncias(filtroEstado);
        return ResponseEntity.ok(lista);
    }

    @PatchMapping("/{id}/resolver")
    public ResponseEntity<?> resolver(@PathVariable Long id, @Valid @RequestBody ResolucionDenunciaDTO dto) {
        if (!UsuarioAutenticado.esAdmin()) {
            logger.warn("Usuario no admin intento resolver denuncia ID {}", id);
            return sinPermisoAdmin();
        }

        Optional<Long> idAdmin = UsuarioAutenticado.idActual();
        if (idAdmin.isEmpty()) {
            return sinSesion();
        }

        logger.info("Admin ID {} resolviendo denuncia ID {}", idAdmin.get(), id);
        DenunciaMensaje resuelta = denunciaService.resolver(id, idAdmin.get(), dto);
        return ResponseEntity.ok(resuelta);
    }

    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ApiError> manejarError(ChatException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiError(ex.getStatus().value(), ex.getMessage(), ex.getErrores()));
    }

    private ResponseEntity<ApiError> sinSesion() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(HttpStatus.UNAUTHORIZED.value(),
                        "Debes iniciar sesion para realizar esta accion", null));
    }

    private ResponseEntity<ApiError> sinPermisoAdmin() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(HttpStatus.FORBIDDEN.value(),
                        "Solo un administrador puede consultar o resolver denuncias", null));
    }
}
