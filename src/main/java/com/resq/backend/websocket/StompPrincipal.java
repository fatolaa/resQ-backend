package com.resq.backend.websocket;

import java.security.Principal;

/**
 * HU-38a: identidad del usuario dentro de la sesion STOMP. El nombre (getName) es el
 * id del usuario, que es lo que Spring usa para resolver los destinos de usuario.
 */
public class StompPrincipal implements Principal {

    private final Long idUsuario;
    private final String email;
    private final String rol;

    public StompPrincipal(Long idUsuario, String email, String rol) {
        this.idUsuario = idUsuario;
        this.email = email;
        this.rol = rol;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public String getEmail() {
        return email;
    }

    public String getRol() {
        return rol;
    }

    @Override
    public String getName() {
        return String.valueOf(idUsuario);
    }
}
