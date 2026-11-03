package com.resq.backend.service;

import com.resq.backend.dto.OrganizacionAdminDTO;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.Usuario;
import com.resq.backend.exception.OrganizacionException;
import com.resq.backend.repository.OrganizacionRepository;
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
 */
@Service
public class AdminOrganizacionService {

    private static final Logger logger = LoggerFactory.getLogger(AdminOrganizacionService.class);

    static final List<String> ESTADOS = List.of(
            EstadoVerificacion.PENDIENTE, EstadoVerificacion.VERIFICADA, EstadoVerificacion.RECHAZADA);

    private final OrganizacionRepository organizacionRepository;
    private final UsuarioRepository usuarioRepository;

    public AdminOrganizacionService(OrganizacionRepository organizacionRepository,
                                    UsuarioRepository usuarioRepository) {
        this.organizacionRepository = organizacionRepository;
        this.usuarioRepository = usuarioRepository;
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

    @Transactional
    public OrganizacionAdminDTO aprobar(Long idOrganizacion, Long idAdministrador) {
        return decidir(idOrganizacion, idAdministrador, EstadoVerificacion.VERIFICADA);
    }

    @Transactional
    public OrganizacionAdminDTO rechazar(Long idOrganizacion, Long idAdministrador) {
        return decidir(idOrganizacion, idAdministrador, EstadoVerificacion.RECHAZADA);
    }

    private OrganizacionAdminDTO decidir(Long idOrganizacion, Long idAdministrador, String nuevoEstado) {
        Organizacion organizacion = buscar(idOrganizacion);
        if (!EstadoVerificacion.PENDIENTE.equals(organizacion.getEstadoVerificacion())) {
            logger.warn("Revision rechazada: la organizacion ID {} ya esta {}",
                    idOrganizacion, organizacion.getEstadoVerificacion());
            throw new OrganizacionException(HttpStatus.CONFLICT,
                    "La organización ya fue revisada: no está pendiente de verificación",
                    Map.of("estadoVerificacion", organizacion.getEstadoVerificacion()));
        }

        organizacion.setEstadoVerificacion(nuevoEstado);
        organizacion.setIdRevisor(idAdministrador);
        organizacion.setFechaRevision(LocalDateTime.now());
        Organizacion guardada = organizacionRepository.save(organizacion);
        logger.info("Organizacion ID {} {} por el administrador ID {}",
                idOrganizacion, nuevoEstado, idAdministrador);
        return aDto(guardada);
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
