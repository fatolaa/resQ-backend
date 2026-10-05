package com.resq.backend.config;

import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtAuthenticationFilter;
import com.resq.backend.security.JwtService;
import com.resq.backend.security.RestAuthenticationEntryPoint;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ROL_ADMIN = "ADMIN";
    private static final String ROL_VOLUNTARIO = "VOLUNTARIO";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public static JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
                                                                  UsuarioRepository usuarioRepository) {
        return new JwtAuthenticationFilter(jwtService, usuarioRepository);
    }

    /**
     * El filtro ya se encadena dentro de la cadena de seguridad. Sin este registro
     * desactivado Spring Boot lo ademas al filtro del contenedor y el token se
     * validaria dos veces por peticion.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> registroDesactivadoJwtFilter(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registro =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // El orden importa: en Spring Security gana el primer patron que
                        // coincide, asi que las reglas especificas van antes de las generales.

                        // Publicas: el alta de cuentas (HU-01) y el login no pueden exigir sesion.
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/login").permitAll()

                        // HU-10: la foto se muestra con un <img> en el mapa del landing, que es
                        // publico. Un <img> no pasa por el interceptor de Angular, asi que nunca
                        // manda Authorization: si esto exigiera sesion, la imagen no se veria.
                        // La subida si va por HttpClient, ahi si llega el token.
                        .requestMatchers(HttpMethod.GET, "/api/reportes/fotos/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reportes/fotos").authenticated()

                        // HU-19: revisar solicitudes es de voluntarios y administradores.
                        // Esta regla tiene que ir ANTES del permitAll de reportes de abajo:
                        // /api/reportes/revision/pendientes es un GET bajo /api/reportes/**,
                        // y sin esta regla la cola de revision quedaria abierta a cualquiera.
                        .requestMatchers(HttpMethod.GET, "/api/reportes/revision/**")
                        .hasAnyRole(ROL_VOLUNTARIO, ROL_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/reportes/revision/**")
                        .hasAnyRole(ROL_VOLUNTARIO, ROL_ADMIN)

                        // El mapa del landing muestra casos a quien no ha iniciado sesion.
                        .requestMatchers(HttpMethod.GET, "/api/reportes/**").permitAll()

                        // HU-19: las notificaciones son de un usuario concreto, nunca publicas.
                        .requestMatchers("/api/notificaciones/**").authenticated()

                        // HU-17: el propio usuario se registra como voluntario. No es una funcion
                        // administrativa, asi que se permite a cualquier usuario autenticado y el
                        // control de que sea su propia cuenta lo hace UsuarioController.
                        .requestMatchers(HttpMethod.PATCH, "/api/usuarios/*/voluntario").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/usuarios/*/voluntario/**").authenticated()

                        // HU-23: gestionar cuentas es exclusivo del administrador.
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole(ROL_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/**").hasRole(ROL_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/usuarios/**").hasRole(ROL_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/**").hasRole(ROL_ADMIN)

                        // Ver un usuario concreto: lo puede pedir su propietario o el administrador.
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/**").authenticated()

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
