package com.resq.backend.config;

import com.resq.backend.websocket.JwtChannelInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * HU-38a: habilita el servidor STOMP sobre WebSocket. El broker es simple (en memoria)
 * porque el despliegue es de una sola instancia; si se escala horizontalmente habria
 * que cambiarlo por un relay (RabbitMQ/ActiveMQ).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;
    private final String origenesPermitidos;

    public WebSocketConfig(JwtChannelInterceptor jwtChannelInterceptor,
                           @Value("${resq.ws.allowed-origins:*}") String origenesPermitidos) {
        this.jwtChannelInterceptor = jwtChannelInterceptor;
        this.origenesPermitidos = origenesPermitidos;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket nativo (sin SockJS): el cliente usa @stomp/stompjs.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origenesPermitidos.split(","));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Salas que el servidor difunde. /app: mensajes que entran a los @MessageMapping.
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
    }
}
