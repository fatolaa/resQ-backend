package com.resq.backend.controller;

import com.resq.backend.dto.ConversacionDTO;
import com.resq.backend.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // GET /api/chat/conversaciones?busqueda=texto
    @GetMapping("/conversaciones")
    public ResponseEntity<List<ConversacionDTO>> listar(@RequestParam(required = false) String busqueda) {
        Long idUsuario = idUsuarioAutenticado();
        return ResponseEntity.ok(chatService.obtenerConversaciones(idUsuario, busqueda));
    }

    // PATCH /api/chat/conversaciones/{idOtroUsuario}/archivar
    @PatchMapping("/conversaciones/{idOtroUsuario}/archivar")
    public ResponseEntity<Void> archivar(@PathVariable Long idOtroUsuario, @RequestParam boolean archivar) {
        Long idUsuario = idUsuarioAutenticado();
        chatService.archivarConversacion(idUsuario, idOtroUsuario, archivar);
        return ResponseEntity.noContent().build();
    }

    // Método auxiliar para obtener el ID del token
    private Long idUsuarioAutenticado() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        try {
            return Long.parseLong(String.valueOf(auth.getPrincipal()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}