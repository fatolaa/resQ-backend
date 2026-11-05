package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.ConversacionDTO;
import com.resq.backend.dto.EnviarMensajeRequestDTO;
import com.resq.backend.entity.Mensaje;
import com.resq.backend.exception.ChatException;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // GET /api/chat/conversaciones?busqueda=texto
    @GetMapping("/conversaciones")
    public ResponseEntity<?> listar(@RequestParam(required = false) String busqueda) {
        Optional<Long> idUsuario = UsuarioAutenticado.idActual();
        if (idUsuario.isEmpty()) {
            return sinSesion();
        }
        return ResponseEntity.ok(chatService.obtenerConversaciones(idUsuario.get(), busqueda));
    }

    // POST /api/chat/mensajes (HU-40: Enviar mensaje validando bloqueos)
    @PostMapping("/mensajes")
    public ResponseEntity<?> enviarMensaje(@Valid @RequestBody EnviarMensajeRequestDTO dto) {
        Optional<Long> idUsuario = UsuarioAutenticado.idActual();
        if (idUsuario.isEmpty()) {
            return sinSesion();
        }
        Mensaje enviado = chatService.enviarMensaje(idUsuario.get(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(enviado);
    }

    // PATCH /api/chat/conversaciones/{idOtroUsuario}/archivar
    @PatchMapping("/conversaciones/{idOtroUsuario}/archivar")
    public ResponseEntity<?> archivar(@PathVariable Long idOtroUsuario, @RequestParam boolean archivar) {
        Optional<Long> idUsuario = UsuarioAutenticado.idActual();
        if (idUsuario.isEmpty()) {
            return sinSesion();
        }
        chatService.archivarConversacion(idUsuario.get(), idOtroUsuario, archivar);
        return ResponseEntity.noContent().build();
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
}
