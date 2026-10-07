package com.resq.backend.dto;

/** HU-20: expresiones regulares del formulario de organización (el frontend usa las mismas). */
public final class ValidacionOrganizacion {

    /** Opcional "+", y solo dígitos, espacios, paréntesis y guiones; entre 7 y 15 dígitos. */
    public static final String TELEFONO = "^(?=(?:\\D*\\d){7,15}\\D*$)\\+?[\\d ()\\-]+$";

    /** Exige dominio con punto y extensión de al menos 2 letras (@Email solo acepta "a@b"). */
    public static final String EMAIL =
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9\\-]+)*\\.[A-Za-z]{2,}$";

    private ValidacionOrganizacion() {
    }
}
