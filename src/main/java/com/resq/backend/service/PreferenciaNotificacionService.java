package com.resq.backend.service;

import com.resq.backend.dto.PreferenciaNotificacionRequestDTO;
import com.resq.backend.entity.PreferenciaNotificacion;
import com.resq.backend.repository.PreferenciaNotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenciaNotificacionService {

    private static final Logger logger = LoggerFactory.getLogger(PreferenciaNotificacionService.class);

    private final PreferenciaNotificacionRepository preferenciaRepository;

    public PreferenciaNotificacionService(PreferenciaNotificacionRepository preferenciaRepository) {
        this.preferenciaRepository = preferenciaRepository;
    }

    /**
     * Devuelve las preferencias del usuario. Si todavia no tiene fila (usuario
     * nuevo), crea una con los valores por defecto y la persiste, para que las
     * siguientes consultas ya la encuentren.
     */
    @Transactional
    public PreferenciaNotificacion obtenerOCrear(Long idUsuario) {
        return preferenciaRepository.findByIdUsuario(idUsuario)
                .orElseGet(() -> crearConValoresPorDefecto(idUsuario));
    }

    private PreferenciaNotificacion crearConValoresPorDefecto(Long idUsuario) {
        PreferenciaNotificacion preferencia = new PreferenciaNotificacion(idUsuario);
        PreferenciaNotificacion guardada = preferenciaRepository.save(preferencia);
        logger.info("Preferencias de notificacion creadas por defecto para el usuario ID: {}", idUsuario);
        return guardada;
    }

    /**
     * Actualiza solo los campos que vienen con valor (los null se dejan como
     * estaban). El ownership ya lo garantiza el controlador al usar el id del
     * token: aqui nunca se recibe un id de otro usuario.
     */
    @Transactional
    public PreferenciaNotificacion actualizar(Long idUsuario, PreferenciaNotificacionRequestDTO cambios) {
        PreferenciaNotificacion preferencia = obtenerOCrear(idUsuario);

        if (cambios.notificarSolicitudAprobada() != null) {
            preferencia.setNotificarSolicitudAprobada(cambios.notificarSolicitudAprobada());
        }
        if (cambios.notificarSolicitudRechazada() != null) {
            preferencia.setNotificarSolicitudRechazada(cambios.notificarSolicitudRechazada());
        }
        if (cambios.notificarReporteCerca() != null) {
            preferencia.setNotificarReporteCerca(cambios.notificarReporteCerca());
        }
        if (cambios.notificarChatMensaje() != null) {
            preferencia.setNotificarChatMensaje(cambios.notificarChatMensaje());
        }

        PreferenciaNotificacion guardada = preferenciaRepository.save(preferencia);
        logger.info("Preferencias de notificacion actualizadas para el usuario ID: {}", idUsuario);
        return guardada;
    }
}
