package com.resq.backend.entity;

/**
 * HU-20: estado de verificación de una organización.
 *
 * Toda organización nace en PENDIENTE_VERIFICACION. Pasar a VERIFICADA o RECHAZADA
 * corresponde a un administrador (historia de verificación, aún no implementada).
 * Mientras no sea VERIFICADA, la organización no puede gestionar casos.
 */
public final class EstadoVerificacion {

    public static final String PENDIENTE = "PENDIENTE_VERIFICACION";
    public static final String VERIFICADA = "VERIFICADA";
    public static final String RECHAZADA = "RECHAZADA";

    private EstadoVerificacion() {
    }
}
