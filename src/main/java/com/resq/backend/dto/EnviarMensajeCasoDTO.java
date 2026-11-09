package com.resq.backend.dto;

import com.resq.backend.entity.MensajeCaso;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * HU-22: cuerpo para enviar un mensaje de texto o una ubicacion. Si vienen latitud y
 * longitud es una ubicacion (el contenido es un comentario opcional); si no, es texto y el
 * contenido es obligatorio. Las fotos van por su propio endpoint multipart.
 */
public record EnviarMensajeCasoDTO(
        @Size(max = MensajeCaso.CONTENIDO_MAX, message = "El mensaje no puede superar los 1000 caracteres")
        String contenido,
        @DecimalMin(value = "-90.0", message = "Latitud invalida")
        @DecimalMax(value = "90.0", message = "Latitud invalida")
        Double latitud,
        @DecimalMin(value = "-180.0", message = "Longitud invalida")
        @DecimalMax(value = "180.0", message = "Longitud invalida")
        Double longitud) {
}
