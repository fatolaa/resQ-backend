package com.resq.backend.websocket;

import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtChannelInterceptor: auth del handshake y autorizacion de suscripciones (HU-38a)")
class JwtChannelInterceptorTest {

    private static final String SECRETO = "resq-clave-de-prueba-suficientemente-larga-para-hmac-256";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ParticipanteCasoService participanteCasoService;

    private JwtService jwtService;
    private JwtChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRETO, 60_000L);
        interceptor = new JwtChannelInterceptor(jwtService, usuarioRepository, participanteCasoService);
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setEmail("u" + id + "@resq.co");
        usuario.setRol("USUARIO");
        return usuario;
    }

    private Message<byte[]> frame(StompCommand comando, Consumer<StompHeaderAccessor> configurar) {
        StompHeaderAccessor acceso = StompHeaderAccessor.create(comando);
        acceso.setLeaveMutable(true);
        configurar.accept(acceso);
        return MessageBuilder.createMessage(new byte[0], acceso.getMessageHeaders());
    }

    @Test
    @DisplayName("un CONNECT con token valido queda autenticado con la identidad del usuario")
    void connectConTokenValidoAutentica() {
        String token = jwtService.generarToken(usuario(7L));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario(7L)));

        Message<?> resultado = interceptor.preSend(
                frame(StompCommand.CONNECT, a -> a.setNativeHeader("Authorization", "Bearer " + token)), null);

        StompHeaderAccessor salida = MessageHeaderAccessor.getAccessor(resultado, StompHeaderAccessor.class);
        assertThat(salida.getUser()).isInstanceOf(StompPrincipal.class);
        assertThat(salida.getUser().getName()).isEqualTo("7");
    }

    @Test
    @DisplayName("un CONNECT sin token se rechaza")
    void connectSinTokenSeRechaza() {
        Message<byte[]> sinToken = frame(StompCommand.CONNECT, a -> { });

        assertThatThrownBy(() -> interceptor.preSend(sinToken, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("un CONNECT con token invalido se rechaza")
    void connectConTokenInvalidoSeRechaza() {
        Message<byte[]> conTokenMalo = frame(StompCommand.CONNECT,
                a -> a.setNativeHeader("Authorization", "Bearer no-es-un-token"));

        assertThatThrownBy(() -> interceptor.preSend(conTokenMalo, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("solo el propio usuario puede suscribirse a su sala personal")
    void soloLaPropiaSalaPersonal() {
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, a -> {
            a.setUser(new StompPrincipal(7L, "u7@resq.co", "USUARIO"));
            a.setDestination("/topic/usuarios/7");
        }), null);

        Message<byte[]> salaAjena = frame(StompCommand.SUBSCRIBE, a -> {
            a.setUser(new StompPrincipal(7L, "u7@resq.co", "USUARIO"));
            a.setDestination("/topic/usuarios/9");
        });

        assertThatThrownBy(() -> interceptor.preSend(salaAjena, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("una sala de caso solo se permite a quien participa del caso")
    void salaDeCasoSoloAParticipantes() {
        when(participanteCasoService.participa(7L, 101L)).thenReturn(true);
        when(participanteCasoService.participa(7L, 202L)).thenReturn(false);

        interceptor.preSend(frame(StompCommand.SUBSCRIBE, a -> {
            a.setUser(new StompPrincipal(7L, "u7@resq.co", "USUARIO"));
            a.setDestination("/topic/casos/101");
        }), null);

        Message<byte[]> casoAjeno = frame(StompCommand.SUBSCRIBE, a -> {
            a.setUser(new StompPrincipal(7L, "u7@resq.co", "USUARIO"));
            a.setDestination("/topic/casos/202");
        });

        assertThatThrownBy(() -> interceptor.preSend(casoAjeno, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("una suscripcion sin usuario autenticado se rechaza")
    void suscripcionSinUsuarioSeRechaza() {
        Message<byte[]> sinUsuario = frame(StompCommand.SUBSCRIBE,
                a -> a.setDestination("/topic/usuarios/7"));

        assertThatThrownBy(() -> interceptor.preSend(sinUsuario, null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
