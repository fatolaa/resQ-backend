package com.resq.backend.service;

import com.resq.backend.dto.MetricasCasosDTO;
import com.resq.backend.entity.EstadoRevision;
import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.ReporteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * HU-33: calcula los indicadores de casos y reportes de un periodo.
 *
 * Igual que {@link MetricasService} (HU-34), se traen las filas del periodo y se agrupan
 * en memoria: los volumenes son chicos y asi las reglas se prueban sin base de datos.
 * Las validaciones del periodo son las mismas que las de HU-34, para que el panel
 * acepte exactamente los mismos rangos en las dos secciones.
 */
@Service
public class MetricasCasosService {

    private static final Logger logger = LoggerFactory.getLogger(MetricasCasosService.class);

    /** Reporte o caso sin categoria, o con un estado vacio. */
    static final String SIN_DATO = "SIN_DATO";

    // Mismos dominios que valida Reporte (HU-09, HU-13, HU-19) y siembra data.sql.
    static final List<String> CATEGORIAS = List.of("PERDIDA", "ENCONTRADA", "ABANDONADA");
    static final List<String> ESTADOS_CASO = List.of("PENDIENTE", "EN_PROCESO", "RESUELTO", "CANCELADO");
    static final List<String> ESTADOS_REVISION = List.of(
            EstadoRevision.PENDIENTE, EstadoRevision.APROBADO, EstadoRevision.RECHAZADO);

    private final ReporteRepository reporteRepository;

    public MetricasCasosService(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    /**
     * @throws IllegalArgumentException si falta una fecha, el periodo esta invertido o
     *                                  supera {@link MetricasService#MAX_DIAS_PERIODO} dias.
     */
    @Transactional(readOnly = true)
    public MetricasCasosDTO calcular(LocalDate desde, LocalDate hasta) {
        MetricasService.validarPeriodo(desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(LocalTime.MAX);
        List<Reporte> reportes = reporteRepository.findByFechaCreacionBetween(inicio, fin);
        List<Reporte> casos = reportes.stream().filter(MetricasCasosService::esCaso).toList();

        MetricasCasosDTO.Reportes indicadoresReportes = new MetricasCasosDTO.Reportes(
                reportes.size(),
                agrupar(reportes, MetricasCasosService::estadoRevisionDe, ESTADOS_REVISION),
                agrupar(reportes, MetricasCasosService::categoriaDe, CATEGORIAS));

        MetricasCasosDTO.Casos indicadoresCasos = new MetricasCasosDTO.Casos(
                casos.size(),
                agrupar(casos, reporte -> normalizar(reporte.getEstado()), ESTADOS_CASO),
                agrupar(casos, MetricasCasosService::categoriaDe, CATEGORIAS),
                agrupar(casos, reporte -> ZonaCiudad.de(reporte.getLatitud(), reporte.getLongitud()),
                        ZonaCiudad.ZONAS));

        logger.info("Metricas de casos calculadas para el periodo {} a {} ({} reportes, {} casos)",
                desde, hasta, indicadoresReportes.total(), indicadoresCasos.total());

        return new MetricasCasosDTO(desde, hasta, indicadoresReportes, indicadoresCasos);
    }

    /** Un reporte pasa a ser caso cuando un voluntario aprueba su informacion (HU-19). */
    static boolean esCaso(Reporte reporte) {
        return EstadoRevision.APROBADO.equals(estadoRevisionDe(reporte));
    }

    /** Los reportes anteriores a HU-19 (estado_revision null) cuentan como pendientes, igual que en la cola. */
    static String estadoRevisionDe(Reporte reporte) {
        String estado = normalizar(reporte.getEstadoRevision());
        return SIN_DATO.equals(estado) ? EstadoRevision.PENDIENTE : estado;
    }

    static String categoriaDe(Reporte reporte) {
        return normalizar(reporte.getTipoCaso());
    }

    /** " perdida " → "PERDIDA"; null o vacio → SIN_DATO. */
    static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return SIN_DATO;
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Cuenta los reportes por la clave que devuelve {@code clave}. Las filas salen en este
     * orden: primero las conocidas (aunque esten en cero), luego otras en orden alfabetico,
     * y al final las que no tienen dato (SIN_DATO o SIN_UBICACION).
     */
    static List<MetricasCasosDTO.Conteo> agrupar(Collection<Reporte> reportes,
                                                 Function<Reporte, String> clave,
                                                 List<String> conocidas) {
        Map<String, Long> conteo = new HashMap<>();
        reportes.forEach(reporte -> conteo.merge(clave.apply(reporte), 1L, Long::sum));

        Set<String> orden = new LinkedHashSet<>(conocidas);
        conteo.keySet().stream()
                .filter(valor -> !conocidas.contains(valor) && !esSinDato(valor))
                .sorted()
                .forEach(orden::add);
        conteo.keySet().stream().filter(MetricasCasosService::esSinDato).sorted().forEach(orden::add);

        return orden.stream()
                .map(valor -> new MetricasCasosDTO.Conteo(valor, conteo.getOrDefault(valor, 0L)))
                .toList();
    }

    private static boolean esSinDato(String valor) {
        return SIN_DATO.equals(valor) || ZonaCiudad.SIN_UBICACION.equals(valor);
    }
}
