package com.resq.backend.exception;

import com.resq.backend.dto.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = ex.getBindingResult().getAllErrors().stream()
                .collect(Collectors.toMap(GlobalExceptionHandler::claveDelError,
                        GlobalExceptionHandler::mensajeDelError, (a, b) -> a));
        return ResponseEntity.badRequest()
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(), "Error de validación", errores));
    }

    private static String claveDelError(ObjectError error) {
        return error instanceof FieldError campo ? campo.getField() : error.getObjectName();
    }

    private static String mensajeDelError(ObjectError error) {
        return error.getDefaultMessage() == null ? "valor invalido" : error.getDefaultMessage();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(), "JSON inválido en el cuerpo de la petición", null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleViolacionIntegridad(DataIntegrityViolationException ex) {
        String causa = ex.getMostSpecificCause().getMessage();
        String mensaje = causa != null && causa.contains("Duplicate entry")
                ? "El email ya está registrado"
                : "Operación rechazada por registros asociados en la base de datos";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(HttpStatus.CONFLICT.value(), mensaje, null));
    }
}