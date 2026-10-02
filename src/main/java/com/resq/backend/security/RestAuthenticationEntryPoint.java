package com.resq.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resq.backend.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * HU-23: responde 401 cuando la peticion no trae una sesion valida.
 *
 * Sin este punto de entrada Spring Security responde 403 tambien a quien no ha
 * iniciado sesion, y el frontend no puede distinguir "inicia sesion" de
 * "no tienes permisos para esto".
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest peticion,
                         HttpServletResponse respuesta,
                         AuthenticationException excepcion) throws IOException {
        respuesta.setStatus(HttpStatus.UNAUTHORIZED.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(respuesta.getOutputStream(),
                new ApiError(HttpStatus.UNAUTHORIZED.value(),
                        "Debes iniciar sesion para acceder a esta operacion", null));
    }
}
