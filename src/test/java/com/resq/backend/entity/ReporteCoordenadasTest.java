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

@DisplayName("Reporte: coherencia de las coordenadas (HU-16)")
class ReporteCoordenadasTest {

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

    private boolean tieneErrorDeCoherencia() {
        return validar().stream()
                .anyMatch(violacion -> violacion.getMessage()
                        .equals("latitud y longitud deben enviarse juntas"));
    }

    @Test
    @DisplayName("acepta un reporte con la latitud y la longitud de la ubicacion actual")
    void debeAceptarLaUbicacionActualCompleta() {
        reporte.setLatitud(-17.389500);
        reporte.setLongitud(-66.156800);

        assertThat(validar()).isEmpty();
    }

    @Test
    @DisplayName("acepta un reporte sin ubicacion porque la ubicacion es opcional")
    void debeAceptarReporteSinUbicacion() {
        assertThat(validar()).isEmpty();
        assertThat(tieneErrorDeCoherencia()).isFalse();
    }

    @Test
    @DisplayName("rechaza una latitud sin longitud porque la ubicacion quedaria incompleta")
    void debeRechazarLatitudSinLongitud() {
        reporte.setLatitud(-17.389500);

        assertThat(tieneErrorDeCoherencia()).isTrue();
    }

    @Test
    @DisplayName("rechaza una longitud sin latitud porque la ubicacion quedaria incompleta")
    void debeRechazarLongitudSinLatitud() {
        reporte.setLongitud(-66.156800);

        assertThat(tieneErrorDeCoherencia()).isTrue();
    }

    @Test
    @DisplayName("acepta el punto de referencia 0,0 porque es una coordenada valida")
    void debeAceptarCoordenadasEnCero() {
        reporte.setLatitud(0.0);
        reporte.setLongitud(0.0);

        assertThat(validar()).isEmpty();
    }

    @Test
    @DisplayName("rechaza coordenadas no finitas que se escapan de la validacion de rango")
    void debeRechazarCoordenadasNoFinitas() {
        reporte.setLatitud(Double.NaN);
        reporte.setLongitud(-66.156800);

        assertThat(tieneErrorDeCoherencia()).isTrue();
    }

    @Test
    @DisplayName("rechaza una longitud infinita")
    void debeRechazarLongitudInfinita() {
        reporte.setLatitud(-17.389500);
        reporte.setLongitud(Double.POSITIVE_INFINITY);

        assertThat(tieneErrorDeCoherencia()).isTrue();
    }

    @Test
    @DisplayName("sigue rechazando una latitud fuera de rango aunque venga con longitud")
    void debeSeguirRechazandoLatitudFueraDeRango() {
        reporte.setLatitud(95.0);
        reporte.setLongitud(-66.156800);

        Set<ConstraintViolation<Reporte>> violaciones = validar();

        assertThat(violaciones)
                .anyMatch(violacion -> violacion.getPropertyPath().toString().equals("latitud"));
    }
}
