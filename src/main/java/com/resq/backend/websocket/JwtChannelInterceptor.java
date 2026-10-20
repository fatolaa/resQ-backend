package com.resq.backend.websocket;

import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

/**
 * HU-38a: autentica el handshake STOMP con el JWT y autoriza cada suscripcion.
 *
 * El token ya no viaja por header HTTP (el upgrade solo abre el socket), asi que se
 * lee de la cabecera nativa Authorization del frame CONNECT. Las reglas de sala
 * evitan que un usuario reciba eventos personales de otro o de un caso en el que no
 * participa.
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final String CABECERA_AUTORIZACION = "Authorization";
    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final ParticipanteCasoService participanteCasoService;

    public JwtChannelInterceptor(JwtService jwtService,
                                 UsuarioRepository usuarioRepository,
                                 ParticipanteCasoService participanteCasoService) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.participanteCasoService = participanteCasoService;
    }

    @Override
    public Message<?> preSend(Message<?> mensaje, MessageChannel canal) {
        StompHeaderAccessor acceso = MessageHeaderAccessor.getAccessor(mensaje, StompHeaderAccessor.class);
        if (acceso == null) {
            return mensaje;
        }
        if (StompCommand.CONNECT.equals(acceso.getCommand())) {
            acceso.setUser(autenticar(acceso));
        } else if (StompCommand.SUBSCRIBE.equals(acceso.getCommand())) {
            autorizarSuscripcion(acceso);
        }
        return mensaje;
    }

    private StompPrincipal autenticar(StompHeaderAccessor acceso) {
        String cabecera = primeraCabecera(acceso, CABECERA_AUTORIZACION);
        if (cabecera == null || !cabecera.startsWith(PREFIJO_BEARER)) {
            throw new AccessDeniedException("Falta el token en el handshake del WebSocket.");
        }
        Long idUsuario = jwtService.extraerIdUsuarioSiEsValido(cabecera.substring(PREFIJO_BEARER.length()));
        if (idUsuario == null) {
            throw new AccessDeniedException("Token invalido o vencido.");
        }
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new AccessDeniedException("Usuario inexistente."));
        return new StompPrincipal(usuario.getIdUsuario(), usuario.getEmail(), usuario.getRol());
    }

    private void autorizarSuscripcion(StompHeaderAccessor acceso) {
        Long idUsuario = idPrincipal(acceso);
        if (idUsuario == null) {
            throw new AccessDeniedException("Sesion no autenticada.");
        }
        Long idUsuarioDeLaSala = SalaWebSocket.idUsuarioDe(acceso.getDestination());
        if (idUsuarioDeLaSala != null) {
            if (!idUsuario.equals(idUsuarioDeLaSala)) {
                throw new AccessDeniedException("No podes suscribirte a la sala de otro usuario.");
            }
            return;
        }
        Long idCaso = SalaWebSocket.idCasoDe(acceso.getDestination());
        if (idCaso != null && !participanteCasoService.participa(idUsuario, idCaso)) {
            throw new AccessDeniedException("No participas del caso " + idCaso + ".");
        }
    }

    private Long idPrincipal(StompHeaderAccessor acceso) {
        Principal principal = acceso.getUser();
        if (principal instanceof StompPrincipal stompPrincipal) {
            return stompPrincipal.getIdUsuario();
        }
        return null;
    }

    private String primeraCabecera(StompHeaderAccessor acceso, String nombre) {
        List<String> valores = acceso.getNativeHeader(nombre);
        return (valores == null || valores.isEmpty()) ? null : valores.get(0);
    }
}
