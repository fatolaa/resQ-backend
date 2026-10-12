package com.resq.backend.dto;

import java.time.LocalDateTime;

public record SolicitudVoluntarioDetalleDTO(
        Long idSolicitud,
        Long idUsuario,
        String nombreUsuario,
        String emailUsuario,
        String telefonoUsuario,
        String tipoAyuda,
        String mensaje,
        String estado,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaResolucion,
        String motivoRechazo,
        Long revisadoPorId,
        String revisadoPorNombre,
        boolean notificada
) {
}