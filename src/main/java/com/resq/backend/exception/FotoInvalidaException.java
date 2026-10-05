package com.resq.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando la fotografía adjuntada no cumple las reglas de la HU-10
 * (archivo vacío, formato no permitido o tamaño excedido).
 */
public class FotoInvalidaException extends RuntimeException {

    private final HttpStatus status;

    public FotoInvalidaException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
