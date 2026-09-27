package com.resq.backend.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Reporte: validacion de coordenadas (HU-14)")
class ReporteValidacionTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    private Reporte reporte;

    @BeforeAll
    static void iniciarValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @BeforeEach
    void setUp() {
        reporte = new Reporte();
        reporte.setIdUsuario(10L);
        reporte.setTipoCaso("PERDIDA");
        reporte.setDescripcion("Perro perdido cerca a la plaza");
        reporte.setEstado("PENDIENTE");
    }

    private Set<ConstraintViolation<Reporte>> validar() {
        return validator.validate(reporte);
    }

    private boolean viola(String campo) {
        return validar().stream().anyMatch(violacion -> violacion.getPropertyPath().toString().equals(campo));
    }

    @Test
    @DisplayName("acepta las coordenadas de Cochabamba")
    void debeAceptarCoordenadasDeCochabamba() {
        reporte.setLatitud(-17.3895);
        reporte.setLongitud(-66.1568);

        assertThat(validar()).isEmpty();
    }

    @Test
    @DisplayName("acepta un reporte sin coordenadas porque la ubicacion es opcional")
    void debeAceptarReporteSinCoordenadas() {
        reporte.setLatitud(null);
        reporte.setLongitud(null);

        assertThat(validar()).isEmpty();
    }

    @Test
    @DisplayName("rechaza una latitud fuera del rango de -90 a 90")
    void debeRechazarLatitudFueraDeRango() {
        reporte.setLatitud(95.0);
        reporte.setLongitud(-66.1568);

        assertThat(viola("latitud")).isTrue();
        assertThat(viola("longitud")).isFalse();
    }

    @Test
    @DisplayName("rechaza una longitud fuera del rango de -180 a 180")
    void debeRechazarLongitudFueraDeRango() {
        reporte.setLatitud(-17.3895);
        reporte.setLongitud(-200.0);

        assertThat(viola("longitud")).isTrue();
        assertThat(viola("latitud")).isFalse();
    }

    @Test
    @DisplayName("acepta los limites exactos del rango")
    void debeAceptarLosLimitesDelRango() {
        reporte.setLatitud(-90.0);
        reporte.setLongitud(-180.0);

        assertThat(validar()).isEmpty();
    }
}
