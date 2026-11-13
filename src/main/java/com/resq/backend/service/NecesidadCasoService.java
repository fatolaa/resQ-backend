package com.resq.backend.service;

import com.resq.backend.dto.NecesidadCasoDTO;
import com.resq.backend.entity.EstadoRevision;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.NecesidadCaso;
import com.resq.backend.entity.Notificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.Reporte;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.NecesidadCasoRepository;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.ReporteRepository;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.UsuarioAutenticado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rediseño: "Que necesita" cada caso.
 *
 * - Quien reporta elige las necesidades al crear el caso; despues quien reporto o la
 *   organizacion del caso (o un admin) pueden agregar o quitar.
 * - Cualquier usuario con sesion, salvo quien reporto, se ofrece (BUSCADA → OFRECIDA)
 *   y se avisa a quien reporto y a la organizacion.
 * - Quien reporto o la organizacion confirma (OFRECIDA → CUBIERTA) o libera (vuelve a
 *   BUSCADA). Quien se ofrecio tambien puede retirar su oferta.
 * - Mientras el caso no este abierto (PENDIENTE o EN_PROCESO) no se puede cambiar nada.
 */
@Service
public class NecesidadCasoService {

    private static final Logger logger = LoggerFactory.getLogger(NecesidadCasoService.class);

    private final NecesidadCasoRepository necesidadRepository;
    private final ReporteRepository reporteRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacionRepository organizacionRepository;
    private final NotificacionService notificacionService;

    public NecesidadCasoService(NecesidadCasoRepository necesidadRepository,
                                ReporteRepository reporteRepository,
                                UsuarioRepository usuarioRepository,
                                OrganizacionRepository organizacionRepository,
                                NotificacionService notificacionService) {
        this.necesidadRepository = necesidadRepository;
        this.reporteRepository = reporteRepository;
        this.usuarioRepository = usuarioRepository;
        this.organizacionRepository = organizacionRepository;
        this.notificacionService = notificacionService;
    }

    // ============================================================ consultar

    @Transactional(readOnly = true)
    public List<NecesidadCasoDTO> listar(Long idReporte) {
        Reporte reporte = buscarCaso(idReporte);
        return aDTOs(necesidadRepository.findByIdReporteOrderByIdNecesidadAsc(reporte.getIdReporte()));
    }

    /** Casos que todavia buscan este tipo de ayuda. */
    @Transactional(readOnly = true)
    public Set<Long> casosQueBuscan(String tipo) {
        return necesidadRepository.idsDeCasosQueBuscan(validarTipo(tipo));
    }

    // ============================================================ al crear el reporte

    /** Valida y limpia la lista que llega con el reporte nuevo (sin repetidos). */
    public List<String> validarTipos(Collection<String> tipos) {
        if (tipos == null) {
            return List.of();
        }
        Set<String> limpios = new LinkedHashSet<>();
        for (String tipo : tipos) {
            limpios.add(validarTipo(tipo));
        }
        return List.copyOf(limpios);
    }

    @Transactional
    public void crearIniciales(Long idReporte, List<String> tipos) {
        for (String tipo : tipos) {
            NecesidadCaso n = new NecesidadCaso();
            n.setIdReporte(idReporte);
            n.setTipo(tipo);
            n.setEstado(NecesidadCaso.BUSCADA);
            necesidadRepository.save(n);
        }
    }

    // ============================================================ gestionar

    @Transactional
    public List<NecesidadCasoDTO> agregar(Long idReporte, String tipo, Long idUsuario) {
        Reporte reporte = buscarCasoAbierto(idReporte);
        exigirGestion(reporte, idUsuario);
        String limpio = validarTipo(tipo);
        if (necesidadRepository.existsByIdReporteAndTipo(idReporte, limpio)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El caso ya tiene esa necesidad");
        }
        crearIniciales(idReporte, List.of(limpio));
        return listar(idReporte);
    }

    @Transactional
    public List<NecesidadCasoDTO> quitar(Long idReporte, Long idNecesidad, Long idUsuario) {
        Reporte reporte = buscarCasoAbierto(idReporte);
        exigirGestion(reporte, idUsuario);
        necesidadRepository.delete(buscarNecesidad(idReporte, idNecesidad));
        return listar(idReporte);
    }

