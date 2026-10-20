package com.resq.backend.service;

import com.resq.backend.dto.AdopcionRequestDTO;
import com.resq.backend.entity.Adopcion;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.Reporte;
import com.resq.backend.exception.AdopcionException;
import com.resq.backend.repository.AdopcionRepository;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.ReporteRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * HU-30: registro y consulta de adopciones.
 *
 * - Solo la registra el representante de una organización verificada.
 * - El caso (reporte) debe existir, estar cerrado (RESUELTO) y no tener ya una adopción.
 * - La adopción solo la pueden consultar la organización que la realizó y quien
 *   reportó el caso.
 */
@Service
public class AdopcionService {

    /** "Caso cerrado" en el dominio de reportes: un caso resuelto. */
    private static final String ESTADO_CERRADO = "RESUELTO";

    private static final Logger logger = LoggerFactory.getLogger(AdopcionService.class);

    private final AdopcionRepository adopcionRepository;
    private final ReporteRepository reporteRepository;
    private final OrganizacionRepository organizacionRepository;
    private final Validator validator;

    public AdopcionService(AdopcionRepository adopcionRepository,
                           ReporteRepository reporteRepository,
                           OrganizacionRepository organizacionRepository,
                           Validator validator) {
        this.adopcionRepository = adopcionRepository;
        this.reporteRepository = reporteRepository;
        this.organizacionRepository = organizacionRepository;
        this.validator = validator;
    }

    public Adopcion registrar(Long idRepresentante, AdopcionRequestDTO solicitud) {
        AdopcionRequestDTO datos = normalizar(solicitud);
        validar(datos);

        Organizacion organizacion = organizacionRepository.findByIdRepresentante(idRepresentante)
                .orElseThrow(() -> {
                    logger.warn("Adopcion rechazada: usuario ID {} no representa una organizacion", idRepresentante);
                    return new AdopcionException(HttpStatus.FORBIDDEN,
                            "Debes representar una organización para registrar una adopción",
                            Map.of("idOrganizacion", "No tienes una organización registrada"));
                });

        if (!EstadoVerificacion.VERIFICADA.equals(organizacion.getEstadoVerificacion())) {
            logger.warn("Adopcion rechazada: organizacion ID {} no esta verificada",
                    organizacion.getIdOrganizacion());
            throw new AdopcionException(HttpStatus.FORBIDDEN,
                    "Tu organización debe estar verificada para registrar adopciones",
                    Map.of("idOrganizacion", "La organización no está verificada"));
        }

        Reporte reporte = reporteRepository.findById(datos.idReporte())
                .orElseThrow(() -> {
                    logger.warn("Adopcion rechazada: el caso ID {} no existe", datos.idReporte());
                    return new AdopcionException(HttpStatus.NOT_FOUND,
                            "El caso no existe",
                            Map.of("idReporte", "No existe un caso con ese id"));
                });

        if (!ESTADO_CERRADO.equals(reporte.getEstado())) {
            logger.warn("Adopcion rechazada: el caso ID {} no esta cerrado (estado {})",
                    datos.idReporte(), reporte.getEstado());
            throw new AdopcionException(HttpStatus.CONFLICT,
                    "Solo se puede registrar la adopción de un caso cerrado",
                    Map.of("idReporte", "El caso debe estar resuelto"));
        }

        if (adopcionRepository.existsByIdReporte(datos.idReporte())) {
            logger.warn("Adopcion rechazada: el caso ID {} ya tiene una adopcion", datos.idReporte());
            throw new AdopcionException(HttpStatus.CONFLICT,
                    "Este caso ya tiene una adopción registrada",
                    Map.of("idReporte", "El caso ya fue adoptado"));
        }

        Adopcion adopcion = new Adopcion();
        adopcion.setIdReporte(datos.idReporte());
        adopcion.setIdOrganizacion(organizacion.getIdOrganizacion());
        adopcion.setNombreAdoptante(datos.nombreAdoptante());
        adopcion.setTelefonoAdoptante(datos.telefonoAdoptante());
        adopcion.setEmailAdoptante(datos.emailAdoptante());
        adopcion.setFechaAdopcion(datos.fechaAdopcion());
        adopcion.setObservaciones(datos.observaciones());

        Adopcion guardada = adopcionRepository.save(adopcion);
        logger.info("Adopcion ID {} registrada por organizacion ID {} para el caso ID {}",
                guardada.getIdAdopcion(), organizacion.getIdOrganizacion(), datos.idReporte());
        return guardada;
    }

    /** Adopción por id, solo si el solicitante es la organización o el reportador del caso. */
    public Optional<Adopcion> obtenerVisible(Long idAdopcion, Long idSolicitante) {
        Optional<Adopcion> adopcion = idAdopcion == null
                ? Optional.empty()
                : adopcionRepository.findById(idAdopcion);
        adopcion.ifPresent(a -> exigirVisibilidad(a, idSolicitante));
        return adopcion;
    }

    /** Adopción de un caso, con la misma regla de visibilidad. */
    public Optional<Adopcion> obtenerDeReporteVisible(Long idReporte, Long idSolicitante) {
        Optional<Adopcion> adopcion = idReporte == null
                ? Optional.empty()
                : adopcionRepository.findByIdReporte(idReporte);
        adopcion.ifPresent(a -> exigirVisibilidad(a, idSolicitante));
        return adopcion;
    }

    private void exigirVisibilidad(Adopcion adopcion, Long idSolicitante) {
        if (idSolicitante == null || !esInvolucrado(adopcion, idSolicitante)) {
            logger.warn("Consulta de adopcion ID {} rechazada para el solicitante ID {}",
                    adopcion.getIdAdopcion(), idSolicitante);
            throw new AdopcionException(HttpStatus.FORBIDDEN,
                    "No puedes consultar los datos de esta adopción",
                    Map.of("adopcion", "Solo la organización y quien reportó el caso pueden consultarla"));
        }
    }

    /** Involucrados: quien reportó el caso y el representante de la organización que adoptó. */
    private boolean esInvolucrado(Adopcion adopcion, Long idSolicitante) {
        boolean esReportador = reporteRepository.findById(adopcion.getIdReporte())
                .map(r -> idSolicitante.equals(r.getIdUsuario()))
                .orElse(false);
        if (esReportador) {
            return true;
        }
        return organizacionRepository.findById(adopcion.getIdOrganizacion())
                .map(o -> idSolicitante.equals(o.getIdRepresentante()))
                .orElse(false);
    }

    private void validar(AdopcionRequestDTO datos) {
        Set<ConstraintViolation<AdopcionRequestDTO>> violaciones = validator.validate(datos);
        if (violaciones.isEmpty()) {
            return;
        }
        Map<String, String> errores = new TreeMap<>();
        violaciones.stream()
                .sorted((a, b) -> a.getMessage().compareTo(b.getMessage()))
                .forEach(v -> errores.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        logger.warn("Adopcion rechazada por validacion: {} campo(s) con error", errores.size());
        throw new AdopcionException(HttpStatus.BAD_REQUEST, "Revisa los datos del formulario", errores);
    }

    private static AdopcionRequestDTO normalizar(AdopcionRequestDTO d) {
        return new AdopcionRequestDTO(
                d.idReporte(),
                limpiar(d.nombreAdoptante()),
                limpiar(d.telefonoAdoptante()),
                limpiar(d.emailAdoptante()),
                d.fechaAdopcion(),
                limpiar(d.observaciones()));
    }

    /** Recorta espacios; un texto vacío queda como null para que @NotBlank lo detecte. */
    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
