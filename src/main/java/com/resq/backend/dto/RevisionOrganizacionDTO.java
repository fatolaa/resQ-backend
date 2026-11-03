package com.resq.backend.dto;

import com.resq.backend.entity.RevisionOrganizacion;
import com.resq.backend.entity.Usuario;

import java.time.LocalDateTime;

/** HU-32: una entrada del historial de verificacion, con el nombre de quien la hizo. */
public record RevisionOrganizacionDTO(
        Long idRevision,
        String accion,
        String nota,
        Long idUsuario,
        String nombreUsuario,
        LocalDateTime fecha) {

    public static RevisionOrganizacionDTO de(RevisionOrganizacion r, Usuario usuario) {
        return new RevisionOrganizacionDTO(r.getIdRevision(), r.getAccion(), r.getNota(), r.getIdUsuario(),
                usuario == null ? null : usuario.getNombre(), r.getFecha());
    }
}
