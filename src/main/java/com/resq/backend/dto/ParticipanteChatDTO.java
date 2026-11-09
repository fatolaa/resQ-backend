package com.resq.backend.dto;

import java.time.LocalDateTime;

/**
 * HU-22: un participante del chat del caso y su presencia. {@code ultimaVez} es cuando se
 * desconecto por ultima vez desde que arranco el servidor (null si no se sabe).
 */
public record ParticipanteChatDTO(
        Long idUsuario,
        String nombre,
        String rol,
        boolean enLinea,
        LocalDateTime ultimaVez) {

    /** Reportante del caso. */
    public static final String REPORTANTE = "REPORTANTE";
    /** Representante de la organizacion que tomo el caso. */
    public static final String ORGANIZACION = "ORGANIZACION";
    /** Voluntario que reviso y aprobo el reporte (HU-19). */
    public static final String VOLUNTARIO = "VOLUNTARIO";
}
