package com.resq.backend.service;

import com.resq.backend.dto.RechazarSolicitudRequestDTO;
import com.resq.backend.dto.SolicitudVoluntarioDetalleDTO;
import com.resq.backend.dto.SolicitudVoluntarioResumenDTO;
import com.resq.backend.entity.EstadoSolicitudVoluntario;
import com.resq.backend.entity.SolicitudVoluntario;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.SolicitudVoluntarioRepository;
import com.resq.backend.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolicitudVoluntarioService {

    private static final Logger logger = LoggerFactory.getLogger(SolicitudVoluntarioService.class);
    private static final int DIAS_BLOQUEO_RECHAZO = 30;

    @Autowired
    private SolicitudVoluntarioRepository solicitudRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private EmailService emailService;

    public List<SolicitudVoluntarioResumenDTO> listarPendientes() {
        logger.info("Listando solicitudes de voluntario pendientes");
        return solicitudRepository.findByEstadoOrderByFechaSolicitudAsc(EstadoSolicitudVoluntario.PENDIENTE)
                .stream()
                .map(this::toResumen)
                .collect(Collectors.toList());
    }

    public SolicitudVoluntarioDetalleDTO obtenerDetalle(Long idSolicitud) {
        logger.info("Obteniendo detalle de solicitud de voluntario ID: {}", idSolicitud);
        SolicitudVoluntario solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud de voluntario no encontrada"));
        return toDetalle(solicitud);
    }

    @Transactional
    public SolicitudVoluntarioDetalleDTO aprobarSolicitud(Long idSolicitud, Long idAdministrador) {
        logger.info("Aprobando solicitud de voluntario ID: {} por admin ID: {}", idSolicitud, idAdministrador);
        SolicitudVoluntario solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud de voluntario no encontrada"));

        if (solicitud.getEstado() != EstadoSolicitudVoluntario.PENDIENTE) {
            throw new RuntimeException("La solicitud no est?? en estado PENDIENTE");
        }

        Usuario admin = usuarioRepository.findById(idAdministrador)
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));

        Usuario solicitante = solicitud.getUsuario();
        solicitante.setRol("VOLUNTARIO");
        usuarioRepository.save(solicitante);

        solicitud.setEstado(EstadoSolicitudVoluntario.APROBADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setRevisadoPor(admin);
        solicitud.setMotivoRechazo(null);
        solicitud.setNotificada(false);

        SolicitudVoluntario guardada = solicitudRepository.save(solicitud);
        notificarResolucion(guardada);

        return toDetalle(guardada);
    }

    @Transactional
    public SolicitudVoluntarioDetalleDTO rechazarSolicitud(Long idSolicitud, Long idAdministrador, RechazarSolicitudRequestDTO request) {
        logger.info("Rechazando solicitud de voluntario ID: {} por admin ID: {}", idSolicitud, idAdministrador);
        SolicitudVoluntario solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud de voluntario no encontrada"));

        if (solicitud.getEstado() != EstadoSolicitudVoluntario.PENDIENTE) {
            throw new RuntimeException("La solicitud no est?? en estado PENDIENTE");
        }

        Usuario admin = usuarioRepository.findById(idAdministrador)
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));

        solicitud.setEstado(EstadoSolicitudVoluntario.RECHAZADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setRevisadoPor(admin);
        solicitud.setMotivoRechazo(request.motivo());
        solicitud.setNotificada(false);

        SolicitudVoluntario guardada = solicitudRepository.save(solicitud);
        notificarResolucion(guardada);

        return toDetalle(guardada);
    }

    @Transactional
    public SolicitudVoluntarioDetalleDTO crearSolicitud(Usuario usuario, String tipoAyuda, String mensaje) {
        logger.info("Creando solicitud de voluntario del usuario ID: {}", usuario.getIdUsuario());
        if ("VOLUNTARIO".equals(usuario.getRol())) {
            throw new IllegalStateException("El usuario ya es voluntario");
        }
        if (solicitudRepository.findTopByUsuarioAndEstadoOrderByFechaSolicitudDesc(usuario, EstadoSolicitudVoluntario.PENDIENTE).isPresent()) {
            throw new IllegalStateException("Ya tienes una solicitud pendiente");
        }
        if (!puedeSolicitarVoluntario(usuario)) {
            throw new IllegalStateException("Debes esperar " + DIAS_BLOQUEO_RECHAZO + " dias desde el ultimo rechazo antes de volver a solicitar");
        }
        SolicitudVoluntario solicitud = new SolicitudVoluntario();
        solicitud.setUsuario(usuario);
        solicitud.setTipoAyuda(tipoAyuda != null ? tipoAyuda.trim() : null);
        solicitud.setMensaje(mensaje != null && !mensaje.isBlank() ? mensaje.trim() : null);
        solicitud.setEstado(EstadoSolicitudVoluntario.PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setNotificada(false);
        return toDetalle(solicitudRepository.save(solicitud));
    }

    public boolean puedeSolicitarVoluntario(Usuario usuario) {
        SolicitudVoluntario ultima = solicitudRepository.findTopByUsuarioOrderByFechaSolicitudDesc(usuario).orElse(null);
        if (ultima == null) {
            return true;
        }
        if (ultima.getEstado() != EstadoSolicitudVoluntario.RECHAZADA) {
            return true;
        }
        if (ultima.getFechaResolucion() == null) {
            return true;
        }
        LocalDateTime limite = ultima.getFechaResolucion().plusDays(DIAS_BLOQUEO_RECHAZO);
        return LocalDateTime.now().isAfter(limite);
    }

    private void notificarResolucion(SolicitudVoluntario solicitud) {
        try {
            String asunto;
            String mensaje;
            if (solicitud.getEstado() == EstadoSolicitudVoluntario.APROBADA) {
                asunto = "Tu solicitud para ser voluntario fue aprobada";
                mensaje = "??Felicitaciones! Tu solicitud para ser voluntario en ResQ ha sido aprobada. Ya tienes el rol VOLUNTARIO.";
            } else {
                asunto = "Tu solicitud para ser voluntario fue rechazada";
                String motivo = solicitud.getMotivoRechazo() != null ? solicitud.getMotivoRechazo() : "";
                mensaje = "Tu solicitud para ser voluntario en ResQ ha sido rechazada. Motivo: " + motivo;
            }

            notificacionService.crearNotificacion(solicitud.getUsuario().getIdUsuario(), asunto, mensaje);
            emailService.enviar(solicitud.getUsuario().getEmail(), asunto, mensaje);
            solicitud.setNotificada(true);
            solicitudRepository.save(solicitud);
            logger.info("Notificaci??n enviada para solicitud ID: {}", solicitud.getIdSolicitud());
        } catch (Exception e) {
            logger.error("Error al notificar resoluci??n de solicitud ID: {}", solicitud.getIdSolicitud(), e);
            solicitud.setNotificada(false);
            solicitudRepository.save(solicitud);
        }
    }

    private SolicitudVoluntarioResumenDTO toResumen(SolicitudVoluntario s) {
        return new SolicitudVoluntarioResumenDTO(
                s.getIdSolicitud(),
                s.getUsuario() != null ? s.getUsuario().getIdUsuario() : null,
                s.getUsuario() != null ? s.getUsuario().getNombre() : null,
                s.getUsuario() != null ? s.getUsuario().getEmail() : null,
                s.getUsuario() != null ? s.getUsuario().getTelefono() : null,
                s.getTipoAyuda(),
                s.getMensaje(),
                s.getEstado() != null ? s.getEstado().name() : null,
                s.getFechaSolicitud()
        );
    }

    private SolicitudVoluntarioDetalleDTO toDetalle(SolicitudVoluntario s) {
        return new SolicitudVoluntarioDetalleDTO(
                s.getIdSolicitud(),
                s.getUsuario() != null ? s.getUsuario().getIdUsuario() : null,
                s.getUsuario() != null ? s.getUsuario().getNombre() : null,
                s.getUsuario() != null ? s.getUsuario().getEmail() : null,
                s.getUsuario() != null ? s.getUsuario().getTelefono() : null,
                s.getTipoAyuda(),
                s.getMensaje(),
                s.getEstado() != null ? s.getEstado().name() : null,
                s.getFechaSolicitud(),
                s.getFechaResolucion(),
                s.getMotivoRechazo(),
                s.getRevisadoPor() != null ? s.getRevisadoPor().getIdUsuario() : null,
                s.getRevisadoPor() != null ? s.getRevisadoPor().getNombre() : null,
                s.isNotificada()
        );
    }
}
