package com.resq.backend.dto;

import com.resq.backend.entity.Organizacion;

/**
 * HU-20: respuesta al registrar. confirmacionEnviada indica si el correo de
 * confirmación salió; si falla, el registro igualmente queda guardado.
 */
public record OrganizacionRegistradaDTO(Organizacion organizacion, boolean confirmacionEnviada) {
}
