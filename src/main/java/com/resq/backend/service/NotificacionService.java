package com.resq.backend.service;

import com.resq.backend.entity.Notificacion;
import com.resq.backend.exception.NotificacionException;
import com.resq.backend.repository.NotificacionRepository;
import com.resq.backend.websocket.EventoWebSocket;
import com.resq.backend.websocket.EventoWebSocketService;
import com.resq.backend.websocket.TipoEventoWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        return crearNotificacion(idUsuario, null, null, Notificacion.SISTEMA,
                mensaje != null ? mensaje : asunto);
    }

    /** HU-37: crea el aviso guardando el origen para poder navegar a el desde la bandeja. */
    public Notificacion crearNotificacion(Long idUsuario, Long idReporte, Long idSolicitud,
                                           String tipo, String mensaje) {
        Notificacion notificacion = new Notificacion(idUsuario, idReporte, idSolicitud, tipo, mensaje);
        notificacion.setLeida(false);
        notificacion.setArchivada(false);
        if (notificacion.getFechaCreacion() == null) {
            notificacion.setFechaCreacion(LocalDateTime.now());
        }
        Notificacion guardada = notificacionRepository.save(notificacion);
        logger.info("Notificacion creada para usuario ID: {}", idUsuario);

        // HU-38a: si el usuario tiene una sesion WebSocket abierta, la notificacion
        // llega en vivo a su sala personal.
        eventoWebSocketService.aUsuario(idUsuario,
                EventoWebSocket.de(TipoEventoWebSocket.NOTIFICACION_NUEVA, datosDe(guardada)));
        return guardada;
    }

    /** HU-37: bandeja ordenada de la mas reciente a la mas antigua, sin las archivadas. */
    public List<Notificacion> listar(Long idUsuario) {
        return notificacionRepository
                .findByIdUsuarioAndArchivadaFalseOrderByFechaCreacionDesc(idUsuario);
    }

    /** HU-37: marca una notificacion propia como leida o no leida. */
    public Notificacion marcarLeida(Long idNotificacion, Long idUsuario, boolean leida) {
        Notificacion notificacion = obtenerPropia(idNotificacion, idUsuario);
        notificacion.setLeida(leida);
        return notificacionRepository.save(notificacion);
    }

    /** HU-37: archiva una notificacion propia y la saca de la bandeja. */
    public Notificacion archivar(Long idNotificacion, Long idUsuario) {
        Notificacion notificacion = obtenerPropia(idNotificacion, idUsuario);
        notificacion.setArchivada(true);
        notificacion.setLeida(true);
        return notificacionRepository.save(notificacion);
    }

    private Notificacion obtenerPropia(Long idNotificacion, Long idUsuario) {
        Notificacion notificacion = notificacionRepository.findById(idNotificacion)
                .orElseThrow(() -> new NotificacionException(HttpStatus.NOT_FOUND,
                        "Notificacion no encontrada", Map.of()));
        if (!Objects.equals(notificacion.getIdUsuario(), idUsuario)) {
            throw new NotificacionException(HttpStatus.FORBIDDEN,
                    "No puedes gestionar las notificaciones de otro usuario", Map.of());
        }
        return notificacion;
    }

    private Map<String, Object> datosDe(Notificacion notificacion) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("idNotificacion", notificacion.getIdNotificacion());
        datos.put("idUsuario", notificacion.getIdUsuario());
        datos.put("idReporte", notificacion.getIdReporte());
        datos.put("idSolicitud", notificacion.getIdSolicitud());
        datos.put("tipo", notificacion.getTipo());
        datos.put("mensaje", notificacion.getMensaje());
        datos.put("leida", notificacion.isLeida());
        datos.put("archivada", notificacion.isArchivada());
        datos.put("fechaCreacion", notificacion.getFechaCreacion());
        return datos;
    }
}
