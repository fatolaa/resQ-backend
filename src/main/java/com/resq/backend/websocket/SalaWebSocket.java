package com.resq.backend.websocket;

/**
 * HU-38a: nombres de las salas (destinos STOMP) y su interpretacion. Una sala de
 * usuario entrega eventos personales (notificaciones, presencia); una de caso, el
 * chat de ese caso.
 */
public final class SalaWebSocket {

    public static final String DESTINO_USUARIOS = "/topic/usuarios/";
    public static final String DESTINO_CASOS = "/topic/casos/";

    private SalaWebSocket() {
    }

    public static String deUsuario(Long idUsuario) {
        return DESTINO_USUARIOS + idUsuario;
    }

    public static String deCaso(Long idCaso) {
        return DESTINO_CASOS + idCaso;
    }

    /** Id de usuario si el destino es una sala de usuario, o null si no lo es. */
    public static Long idUsuarioDe(String destino) {
        return idDePrefijo(destino, DESTINO_USUARIOS);
    }

    /** Id de caso si el destino es una sala de caso, o null si no lo es. */
    public static Long idCasoDe(String destino) {
        return idDePrefijo(destino, DESTINO_CASOS);
    }

    private static Long idDePrefijo(String destino, String prefijo) {
        if (destino == null || !destino.startsWith(prefijo)) {
            return null;
        }
        String resto = destino.substring(prefijo.length());
        if (resto.isEmpty() || resto.contains("/")) {
            return null;
        }
        try {
            return Long.parseLong(resto);
        } catch (NumberFormatException noEsId) {
            return null;
        }
    }
}
