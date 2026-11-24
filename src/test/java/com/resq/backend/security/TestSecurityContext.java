package com.resq.backend.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public final class TestSecurityContext {

    private TestSecurityContext() {
    }

    public static void authenticateAs(long idUsuario, String rol) {
        var autenticacion = new UsernamePasswordAuthenticationToken(
                String.valueOf(idUsuario), null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
