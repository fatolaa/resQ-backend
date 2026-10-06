package com.resq.backend.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** HU-20: error de negocio al registrar o consultar una organización. */
public class OrganizacionException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> errores;

    public OrganizacionException(HttpStatus status, String mensaje, Map<String, String> errores) {
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
