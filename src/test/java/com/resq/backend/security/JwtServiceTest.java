package com.resq.backend.security;

import com.resq.backend.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService: emision y validacion del token (HU-23)")
class JwtServiceTest {

    private static final String SECRETO = "resq-clave-de-prueba-suficientemente-larga-para-hmac-256";
    private static final long EXPIRACION_MS = 60_000L;

    private static Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombre("Ana Admin");
        usuario.setEmail("admin@resq.co");
        usuario.setRol("ADMIN");
        return usuario;
    }

    @Test
    @DisplayName("el token generado devuelve el mismo id de usuario")
    void debeRecuperarElIdDelTokenGenerado() {
        JwtService servicio = new JwtService(SECRETO, EXPIRACION_MS);

        String token = servicio.generarToken(usuario(7L));

        assertThat(servicio.extraerIdUsuarioSiEsValido(token)).isEqualTo(7L);
    }

    @Test
    @DisplayName("un token firmado con otra clave no se acepta")
    void debeRechazarUnTokenFirmadoConOtraClave() {
        JwtService emisor = new JwtService(SECRETO, EXPIRACION_MS);
        JwtService verificador = new JwtService("otra-clave-distinta-y-larga-para-hmac-256-bits", EXPIRACION_MS);

        String token = emisor.generarToken(usuario(7L));

        assertThat(verificador.extraerIdUsuarioSiEsValido(token)).isNull();
    }

    @Test
    @DisplayName("un token manipulado no se acepta")
    void debeRechazarUnTokenManipulado() {
        JwtService servicio = new JwtService(SECRETO, EXPIRACION_MS);
        String token = servicio.generarToken(usuario(7L));

        String manipulado = token.substring(0, token.lastIndexOf('.') + 1) + "ZmF1c2U";

        assertThat(servicio.extraerIdUsuarioSiEsValido(manipulado)).isNull();
    }

    @Test
    @DisplayName("un token ya vencido no se acepta")
    void debeRechazarUnTokenVencido() {
        JwtService servicio = new JwtService(SECRETO, -1_000L);

        String token = servicio.generarToken(usuario(7L));

        assertThat(servicio.extraerIdUsuarioSiEsValido(token)).isNull();
    }

    @Test
    @DisplayName("un texto que no es un token devuelve null en lugar de propagar la excepcion")
    void debeDevolverNullAnteUnTextoQueNoEsToken() {
        JwtService servicio = new JwtService(SECRETO, EXPIRACION_MS);

        assertThat(servicio.extraerIdUsuarioSiEsValido("esto-no-es-un-token")).isNull();
    }

    @Test
    @DisplayName("el token no lleva el rol, para que un cambio de rol surta efecto de inmediato")
    void elTokenNoLlevaElRol() {
        JwtService servicio = new JwtService(SECRETO, EXPIRACION_MS);

        String token = servicio.generarToken(usuario(7L));

        assertThat(token).doesNotContain("ADMIN").doesNotContain("ADMINISTRADOR");
    }
}
