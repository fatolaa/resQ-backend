package com.resq.backend.service;

import com.resq.backend.dto.OrganizacionAdminDTO;
import com.resq.backend.dto.RevisionOrganizacionDTO;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Notificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.RevisionOrganizacion;
import com.resq.backend.entity.Usuario;
import com.resq.backend.exception.OrganizacionException;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.RevisionOrganizacionRepository;
import com.resq.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HU-25: el administrador consulta las organizaciones registradas y las aprueba o rechaza.
 *
 * Solo se puede decidir sobre una organizacion PENDIENTE_VERIFICACION: asi una decision no
 * pisa otra (por ejemplo, dos administradores revisando a la vez).
 *
 * HU-32: cada decision lleva una nota (obligatoria al rechazar), queda en el historial y se
 * notifica al representante de la organizacion.
 */
@Service
public class AdminOrganizacionService {

    private static final Logger logger = LoggerFactory.getLogger(AdminOrganizacionService.class);

    static final List<String> ESTADOS = List.of(
            EstadoVerificacion.PENDIENTE, EstadoVerificacion.VERIFICADA, EstadoVerificacion.RECHAZADA);

    private final OrganizacionRepository organizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoStorageService documentoStorage;
    private final RevisionOrganizacionRepository revisionRepository;
    private final NotificacionService notificacionService;
    private final EmailService emailService;

    public AdminOrganizacionService(OrganizacionRepository organizacionRepository,
                                    UsuarioRepository usuarioRepository,
                                    DocumentoStorageService documentoStorage,
                                    RevisionOrganizacionRepository revisionRepository,
                                    NotificacionService notificacionService,
                                    EmailService emailService) {
        this.organizacionRepository = organizacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.documentoStorage = documentoStorage;
        this.revisionRepository = revisionRepository;
        this.notificacionService = notificacionService;
        this.emailService = emailService;
    }