    @Transactional
    public List<NecesidadCasoDTO> ofrecer(Long idReporte, Long idNecesidad, Long idUsuario) {
        Reporte reporte = buscarCasoAbierto(idReporte);
        if (idUsuario.equals(reporte.getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes ofrecerte en tu propio reporte");
        }
        NecesidadCaso necesidad = buscarNecesidad(idReporte, idNecesidad);
        if (!NecesidadCaso.BUSCADA.equals(necesidad.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alguien ya se ofreció para esta necesidad");
        }
        necesidad.setEstado(NecesidadCaso.OFRECIDA);
        necesidad.setIdAyudante(idUsuario);
        necesidadRepository.save(necesidad);
        logger.info("Usuario ID {} se ofreció para {} en el caso ID {}", idUsuario, necesidad.getTipo(), idReporte);
        avisarOferta(reporte, necesidad, idUsuario);
        return listar(idReporte);
    }

    @Transactional
    public List<NecesidadCasoDTO> confirmar(Long idReporte, Long idNecesidad, Long idUsuario) {
        Reporte reporte = buscarCasoAbierto(idReporte);
        exigirGestion(reporte, idUsuario);
        NecesidadCaso necesidad = buscarNecesidad(idReporte, idNecesidad);
        if (!NecesidadCaso.OFRECIDA.equals(necesidad.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se confirma una necesidad que alguien ofreció");
        }
        necesidad.setEstado(NecesidadCaso.CUBIERTA);
        necesidadRepository.save(necesidad);
        return listar(idReporte);
    }

    /** Vuelve a buscarse: la gestion del caso o quien se habia ofrecido. */
    @Transactional
    public List<NecesidadCasoDTO> liberar(Long idReporte, Long idNecesidad, Long idUsuario) {
        Reporte reporte = buscarCasoAbierto(idReporte);
        NecesidadCaso necesidad = buscarNecesidad(idReporte, idNecesidad);
        boolean esSuOferta = idUsuario.equals(necesidad.getIdAyudante());
        if (!esSuOferta) {
            exigirGestion(reporte, idUsuario);
        }
        necesidad.setEstado(NecesidadCaso.BUSCADA);
        necesidad.setIdAyudante(null);
        necesidadRepository.save(necesidad);
        return listar(idReporte);
    }

    /** Quien reporto, el representante de la organizacion del caso o un admin. */
    public boolean gestiona(Reporte reporte, Long idUsuario) {
        if (idUsuario == null) {
            return false;
        }
        if (idUsuario.equals(reporte.getIdUsuario()) || UsuarioAutenticado.esAdmin()) {
            return true;
        }
        Long idOrganizacion = reporte.getIdOrganizacion();
        return idOrganizacion != null && organizacionRepository.findByIdRepresentante(idUsuario)
                .filter(o -> idOrganizacion.equals(o.getIdOrganizacion()))
                .filter(o -> EstadoVerificacion.VERIFICADA.equals(o.getEstadoVerificacion()))
                .isPresent();
    }

    // ============================================================ apoyo

    private void exigirGestion(Reporte reporte, Long idUsuario) {
        if (!gestiona(reporte, idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo quien reportó o la organización del caso pueden cambiar sus necesidades");
        }
    }

    private Reporte buscarCaso(Long idReporte) {
        return reporteRepository.findById(idReporte)
                .filter(r -> !EstadoRevision.RECHAZADO.equals(r.getEstadoRevision()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El caso no existe"));
    }

    private Reporte buscarCasoAbierto(Long idReporte) {
        Reporte reporte = buscarCaso(idReporte);
        if (!"PENDIENTE".equals(reporte.getEstado()) && !"EN_PROCESO".equals(reporte.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El caso ya está cerrado");
        }
        return reporte;
    }

    private NecesidadCaso buscarNecesidad(Long idReporte, Long idNecesidad) {
        return necesidadRepository.findById(idNecesidad)
                .filter(n -> idReporte.equals(n.getIdReporte()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La necesidad no existe"));
    }

    private static String validarTipo(String tipo) {
        String limpio = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!NecesidadCaso.TIPOS.contains(limpio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de necesidad inválido: " + tipo);
        }
        return limpio;
    }

    private List<NecesidadCasoDTO> aDTOs(List<NecesidadCaso> necesidades) {
        Set<Long> ids = new HashSet<>();
        necesidades.forEach(n -> {
            if (n.getIdAyudante() != null) {
                ids.add(n.getIdAyudante());
            }
        });
        Map<Long, Usuario> ayudantes = ids.isEmpty() ? Map.of()
                : usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getIdUsuario, Function.identity()));
        // Map.of() no acepta get(null): sin ayudante no se busca nada.
        return necesidades.stream()
                .map(n -> NecesidadCasoDTO.de(n, n.getIdAyudante() == null ? null : ayudantes.get(n.getIdAyudante())))
                .toList();
    }

    /** Aviso en la bandeja de quien reporto y de la organizacion; si falla, la oferta queda igual. */
    private void avisarOferta(Reporte reporte, NecesidadCaso necesidad, Long idAyudante) {
        String nombre = usuarioRepository.findById(idAyudante).map(Usuario::getNombre).orElse("Alguien");
        String texto = nombre + " se ofreció para " + necesidad.getTipo().replace('_', ' ').toLowerCase()
                + " en el caso #" + reporte.getIdReporte();
        Set<Long> destinatarios = new LinkedHashSet<>();
        destinatarios.add(reporte.getIdUsuario());
        if (reporte.getIdOrganizacion() != null) {
            organizacionRepository.findById(reporte.getIdOrganizacion())
                    .map(Organizacion::getIdRepresentante)
                    .ifPresent(destinatarios::add);
        }
        destinatarios.remove(idAyudante);
        destinatarios.remove(null);
        for (Long destino : destinatarios) {
            try {
                notificacionService.crearNotificacion(destino, reporte.getIdReporte(), null,
                        Notificacion.NECESIDAD_OFRECIDA, texto);
            } catch (RuntimeException e) {
                logger.error("No se pudo avisar al usuario ID {} de la oferta en el caso ID {}", destino,
                        reporte.getIdReporte(), e);
            }
        }
    }
}
