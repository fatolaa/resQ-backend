package com.resq.backend.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * HU-20: identifica al usuario que hace la petición.
 *
 * Mientras el backend no valide tokens (HU-23 aún sin integrar) no hay usuario
 * autenticado y los controladores usan el id que manda el cliente, igual que el resto
 * del proyecto. Cuando la cadena de seguridad autentica con JWT, el principal es el id
 * del usuario (String) y este helper lo devuelve para que NO se confíe en el cliente.
 */
public final class UsuarioAutenticado {

    private UsuarioAutenticado() {
    }

    /** Id del usuario autenticado por token, o vacío si la petición es anónima. */
    public static Optional<Long> idActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || autenticacion instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(String.valueOf(autenticacion.getPrincipal())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static boolean esAdmin() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null) {
            return false;
        }
        for (GrantedAuthority autoridad : autenticacion.getAuthorities()) {
            if ("ROLE_ADMIN".equals(autoridad.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
