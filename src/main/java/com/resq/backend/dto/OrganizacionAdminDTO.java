package com.resq.backend.dto;

import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.Usuario;

import java.time.LocalDateTime;

/**
 * HU-25: una organizacion tal como la ve el administrador.
 *
 * Suma a los datos de la organizacion quien la representa y, si ya fue revisada, quien
 * la aprobo o rechazo y cuando. Esos datos no estan en el JSON publico de la entidad.
 */
public record OrganizacionAdminDTO(
        Long idOrganizacion,
        String nombre,
        String tipo,
        String direccion,
        String telefono,
        String email,
        String descripcion,
        String horarios,
        String zonasCobertura,
        String logoUrl,
        boolean tieneDocumento,
        String estadoVerificacion,
        LocalDateTime fechaRegistro,
        Long idRepresentante,
        String nombreRepresentante,
        String emailRepresentante,
        Long idRevisor,
        String nombreRevisor,
        LocalDateTime fechaRevision,
        String notaRevision) {

    /** {@code representante} y {@code revisor} pueden ser null (cuenta borrada o sin revisar). */
    public static OrganizacionAdminDTO de(Organizacion o, Usuario representante, Usuario revisor) {
        return new OrganizacionAdminDTO(
                o.getIdOrganizacion(),
                o.getNombre(),
                o.getTipo(),
                o.getDireccion(),
                o.getTelefono(),
                o.getEmail(),
                o.getDescripcion(),
                o.getHorarios(),
                o.getZonasCobertura(),
                o.getLogoUrl(),
                o.tieneDocumento(),
                o.getEstadoVerificacion(),
                o.getFechaRegistro(),
                o.getIdRepresentante(),
                representante == null ? null : representante.getNombre(),
                representante == null ? null : representante.getEmail(),
                o.getIdRevisor(),
                revisor == null ? null : revisor.getNombre(),
                o.getFechaRevision(),
                o.getNotaRevision());
    }
}
