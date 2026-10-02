package com.resq.backend.security;

import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * HU-23: autentica la peticion a partir del header Authorization: Bearer <token>.
 *
 * El rol se lee siempre de la base de datos, nunca del token, para que el panel de
 * administracion surta efecto en el momento en que el administrador guarda el cambio.
 *
 * No es un @Component: se declara como bean dentro de SecurityConfig para que solo
 * exista cuando la cadena de seguridad esta activa, y no rompa los @WebMvcTest de
 * los demas controladores.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";
    private static final String PREFIJO_ROL = "ROLE_";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest peticion,
                                    @NonNull HttpServletResponse respuesta,
                                    @NonNull FilterChain cadena) throws ServletException, IOException {
        autenticarSiHayToken(peticion);
        cadena.doFilter(peticion, respuesta);
    }

    private void autenticarSiHayToken(HttpServletRequest peticion) {
        String cabecera = peticion.getHeader("Authorization");
        if (cabecera == null || !cabecera.startsWith(PREFIJO_BEARER)) {
            return;
        }

        String token = cabecera.substring(PREFIJO_BEARER.length());
        Long idUsuario = jwtService.extraerIdUsuarioSiEsValido(token);
        if (idUsuario == null) {
            SecurityContextHolder.clearContext();
            return;
        }

        usuarioRepository.findById(idUsuario).ifPresent(usuario -> registrarEnContexto(usuario, peticion));
    }

    private void registrarEnContexto(Usuario usuario, HttpServletRequest peticion) {
        var autoridad = new SimpleGrantedAuthority(PREFIJO_ROL + usuario.getRol().toUpperCase());
        // El principal es el id para que los controladores resuelvan la cuenta llamante;
        // el correo queda en los detalles, que es donde aparece en los logs.
        var autenticacion = new UsernamePasswordAuthenticationToken(
                String.valueOf(usuario.getIdUsuario()), null, List.of(autoridad));
        autenticacion.setDetails(usuario.getEmail());
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
