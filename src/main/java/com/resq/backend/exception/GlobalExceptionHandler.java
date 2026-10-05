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
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final long maxBytesFoto;

    public GlobalExceptionHandler(@Value("${resq.fotos.max-bytes:5242880}") long maxBytesFoto) {
        this.maxBytesFoto = maxBytesFoto;
    }

    @ExceptionHandler(FotoInvalidaException.class)
    public ResponseEntity<ApiError> handleFotoInvalida(FotoInvalidaException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiError(ex.getStatus().value(), ex.getMessage(), Map.of("foto", ex.getMessage())));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTamanoExcedido(MaxUploadSizeExceededException ex) {
        String mensaje = "La fotografía supera el tamaño máximo permitido de " + (maxBytesFoto / (1024 * 1024)) + " MB";
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiError(HttpStatus.PAYLOAD_TOO_LARGE.value(), mensaje, Map.of("foto", mensaje)));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleParteFaltante(MissingServletRequestPartException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(), "Debes seleccionar una fotografía",
                        Map.of("foto", "obligatoria")));
    }

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