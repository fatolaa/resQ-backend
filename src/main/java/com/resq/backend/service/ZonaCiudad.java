package com.resq.backend.service;

import java.util.List;

/**
 * HU-33: zona de la ciudad en la que cae un caso, para agruparlos en las metricas.
 *
 * Los reportes solo guardan latitud y longitud (HU-16), no un barrio. En vez de
 * mantener un mapa de barrios, la zona se calcula respecto del centro de Cochabamba
 * (la Plaza 14 de Septiembre, el mismo centro que usa el mapa del frontend):
 * <ul>
 *   <li>CENTRO: a {@value #RADIO_CENTRO_KM} km o menos del centro.</li>
 *   <li>NORTE, SUR, ESTE, OESTE: segun el rumbo desde el centro, hasta
 *       {@value #RADIO_CIUDAD_KM} km.</li>
 *   <li>AFUERAS: mas lejos que eso (Quillacollo, Sacaba, otras ciudades).</li>
 *   <li>SIN_UBICACION: el reporte no tiene coordenadas.</li>
 * </ul>
 */
public final class ZonaCiudad {

    public static final String CENTRO = "CENTRO";
    public static final String NORTE = "NORTE";
    public static final String SUR = "SUR";
    public static final String ESTE = "ESTE";
    public static final String OESTE = "OESTE";
    public static final String AFUERAS = "AFUERAS";
    public static final String SIN_UBICACION = "SIN_UBICACION";

    /** Orden fijo de las filas de la tabla de zonas. */
    public static final List<String> ZONAS = List.of(CENTRO, NORTE, SUR, ESTE, OESTE, AFUERAS);

    static final double LATITUD_CENTRO = -17.3895;
    static final double LONGITUD_CENTRO = -66.1568;
    static final double RADIO_CENTRO_KM = 1.5;
    static final double RADIO_CIUDAD_KM = 12.0;

    private static final double RADIO_TIERRA_KM = 6371.0;

    private ZonaCiudad() {
    }

    public static String de(Double latitud, Double longitud) {
        if (latitud == null || longitud == null) {
            return SIN_UBICACION;
        }
        double distancia = distanciaKm(LATITUD_CENTRO, LONGITUD_CENTRO, latitud, longitud);
        if (distancia <= RADIO_CENTRO_KM) {
            return CENTRO;
        }
        if (distancia > RADIO_CIUDAD_KM) {
            return AFUERAS;
        }
        return puntoCardinal(rumbo(LATITUD_CENTRO, LONGITUD_CENTRO, latitud, longitud));
    }

    /** Rumbo en grados (0 = norte, 90 = este) → el punto cardinal de su cuadrante de 90 grados. */
    static String puntoCardinal(double rumbo) {
        if (rumbo >= 315 || rumbo < 45) {
            return NORTE;
        }
        if (rumbo < 135) {
            return ESTE;
        }
        return rumbo < 225 ? SUR : OESTE;
    }

    /** Distancia de Haversine, suficiente para distancias dentro de una ciudad. */
    static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RADIO_TIERRA_KM * Math.asin(Math.sqrt(a));
    }

    /** Rumbo inicial de 1 a 2, normalizado a [0, 360). */
    static double rumbo(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLon = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dLon) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }
}
