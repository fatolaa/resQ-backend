package com.resq.backend.dto;

/**
 * HU-19: decisión de un voluntario sobre una solicitud.
 * decision: APROBAR o RECHAZAR. nota: obligatoria al rechazar, opcional al aprobar.
 */
public record DecisionRevisionDTO(Long idRevisor, String decision, String nota) {
}
