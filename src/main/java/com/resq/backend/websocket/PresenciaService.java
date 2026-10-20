package com.resq.backend.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HU-38a: mantiene el estado "en linea" por usuario (contando sesiones, porque un
 * usuario puede tener varias pestanas) y emite user.online a la sala personal y a los
 * casos en los que participa.
 */
@Component
public class PresenciaService {

    private static final Logger logger = LoggerFactory.getLogger(PresenciaService.class);

    private final EventoWebSocketService eventos;
    private final ParticipanteCasoService participanteCasoService;
    private final Map<Long, Integer> sesionesPorUsuario = new ConcurrentHashMap<>();

    public PresenciaService(EventoWebSocketService eventos,
                            ParticipanteCasoService participanteCasoService) {
        this.eventos = eventos;
        this.participanteCasoService = participanteCasoService;
    }

    @EventListener
    public void alConectar(SessionConnectedEvent evento) {
        StompPrincipal principal = principalDe(evento.getUser());
        if (principal == null) {
            return;
        }
        int sesiones = sesionesPorUsuario.merge(principal.getIdUsuario(), 1, Integer::sum);
        if (sesiones == 1) {
            logger.info("Usuario ID {} en linea", principal.getIdUsuario());
            publicarPresencia(principal.getIdUsuario(), true);
        }
    }

    @EventListener
    public void alDesconectar(SessionDisconnectEvent evento) {
        StompPrincipal principal = principalDe(evento.getUser());
        if (principal == null) {
            return;
        }
        Integer sesiones = sesionesPorUsuario.computeIfPresent(principal.getIdUsuario(),
                (id, actual) -> actual > 1 ? actual - 1 : null);
        if (sesiones == null) {
            logger.info("Usuario ID {} fuera de linea", principal.getIdUsuario());
            publicarPresencia(principal.getIdUsuario(), false);
        }
    }

    private void publicarPresencia(Long idUsuario, boolean enLinea) {
        EventoWebSocket evento = EventoWebSocket.de(TipoEventoWebSocket.USUARIO_EN_LINEA,
                Map.of("idUsuario", idUsuario, "enLinea", enLinea));
        eventos.aUsuario(idUsuario, evento);
        for (Long idCaso : participanteCasoService.casosDe(idUsuario)) {
            eventos.aCaso(idCaso, evento);
        }
    }

    private StompPrincipal principalDe(Principal principal) {
        return principal instanceof StompPrincipal stompPrincipal ? stompPrincipal : null;
    }
}
