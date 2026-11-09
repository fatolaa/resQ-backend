package com.resq.backend.controller;

import com.resq.backend.dto.EnviarMensajeCasoDTO;
import com.resq.backend.dto.MensajeCasoDTO;
import com.resq.backend.dto.ParticipanteChatDTO;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.service.ChatCasoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * HU-22: chat de un caso entre sus participantes. Requiere sesion; el servicio verifica que
 * el usuario participe del caso (403 si no) y que el caso exista (404).
 *
 * - GET  /api/casos/{idCaso}/chat/mensajes        historial
 * - POST /api/casos/{idCaso}/chat/mensajes        texto o ubicacion (JSON)
 * - POST /api/casos/{idCaso}/chat/fotos           foto (multipart: foto + comentario opcional)
 * - GET  /api/casos/{idCaso}/chat/participantes   participantes y presencia
 *
 * Los mensajes nuevos llegan en vivo por WebSocket (chat.message en /topic/casos/{idCaso}).
 */
@RestController
@RequestMapping("/api/casos/{idCaso}/chat")
public class ChatCasoController {

    private final ChatCasoService chatCasoService;

    public ChatCasoController(ChatCasoService chatCasoService) {
        this.chatCasoService = chatCasoService;
    }

    @GetMapping("/mensajes")
    public ResponseEntity<List<MensajeCasoDTO>> historial(@PathVariable Long idCaso) {
        return ResponseEntity.ok(chatCasoService.historial(idCaso, idUsuario()));
    }

    @PostMapping("/mensajes")
    public ResponseEntity<MensajeCasoDTO> enviar(@PathVariable Long idCaso,
                                                 @Valid @RequestBody EnviarMensajeCasoDTO mensaje) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatCasoService.enviar(idCaso, idUsuario(), mensaje));
    }

    @PostMapping(value = "/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MensajeCasoDTO> enviarFoto(@PathVariable Long idCaso,
                                                     @RequestParam("foto") MultipartFile foto,
                                                     @RequestParam(name = "comentario", required = false) String comentario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatCasoService.enviarFoto(idCaso, idUsuario(), foto, comentario));
    }

    @GetMapping("/participantes")
    public ResponseEntity<List<ParticipanteChatDTO>> participantes(@PathVariable Long idCaso) {
        return ResponseEntity.ok(chatCasoService.participantes(idCaso, idUsuario()));
    }

    /** SecurityConfig ya exige sesion (anyRequest().authenticated()), asi que el id siempre esta. */
    private static Long idUsuario() {
        return UsuarioAutenticado.idActual()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión"));
    }
}