    /**
     * Todas las organizaciones, las mas recientes primero. {@code estado} filtra por estado de
     * verificacion; null o vacio trae todas.
     *
     * @throws OrganizacionException 400 si el estado no es uno de los tres conocidos.
     */
    @Transactional(readOnly = true)
    public List<OrganizacionAdminDTO> listar(String estado) {
        String filtro = normalizarEstado(estado);
        List<Organizacion> organizaciones = filtro == null
                ? organizacionRepository.findAllByOrderByFechaRegistroDesc()
                : organizacionRepository.findByEstadoVerificacionOrderByFechaRegistroDesc(filtro);

        // Una sola consulta para los nombres de representantes y revisores de toda la lista.
        Set<Long> ids = new HashSet<>();
        organizaciones.forEach(o -> {
            ids.add(o.getIdRepresentante());
            if (o.getIdRevisor() != null) {
                ids.add(o.getIdRevisor());
            }
        });
        Map<Long, Usuario> usuarios = usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getIdUsuario, Function.identity()));

        return organizaciones.stream()
                .map(o -> OrganizacionAdminDTO.de(o, usuarios.get(o.getIdRepresentante()),
                        o.getIdRevisor() == null ? null : usuarios.get(o.getIdRevisor())))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizacionAdminDTO detalle(Long idOrganizacion) {
        return aDto(buscar(idOrganizacion));
    }

    /**
     * HU-32: el documento de verificacion que adjunto la organizacion.
     *
     * @throws OrganizacionException 404 si la organizacion no existe o no tiene documento.
     */
    @Transactional(readOnly = true)
    public DocumentoStorageService.DocumentoAlmacenado documento(Long idOrganizacion) {
        Organizacion organizacion = buscar(idOrganizacion);
        return documentoStorage.cargar(organizacion.getDocumento())
                .orElseThrow(() -> new OrganizacionException(HttpStatus.NOT_FOUND,
                        "La organización no adjuntó un documento", Map.of("documento", "Sin documento")));
    }

    /** HU-32: historial de verificacion de la organizacion, lo mas reciente primero. */
    @Transactional(readOnly = true)
    public List<RevisionOrganizacionDTO> historial(Long idOrganizacion) {
        buscar(idOrganizacion);
        List<RevisionOrganizacion> revisiones =
                revisionRepository.findByIdOrganizacionOrderByFechaDescIdRevisionDesc(idOrganizacion);
        Set<Long> ids = revisiones.stream().map(RevisionOrganizacion::getIdUsuario).collect(Collectors.toSet());
        Map<Long, Usuario> usuarios = usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getIdUsuario, Function.identity()));
        return revisiones.stream()
                .map(r -> RevisionOrganizacionDTO.de(r, usuarios.get(r.getIdUsuario())))
                .toList();
    }

    /** La nota es opcional al aprobar (por ejemplo, "documentacion en regla"). */
    @Transactional
    public OrganizacionAdminDTO aprobar(Long idOrganizacion, Long idAdministrador, String nota) {
        return decidir(idOrganizacion, idAdministrador, EstadoVerificacion.VERIFICADA, limpiar(nota));
    }

    /**
     * Al rechazar, el motivo es obligatorio: la organizacion lo necesita para corregir su
     * solicitud y reenviarla.
     *
     * @throws OrganizacionException 400 si el motivo esta vacio.
     */
    @Transactional
    public OrganizacionAdminDTO rechazar(Long idOrganizacion, Long idAdministrador, String motivo) {
        String nota = limpiar(motivo);
        if (nota == null) {
            throw new OrganizacionException(HttpStatus.BAD_REQUEST, "Indica el motivo del rechazo",
                    Map.of("nota", "El motivo del rechazo es obligatorio"));
        }
        return decidir(idOrganizacion, idAdministrador, EstadoVerificacion.RECHAZADA, nota);
    }

    private OrganizacionAdminDTO decidir(Long idOrganizacion, Long idAdministrador, String nuevoEstado,
                                         String nota) {
        Organizacion organizacion = buscar(idOrganizacion);
        if (!EstadoVerificacion.PENDIENTE.equals(organizacion.getEstadoVerificacion())) {
            logger.warn("Revision rechazada: la organizacion ID {} ya esta {}",
                    idOrganizacion, organizacion.getEstadoVerificacion());
            throw new OrganizacionException(HttpStatus.CONFLICT,
                    "La organización ya fue revisada: no está pendiente de verificación",
                    Map.of("estadoVerificacion", organizacion.getEstadoVerificacion()));
        }

        LocalDateTime ahora = LocalDateTime.now();
        organizacion.setEstadoVerificacion(nuevoEstado);
        organizacion.setIdRevisor(idAdministrador);
        organizacion.setFechaRevision(ahora);
        organizacion.setNotaRevision(nota);
        Organizacion guardada = organizacionRepository.save(organizacion);

        boolean aprobada = EstadoVerificacion.VERIFICADA.equals(nuevoEstado);
        revisionRepository.save(new RevisionOrganizacion(idOrganizacion,
                aprobada ? RevisionOrganizacion.APROBADA : RevisionOrganizacion.RECHAZADA,
                nota, idAdministrador, ahora));
        logger.info("Organizacion ID {} {} por el administrador ID {}",
                idOrganizacion, nuevoEstado, idAdministrador);

        notificarResultado(guardada, aprobada);
        return aDto(guardada);
    }

    /**
     * Aviso en la bandeja (HU-37) y por correo. Si el aviso falla, la decision ya tomada no
     * se deshace: es un efecto secundario, igual que el correo de confirmacion del registro.
     */
    private void notificarResultado(Organizacion o, boolean aprobada) {
        String mensaje = aprobada
                ? "Tu organización \"" + o.getNombre() + "\" fue verificada. Ya puedes gestionar casos en ResQ."
                        + (o.getNotaRevision() == null ? "" : " Nota del administrador: " + o.getNotaRevision())
                : "Tu organización \"" + o.getNombre() + "\" fue rechazada. Motivo: " + o.getNotaRevision()
                        + ". Corrige lo indicado y vuelve a enviar la solicitud desde tu perfil.";
        try {
            notificacionService.crearNotificacion(o.getIdRepresentante(), null, null,
                    aprobada ? Notificacion.ORGANIZACION_VERIFICADA : Notificacion.ORGANIZACION_RECHAZADA, mensaje);
        } catch (RuntimeException e) {
            logger.error("No se pudo notificar la verificacion de la organizacion ID {}", o.getIdOrganizacion(), e);
        }
        emailService.enviar(o.getEmail(),
                aprobada ? "ResQ: tu organización fue verificada" : "ResQ: tu organización fue rechazada",
                mensaje);
    }

    private Organizacion buscar(Long idOrganizacion) {
        return organizacionRepository.findById(idOrganizacion)
                .orElseThrow(() -> new OrganizacionException(HttpStatus.NOT_FOUND, "La organización no existe",
                        Map.of("idOrganizacion", "La organización no existe")));
    }

    private OrganizacionAdminDTO aDto(Organizacion o) {
        Usuario representante = usuarioRepository.findById(o.getIdRepresentante()).orElse(null);
        Usuario revisor = o.getIdRevisor() == null ? null
                : usuarioRepository.findById(o.getIdRevisor()).orElse(null);
        return OrganizacionAdminDTO.de(o, representante, revisor);
    }

    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    /** " pendiente_verificacion " → "PENDIENTE_VERIFICACION"; vacio → null (sin filtro). */
    static String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        String valor = estado.trim().toUpperCase(Locale.ROOT);
        if (!ESTADOS.contains(valor)) {
            throw new OrganizacionException(HttpStatus.BAD_REQUEST,
                    "Estado inválido: " + estado.trim() + ". Permitidos: " + String.join(", ", ESTADOS),
                    Map.of("estado", "El estado debe ser " + String.join(", ", ESTADOS)));
        }
        return valor;
    }
}
