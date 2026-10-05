package com.resq.backend.entity;

/**
 * HU-19: resultado de la revisión de un reporte por un voluntario.
 * Es independiente de Reporte.estado (PENDIENTE, EN_PROCESO, ...), que indica
 * cómo avanza la atención del caso y no si la información fue validada.
 */
public final class EstadoRevision {

    public static final String PENDIENTE = "PENDIENTE_REVISION";
    public static final String APROBADO = "APROBADO";
    public static final String RECHAZADO = "RECHAZADO";

    private EstadoRevision() {
    }
}
