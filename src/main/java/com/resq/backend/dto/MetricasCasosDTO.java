package com.resq.backend.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * HU-33: indicadores de casos y reportes para un periodo [desde, hasta] (ambos inclusive).
 *
 * <ul>
 *   <li><b>Reporte</b>: todo aviso que un usuario registro en la plataforma, haya pasado
 *       o no por la revision del voluntario (HU-19).</li>
 *   <li><b>Caso</b>: un reporte cuya revision fue APROBADA, es decir, un aviso validado
 *       que ya entra a la atencion de voluntarios y organizaciones.</li>
 * </ul>
 *
 * Por eso los reportes se agrupan por su estado de revision y los casos por su estado de
 * atencion (PENDIENTE, EN_PROCESO, RESUELTO, CANCELADO). Se cuentan los creados dentro del
 * periodo, con el estado que tienen hoy: editar o eliminar un reporte cambia el indicador
 * en la siguiente consulta, porque no se guarda nada precalculado.
 *
 * Igual que en HU-34, para comparar dos periodos el frontend pide este recurso dos veces.
 */
public record MetricasCasosDTO(
        LocalDate desde,
        LocalDate hasta,
        Reportes reportes,
        Casos casos) {

    /**
     * @param total        reportes registrados en el periodo.
     * @param porEstado    por estado de revision: PENDIENTE_REVISION, APROBADO, RECHAZADO.
     * @param porCategoria por tipo de caso: PERDIDA, ENCONTRADA, ABANDONADA.
     */
    public record Reportes(long total, List<Conteo> porEstado, List<Conteo> porCategoria) {
    }

    /**
     * @param total        casos (reportes aprobados) registrados en el periodo.
     * @param porEstado    por estado de atencion: PENDIENTE, EN_PROCESO, RESUELTO, CANCELADO.
     * @param porCategoria por tipo de caso.
     * @param porZona      por zona de la ciudad, calculada a partir de las coordenadas
     *                     (ver {@code ZonaCiudad}).
     */
    public record Casos(long total, List<Conteo> porEstado, List<Conteo> porCategoria, List<Conteo> porZona) {
    }

    /**
     * Una fila de una tabla agrupada. Los valores conocidos se listan siempre, aunque
     * esten en cero, para que dos periodos comparados tengan las mismas filas.
     */
    public record Conteo(String clave, long cantidad) {
    }
}
