package com.resq.backend.exception;

import org.springframework.http.HttpStatus;
import java.util.Map;

/** HU-40: error de negocio al denunciar o bloquear en el chat. */
public class ChatException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> errores;

    public ChatException(HttpStatus status, String mensaje, Map<String, String> errores) {
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
