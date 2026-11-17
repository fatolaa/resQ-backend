package com.resq.backend.dto;

import com.resq.backend.entity.SeguimientoCaso;

import java.time.LocalDateTime;

/** Rediseño: una novedad del caso tal como la ve cualquiera (sin ids internos). */
public record NovedadCasoDTO(LocalDateTime fechaCreacion, String nota, String fotoUrl) {

    public static NovedadCasoDTO de(SeguimientoCaso s) {
        return new NovedadCasoDTO(s.getFechaCreacion(), s.getNota(), s.getFotoUrl());
    }
}
