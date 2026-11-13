package com.resq.backend.dto;

import com.resq.backend.entity.Organizacion;

/**
 * Rediseño: lo que cualquiera puede ver de una organizacion verificada (Inicio y
 * detalle del caso). No incluye el representante ni los datos de contacto, que
 * siguen en la ficha con sesion (HU-29).
 */
public record OrganizacionPublicaDTO(
        Long idOrganizacion,
        String nombre,
        String tipo,
        String logoUrl,
        String zonasCobertura) {

    public static OrganizacionPublicaDTO de(Organizacion o) {
        return new OrganizacionPublicaDTO(o.getIdOrganizacion(), o.getNombre(), o.getTipo(), o.getLogoUrl(),
                o.getZonasCobertura());
    }
}
