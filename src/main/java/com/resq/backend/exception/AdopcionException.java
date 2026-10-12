package com.resq.backend.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** HU-30: error de negocio al registrar o consultar una adopción. */
public class AdopcionException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> errores;

    public AdopcionException(HttpStatus status, String mensaje, Map<String, String> errores) {
        super(mensaje);
        this.status = status;
        this.errores = errores;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
