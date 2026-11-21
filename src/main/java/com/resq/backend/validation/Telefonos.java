package com.resq.backend.validation;

/**
 * Contacto de quien reporta: reglas de un numero de telefono.
 *
 * Se aceptan espacios, guiones y parentesis al escribirlo ("7 123-4567",
 * "+591 71234567"), pero se guarda limpio: un "+" opcional y de 7 a 15 digitos.
 */
public final class Telefonos {

    /** Lo que se acepta al recibirlo, antes de limpiarlo. */
    public static final String PATRON_ENTRADA = "^[+0-9 ()\\-]{7,25}$";

    private static final String PATRON_LIMPIO = "^\\+?[0-9]{7,15}$";

    private Telefonos() {
    }

    /**
     * Devuelve el numero limpio, o null si viene vacio.
     *
     * @throws IllegalArgumentException si no parece un telefono.
     */
    public static String normalizar(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return null;
        }
        String limpio = telefono.trim().replaceAll("[\\s()\\-]", "");
        if (!limpio.matches(PATRON_LIMPIO)) {
            throw new IllegalArgumentException("El teléfono debe tener entre 7 y 15 dígitos");
        }
        return limpio;
    }
}
