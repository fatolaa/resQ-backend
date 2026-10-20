package com.resq.backend.service;

import com.resq.backend.dto.MetricasDTO;
import com.resq.backend.entity.AccesoUsuario;
import com.resq.backend.entity.EstadoSolicitudVoluntario;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.SolicitudVoluntario;
import com.resq.backend.entity.TipoOrganizacion;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.AccesoUsuarioRepository;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.SolicitudVoluntarioRepository;
import com.resq.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * HU-34: calcula los indicadores de usuarios, voluntarios y organizaciones de un periodo.
 *
 * Los volumenes del proyecto son chicos, asi que se traen las filas del periodo y se
 * agrupan en memoria: las reglas quedan en un solo lugar y se pueden probar sin base
 * de datos.
 */
@Service
public class MetricasService {

    private static final Logger logger = LoggerFactory.getLogger(MetricasService.class);

    /** Dos anios: alcanza para comparar un anio con el anterior sin traer toda la historia. */
    static final int MAX_DIAS_PERIODO = 731;

    /** Hasta un mes la evolucion va por dia; hasta seis meses, por semana; despues, por mes. */
    static final int MAX_DIAS_AGRUPACION_DIARIA = 31;
    static final int MAX_DIAS_AGRUPACION_SEMANAL = 186;

    /** Voluntario u organizacion que no indico su tipo. */
    static final String SIN_TIPO = "SIN_TIPO";

    // Mismas categorias que valida UsuarioController (HU-17). Se listan siempre, aunque
    // esten en cero, para que las tablas de dos periodos tengan las mismas filas.
    private static final List<String> TIPOS_AYUDA = List.of("TRANSPORTE", "HOGAR_TEMPORAL", "ALIMENTO", "RESCATE");

    private static final List<String> TIPOS_ORGANIZACION = List.of(
            TipoOrganizacion.REFUGIO, TipoOrganizacion.VETERINARIA, TipoOrganizacion.RESCATISTA_INDEPENDIENTE);

    private static final String ROL_VOLUNTARIO = "VOLUNTARIO";

    public enum Agrupacion { DIA, SEMANA, MES }

    private final UsuarioRepository usuarioRepository;
    private final AccesoUsuarioRepository accesoUsuarioRepository;
    private final SolicitudVoluntarioRepository solicitudVoluntarioRepository;
    private final OrganizacionRepository organizacionRepository;

    public MetricasService(UsuarioRepository usuarioRepository,
                           AccesoUsuarioRepository accesoUsuarioRepository,
                           SolicitudVoluntarioRepository solicitudVoluntarioRepository,
                           OrganizacionRepository organizacionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.accesoUsuarioRepository = accesoUsuarioRepository;
        this.solicitudVoluntarioRepository = solicitudVoluntarioRepository;
        this.organizacionRepository = organizacionRepository;
    }

    /**
     * @throws IllegalArgumentException si falta una fecha, el periodo esta invertido o
     *                                  supera {@link #MAX_DIAS_PERIODO} dias.
     */
    @Transactional(readOnly = true)
    public MetricasDTO calcular(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(LocalTime.MAX);
        Agrupacion agrupacion = agrupacionPara(desde, hasta);

        List<Usuario> registrados = usuarioRepository.findByFechaRegistroBetween(inicio, fin);
        List<AccesoUsuario> accesos = accesoUsuarioRepository.findByFechaBetween(inicio, fin);
        long activos = accesos.stream().map(AccesoUsuario::getIdUsuario).distinct().count();

        MetricasDTO.Usuarios usuarios = new MetricasDTO.Usuarios(
                registrados.size(),
                activos,
                usuarioRepository.countByFechaRegistroLessThanEqualOrFechaRegistroIsNull(fin));

        logger.info("Metricas calculadas para el periodo {} a {} ({} registrados, {} activos)",
                desde, hasta, usuarios.registrados(), usuarios.activos());

        return new MetricasDTO(
                desde,
                hasta,
                agrupacion.name(),
                usuarios,
                voluntarios(inicio, fin),
                organizaciones(inicio, fin),
                evolucion(desde, hasta, agrupacion, registrados, accesos));
    }

