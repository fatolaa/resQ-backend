package com.resq.backend.entity;

import java.util.Set;

/** HU-20: tipos de organización que pueden registrarse. */
public final class TipoOrganizacion {

    public static final String REFUGIO = "REFUGIO";
    public static final String VETERINARIA = "VETERINARIA";
    public static final String RESCATISTA_INDEPENDIENTE = "RESCATISTA_INDEPENDIENTE";

    public static final Set<String> VALORES = Set.of(REFUGIO, VETERINARIA, RESCATISTA_INDEPENDIENTE);

    private TipoOrganizacion() {
    }
}
