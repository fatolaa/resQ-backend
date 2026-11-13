package com.resq.backend.dto;

import java.util.List;

/**
 * Contacto de quien reporto un caso. Solo se entrega con sesion iniciada y solo si
 * quien reporto acepto mostrarlo en ese reporte.
 *
 * @param nombre   nombre de la cuenta que reporto.
 * @param insignia "VOLUNTARIO", "ORGANIZACION" o null para una cuenta comun.
 *                 Nunca el rol interno (no se muestra quien es administrador).
 * @param telefonos uno o dos numeros: el de la cuenta y/o uno solo para este caso.
 */
public record ContactoReporteDTO(String nombre, String insignia, List<Telefono> telefonos) {

    public static final String INSIGNIA_VOLUNTARIO = "VOLUNTARIO";
    public static final String INSIGNIA_ORGANIZACION = "ORGANIZACION";

    /**
     * @param numero numero limpio (con "+" opcional y solo digitos).
     * @param origen "CUENTA" (el de su perfil) o "CASO" (uno solo para este reporte).
     */
    public record Telefono(String numero, String origen) {
        public static final String CUENTA = "CUENTA";
        public static final String CASO = "CASO";
    }
}
