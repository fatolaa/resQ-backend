package com.resq.backend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UsuarioAutenticado: identidad de quien hace la petición (HU-20)")
class UsuarioAutenticadoTest {

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("sin autenticación no hay usuario")
    void sinAutenticacion() {
        SecurityContextHolder.clearContext();

        assertThat(UsuarioAutenticado.idActual()).isEmpty();
        assertThat(UsuarioAutenticado.esAdmin()).isFalse();
    }

    @Test
    @DisplayName("una petición anónima no cuenta como usuario")
    void anonimo() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "clave", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(UsuarioAutenticado.idActual()).isEmpty();
    }

    @Test
    @DisplayName("el principal numérico del token es el id del usuario")
    void principalNumerico() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "42", null, List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))));

        assertThat(UsuarioAutenticado.idActual()).contains(42L);
        assertThat(UsuarioAutenticado.esAdmin()).isFalse();
    }

    @Test
    @DisplayName("un principal que no es un id numérico se ignora")
    void principalNoNumerico() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "alguien@correo.com", null, List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))));

        assertThat(UsuarioAutenticado.idActual()).isEmpty();
    }

    @Test
    @DisplayName("reconoce al administrador por su autoridad ROLE_ADMIN")
    void reconoceAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        assertThat(UsuarioAutenticado.esAdmin()).isTrue();
    }
}
