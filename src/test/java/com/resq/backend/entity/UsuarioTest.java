package com.resq.backend.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Usuario: fecha de registro (HU-34)")
class UsuarioTest {

    @Test
    @DisplayName("al guardarse por primera vez queda con fecha de registro")
    void naceConFechaDeRegistro() {
        Usuario usuario = new Usuario();

        usuario.prePersist();

        assertThat(usuario.getFechaRegistro()).isNotNull();
    }

    @Test
    @DisplayName("prePersist no pisa una fecha ya definida")
    void noPisaLaFecha() {
        LocalDateTime fecha = LocalDateTime.of(2025, 3, 1, 10, 0);
        Usuario usuario = new Usuario();
        usuario.setFechaRegistro(fecha);

        usuario.prePersist();

        assertThat(usuario.getFechaRegistro()).isEqualTo(fecha);
    }
}
