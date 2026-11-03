package com.resq.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * HU-32: cuerpo de aprobar/rechazar una organizacion. Al rechazar, la nota (el motivo) es
 * obligatoria; al aprobar es opcional. La obligatoriedad la valida el servicio.
 */
public record DecisionOrganizacionRequestDTO(
        @Size(max = 500, message = "La nota no puede superar los 500 caracteres")
        String nota) {
}
