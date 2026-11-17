package com.resq.backend.dto;

import com.resq.backend.entity.NecesidadCaso;
import com.resq.backend.entity.Usuario;

/**
 * Rediseño: una necesidad del caso como la ve cualquiera.
 *
 * @param nombreAyudante solo el primer nombre de quien se ofrecio o la cubre.
 */
public record NecesidadCasoDTO(Long idNecesidad, String tipo, String estado, Long idAyudante, String nombreAyudante) {

    public static NecesidadCasoDTO de(NecesidadCaso n, Usuario ayudante) {
        return new NecesidadCasoDTO(n.getIdNecesidad(), n.getTipo(), n.getEstado(), n.getIdAyudante(),
                primerNombre(ayudante));
    }

    static String primerNombre(Usuario usuario) {
        if (usuario == null || usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            return null;
        }
        return usuario.getNombre().trim().split("\\s+")[0];
    }
}
