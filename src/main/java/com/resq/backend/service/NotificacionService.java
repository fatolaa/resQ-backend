package com.resq.backend.service;

import com.resq.backend.entity.Notificacion;
import com.resq.backend.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificacionService {

    private static final Logger logger = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    public void crearNotificacion(Long idUsuario, String asunto, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setIdUsuario(idUsuario);
        notificacion.setTipo("SISTEMA");
        notificacion.setMensaje(mensaje != null ? mensaje : asunto);
        notificacion.setLeida(false);
        if (notificacion.getFechaCreacion() == null) {
            notificacion.setFechaCreacion(LocalDateTime.now());
        }
        notificacionRepository.save(notificacion);
        logger.info("Notificación creada para usuario ID: {}", idUsuario);
    }
}