package com.resq.backend.websocket;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * HU-38a: mensajes que el cliente envia al servidor. El envio de un mensaje de chat
 * en si es de HU-22 (#41); aca solo van los eventos que el transporte ya soporta:
 * el aviso de "escribiendo" y la peticion de replay al reconectar.
 */
@Controller
public class ChatWebSocketController {

    private final EventoWebSocketService eventos;
    private final ParticipanteCasoService participanteCasoService;

    public ChatWebSocketController(EventoWebSocketService eventos,
                                   ParticipanteCasoService participanteCasoService) {
        this.eventos = eventos;
        this.participanteCasoService = participanteCasoService;
    }

    /** El cliente avisa que esta escribiendo en un caso; se difunde a sus participantes. */
    @MessageMapping("/caso/{idCaso}/escribiendo")
    public void escribiendo(@DestinationVariable Long idCaso, Principal principal) {
        Long idUsuario = idDe(principal);
        if (idUsuario == null || !participanteCasoService.participa(idUsuario, idCaso)) {
            return;
        }
        eventos.aCaso(idCaso, EventoWebSocket.de(TipoEventoWebSocket.CHAT_ESCRIBIENDO,
                Map.of("idCaso", idCaso, "idUsuario", idUsuario)));
    }

    /** Tras (re)conectar, el cliente pide los eventos personales que se perdio. */
    @MessageMapping("/replay")
    public void replay(@Payload PeticionReplay peticion, Principal principal) {
        Long idUsuario = idDe(principal);
        if (idUsuario == null) {
            return;
        }
        long ultimoId = peticion != null && peticion.ultimoId() != null ? peticion.ultimoId() : 0L;
        eventos.reenviarPendientes(idUsuario, ultimoId);
    }

    private Long idDe(Principal principal) {
        return principal instanceof StompPrincipal stompPrincipal ? stompPrincipal.getIdUsuario() : null;
    }

    public record PeticionReplay(Long ultimoId) {
    }
}
