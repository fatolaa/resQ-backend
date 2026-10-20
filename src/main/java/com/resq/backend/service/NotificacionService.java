package com.resq.backend.service;

import com.resq.backend.entity.Notificacion;
import com.resq.backend.repository.NotificacionRepository;
import com.resq.backend.websocket.EventoWebSocket;
import com.resq.backend.websocket.EventoWebSocketService;
import com.resq.backend.websocket.TipoEventoWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotificacionService {

    private static final Logger logger = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificacionRepository;
    private final EventoWebSocketService eventoWebSocketService;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               EventoWebSocketService eventoWebSocketService) {
        this.notificacionRepository = notificacionRepository;
        this.eventoWebSocketService = eventoWebSocketService;
    }

    public Notificacion crearNotificacion(Long idUsuario, String asunto, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setIdUsuario(idUsuario);
        notificacion.setTipo("SISTEMA");
        notificacion.setMensaje(mensaje != null ? mensaje : asunto);
        notificacion.setLeida(false);
        if (notificacion.getFechaCreacion() == null) {
            notificacion.setFechaCreacion(LocalDateTime.now());
        }
        Notificacion guardada = notificacionRepository.save(notificacion);
        logger.info("Notificación creada para usuario ID: {}", idUsuario);

        // HU-38a: si el usuario tiene una sesion WebSocket abierta, la notificacion
        // llega en vivo a su sala personal.
        eventoWebSocketService.aUsuario(idUsuario,
                EventoWebSocket.de(TipoEventoWebSocket.NOTIFICACION_NUEVA, datosDe(guardada)));
        return guardada;
    }

    private Map<String, Object> datosDe(Notificacion notificacion) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("idNotificacion", notificacion.getIdNotificacion());
        datos.put("idUsuario", notificacion.getIdUsuario());
        datos.put("idReporte", notificacion.getIdReporte());
        datos.put("tipo", notificacion.getTipo());
        datos.put("mensaje", notificacion.getMensaje());
        datos.put("leida", notificacion.isLeida());
        datos.put("fechaCreacion", notificacion.getFechaCreacion());
        return datos;
    }
}
