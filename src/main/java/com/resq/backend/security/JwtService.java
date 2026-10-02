package com.resq.backend.security;

import com.resq.backend.entity.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * HU-23: emite y valida el token JWT que identifica al usuario en cada peticion.
 *
 * El token solo lleva el id del usuario. El rol NO viaja en el token a proposito:
 * se relee de la base de datos en cada peticion para que un cambio de rol hecho por
 * un administrador surta efecto de inmediato y no espere a que caduque el token.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiration-ms}") long expiracionMs) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(Usuario usuario) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getIdUsuario()))
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(clave)
                .compact();
    }

    public Long extraerIdUsuario(String token) {
        return Long.parseLong(Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject());
    }

    /** Extrae el id del token sin propagar la excepcion si el token es invalido o vencio. */
    public Long extraerIdUsuarioSiEsValido(String token) {
        try {
            return extraerIdUsuario(token);
        } catch (RuntimeException tokenInvalido) {
            return null;
        }
    }
}
