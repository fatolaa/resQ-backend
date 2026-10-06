package com.resq.backend.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Organizacion: estado de verificación y gestión de casos (HU-20)")
class OrganizacionTest {

    @Test
    @DisplayName("al guardarse por primera vez queda pendiente de verificación, con fecha de registro")
    void nacePendienteDeVerificacion() {
        Organizacion organizacion = new Organizacion();

        organizacion.prePersist();

        assertThat(organizacion.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE);
        assertThat(organizacion.getFechaRegistro()).isNotNull();
    }

    @Test
    @DisplayName("prePersist no pisa un estado ya definido")
    void noPisaElEstado() {
        Organizacion organizacion = new Organizacion();
        organizacion.setEstadoVerificacion(EstadoVerificacion.VERIFICADA);

        organizacion.prePersist();

        assertThat(organizacion.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.VERIFICADA);
    }

    @Test
    @DisplayName("solo una organización VERIFICADA puede gestionar casos")
    void soloVerificadaGestionaCasos() {
        Organizacion organizacion = new Organizacion();

        organizacion.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);
        assertThat(organizacion.puedeGestionarCasos()).isFalse();

        organizacion.setEstadoVerificacion(EstadoVerificacion.RECHAZADA);
        assertThat(organizacion.puedeGestionarCasos()).isFalse();

        organizacion.setEstadoVerificacion(null);
        assertThat(organizacion.puedeGestionarCasos()).isFalse();

        organizacion.setEstadoVerificacion(EstadoVerificacion.VERIFICADA);
        assertThat(organizacion.puedeGestionarCasos()).isTrue();
    }

    @Test
    @DisplayName("el JSON incluye estadoVerificacion y puedeGestionarCasos para el frontend")
    void jsonIncluyeEstadoYPermiso() throws Exception {
        Organizacion organizacion = new Organizacion();
        organizacion.setNombre("Refugio Patitas");
        organizacion.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);

        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(organizacion);

        assertThat(json).contains("\"estadoVerificacion\":\"PENDIENTE_VERIFICACION\"")
                .contains("\"puedeGestionarCasos\":false");
    }

    @Test
    @DisplayName("los tipos permitidos son refugio, veterinaria y rescatista independiente")
    void tiposPermitidos() {
        assertThat(TipoOrganizacion.VALORES)
                .containsExactlyInAnyOrder("REFUGIO", "VETERINARIA", "RESCATISTA_INDEPENDIENTE");
    }
}