    static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Debes indicar la fecha inicial y la final del periodo");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAX_DIAS_PERIODO) {
            throw new IllegalArgumentException("El periodo no puede superar los " + MAX_DIAS_PERIODO + " dias");
        }
    }

    static Agrupacion agrupacionPara(LocalDate desde, LocalDate hasta) {
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        if (dias <= MAX_DIAS_AGRUPACION_DIARIA) {
            return Agrupacion.DIA;
        }
        return dias <= MAX_DIAS_AGRUPACION_SEMANAL ? Agrupacion.SEMANA : Agrupacion.MES;
    }

    // ---------------------------------------------------------------- voluntarios

    private MetricasDTO.Voluntarios voluntarios(LocalDateTime inicio, LocalDateTime fin) {
        List<SolicitudVoluntario> aprobadas = solicitudVoluntarioRepository
                .findByEstado(EstadoSolicitudVoluntario.APROBADA).stream()
                .filter(solicitud -> solicitud.getUsuario() != null && solicitud.getFechaResolucion() != null)
                .sorted(Comparator.comparing(SolicitudVoluntario::getFechaResolucion))
                .toList();

        // Al aprobar una solicitud (HU-35) el usuario pasa a VOLUNTARIO pero su tipo de
        // ayuda queda solo en la solicitud. Para esos casos se usa la ultima aprobada.
        Map<Long, String> tipoAprobadoPorUsuario = new HashMap<>();
        aprobadas.forEach(solicitud ->
                tipoAprobadoPorUsuario.put(solicitud.getUsuario().getIdUsuario(), solicitud.getTipoAyuda()));

        Map<String, long[]> conteo = new HashMap<>();
        Set<Long> nuevos = new HashSet<>();
        for (SolicitudVoluntario solicitud : aprobadas) {
            if (enPeriodo(solicitud.getFechaResolucion(), inicio, fin)
                    && nuevos.add(solicitud.getUsuario().getIdUsuario())) {
                tiposDe(solicitud.getTipoAyuda())
                        .forEach(tipo -> conteo.computeIfAbsent(tipo, clave -> new long[2])[0]++);
            }
        }

        List<Usuario> actuales = usuarioRepository.findByRolIgnoreCase(ROL_VOLUNTARIO);
        for (Usuario voluntario : actuales) {
            String tipos = esVacio(voluntario.getTipoAyuda())
                    ? tipoAprobadoPorUsuario.get(voluntario.getIdUsuario())
                    : voluntario.getTipoAyuda();
            tiposDe(tipos).forEach(tipo -> conteo.computeIfAbsent(tipo, clave -> new long[2])[1]++);
        }

        List<MetricasDTO.VoluntariosPorTipo> porTipo = ordenarPorTipo(conteo.keySet(), TIPOS_AYUDA).stream()
                .map(tipo -> {
                    long[] valores = conteo.getOrDefault(tipo, new long[2]);
                    return new MetricasDTO.VoluntariosPorTipo(tipo, valores[0], valores[1]);
                })
                .toList();

        return new MetricasDTO.Voluntarios(nuevos.size(), actuales.size(), porTipo);
    }

    /** "TRANSPORTE, alimento" → [TRANSPORTE, ALIMENTO]; vacio → [SIN_TIPO]. */
    static List<String> tiposDe(String tipos) {
        if (esVacio(tipos)) {
            return List.of(SIN_TIPO);
        }
        List<String> separados = Arrays.stream(tipos.split(","))
                .map(String::trim)
                .filter(tipo -> !tipo.isEmpty())
                .map(tipo -> tipo.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        return separados.isEmpty() ? List.of(SIN_TIPO) : separados;
    }

    // ---------------------------------------------------------------- organizaciones

    private MetricasDTO.Organizaciones organizaciones(LocalDateTime inicio, LocalDateTime fin) {
        List<Organizacion> existentes = organizacionRepository
                .findByFechaRegistroLessThanEqualOrFechaRegistroIsNull(fin);

        Map<String, ContadorOrganizaciones> porTipo = new HashMap<>();
        long nuevas = 0;
        for (Organizacion organizacion : existentes) {
            String tipo = esVacio(organizacion.getTipo())
                    ? SIN_TIPO
                    : organizacion.getTipo().trim().toUpperCase(Locale.ROOT);
            ContadorOrganizaciones contador = porTipo.computeIfAbsent(tipo, clave -> new ContadorOrganizaciones());

            contador.total++;
            if (enPeriodo(organizacion.getFechaRegistro(), inicio, fin)) {
                contador.nuevas++;
                nuevas++;
            }
            String estado = organizacion.getEstadoVerificacion();
            if (EstadoVerificacion.VERIFICADA.equals(estado)) {
                contador.verificadas++;
            } else if (EstadoVerificacion.RECHAZADA.equals(estado)) {
                contador.rechazadas++;
            } else {
                contador.pendientes++;
            }
        }

        List<MetricasDTO.OrganizacionesPorTipo> filas = ordenarPorTipo(porTipo.keySet(), TIPOS_ORGANIZACION).stream()
                .map(tipo -> {
                    ContadorOrganizaciones contador = porTipo.getOrDefault(tipo, new ContadorOrganizaciones());
                    return new MetricasDTO.OrganizacionesPorTipo(tipo, contador.nuevas, contador.pendientes,
                            contador.verificadas, contador.rechazadas, contador.total);
                })
                .toList();

        return new MetricasDTO.Organizaciones(nuevas, existentes.size(), filas);
    }

    private static final class ContadorOrganizaciones {
        private long nuevas;
        private long pendientes;
        private long verificadas;
        private long rechazadas;
        private long total;
    }

    // ---------------------------------------------------------------- evolucion

    private List<MetricasDTO.PuntoEvolucion> evolucion(LocalDate desde, LocalDate hasta, Agrupacion agrupacion,
                                                       List<Usuario> registrados, List<AccesoUsuario> accesos) {
        TreeMap<LocalDate, long[]> registradosPorTramo = new TreeMap<>();
        TreeMap<LocalDate, Set<Long>> activosPorTramo = new TreeMap<>();
        for (LocalDate inicio : iniciosDeTramo(desde, hasta, agrupacion)) {
            registradosPorTramo.put(inicio, new long[1]);
            activosPorTramo.put(inicio, new HashSet<>());
        }

        for (Usuario usuario : registrados) {
            registradosPorTramo.floorEntry(usuario.getFechaRegistro().toLocalDate()).getValue()[0]++;
        }
        for (AccesoUsuario acceso : accesos) {
            activosPorTramo.floorEntry(acceso.getFecha().toLocalDate()).getValue().add(acceso.getIdUsuario());
        }

        return registradosPorTramo.keySet().stream()
                .map(inicio -> new MetricasDTO.PuntoEvolucion(
                        inicio, registradosPorTramo.get(inicio)[0], activosPorTramo.get(inicio).size()))
                .toList();
    }

    /**
     * Primer dia de cada tramo. El primero siempre es {@code desde}; con agrupacion por
     * mes, los siguientes caen el dia 1 de cada mes.
     */
    static List<LocalDate> iniciosDeTramo(LocalDate desde, LocalDate hasta, Agrupacion agrupacion) {
        List<LocalDate> inicios = new ArrayList<>();
        LocalDate actual = desde;
        while (!actual.isAfter(hasta)) {
            inicios.add(actual);
            actual = switch (agrupacion) {
                case DIA -> actual.plusDays(1);
                case SEMANA -> actual.plusWeeks(1);
                case MES -> actual.withDayOfMonth(1).plusMonths(1);
            };
        }
        return inicios;
    }

    // ---------------------------------------------------------------- utilidades

    /** Primero los tipos conocidos (aunque esten en cero), luego otros, y SIN_TIPO al final. */
    private static List<String> ordenarPorTipo(Set<String> encontrados, List<String> conocidos) {
        Set<String> orden = new LinkedHashSet<>(conocidos);
        encontrados.stream()
                .filter(tipo -> !conocidos.contains(tipo) && !SIN_TIPO.equals(tipo))
                .sorted()
                .forEach(orden::add);
        if (encontrados.contains(SIN_TIPO)) {
            orden.add(SIN_TIPO);
        }
        return List.copyOf(orden);
    }

    private static boolean enPeriodo(LocalDateTime fecha, LocalDateTime inicio, LocalDateTime fin) {
        return fecha != null && !fecha.isBefore(inicio) && !fecha.isAfter(fin);
    }

    private static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
