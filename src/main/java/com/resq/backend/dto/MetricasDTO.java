package com.resq.backend.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * HU-34: indicadores de la comunidad para un periodo [desde, hasta] (ambos inclusive).
 *
 * Para comparar dos periodos el frontend pide este mismo recurso dos veces: asi
 * cada periodo se calcula con las mismas reglas y la respuesta no depende de cuantos
 * periodos se comparen.
 *
 * @param agrupacion tamano de cada tramo de {@code evolucion}: DIA, SEMANA o MES,
 *                   segun lo largo que sea el periodo.
 */
public record MetricasDTO(
        LocalDate desde,
        LocalDate hasta,
        String agrupacion,
        Usuarios usuarios,
        Voluntarios voluntarios,
        Organizaciones organizaciones,
        List<PuntoEvolucion> evolucion) {

    /**
     * @param registrados   cuentas creadas dentro del periodo.
     * @param activos       usuarios distintos que iniciaron sesion dentro del periodo.
     * @param totalAlCierre cuentas que existian al terminar el periodo.
     */
    public record Usuarios(long registrados, long activos, long totalAlCierre) {
    }

    /**
     * @param nuevos       usuarios cuya solicitud de voluntario se aprobo dentro del periodo.
     * @param actuales     usuarios que hoy tienen el rol VOLUNTARIO (no depende del periodo).
     * @param porTipoAyuda un voluntario que ofrece varios tipos de ayuda cuenta en cada
     *                     uno, asi que la suma por tipo puede superar al total.
     */
    public record Voluntarios(long nuevos, long actuales, List<VoluntariosPorTipo> porTipoAyuda) {
    }

    public record VoluntariosPorTipo(String tipoAyuda, long nuevos, long actuales) {
    }

    /**
     * @param nuevas        organizaciones registradas dentro del periodo.
     * @param totalAlCierre organizaciones que existian al terminar el periodo.
     * @param porTipo       las columnas de estado usan la verificacion actual de cada
     *                      organizacion: no se guarda el historial de cambios de estado.
     */
    public record Organizaciones(long nuevas, long totalAlCierre, List<OrganizacionesPorTipo> porTipo) {
    }

    public record OrganizacionesPorTipo(
            String tipo,
            long nuevas,
            long pendientes,
            long verificadas,
            long rechazadas,
            long total) {
    }

    /** Un tramo de la evolucion; {@code inicio} es su primer dia. */
    public record PuntoEvolucion(LocalDate inicio, long registrados, long activos) {
    }
}
