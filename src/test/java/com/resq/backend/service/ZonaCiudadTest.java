package com.resq.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ZonaCiudad: zona de un caso a partir de sus coordenadas (HU-33)")
class ZonaCiudadTest {

    @Test
    @DisplayName("sin coordenadas el caso queda SIN_UBICACION")
    void sinCoordenadas() {
        assertThat(ZonaCiudad.de(null, null)).isEqualTo(ZonaCiudad.SIN_UBICACION);
        assertThat(ZonaCiudad.de(-17.39, null)).isEqualTo(ZonaCiudad.SIN_UBICACION);
        assertThat(ZonaCiudad.de(null, -66.15)).isEqualTo(ZonaCiudad.SIN_UBICACION);
    }

    @Test
    @DisplayName("la Plaza 14 de Septiembre y el Parque Urbano Central son CENTRO")
    void centro() {
        assertThat(ZonaCiudad.de(-17.3892, -66.1567)).isEqualTo(ZonaCiudad.CENTRO);
        assertThat(ZonaCiudad.de(-17.3913, -66.1637)).isEqualTo(ZonaCiudad.CENTRO);
    }

    @Test
    @DisplayName("fuera del centro la zona sale del rumbo desde la plaza")
    void puntosCardinales() {
        assertThat(ZonaCiudad.de(-17.365, -66.160)).isEqualTo(ZonaCiudad.NORTE);   // Cala Cala
        assertThat(ZonaCiudad.de(-17.420, -66.155)).isEqualTo(ZonaCiudad.SUR);
        assertThat(ZonaCiudad.de(-17.390, -66.120)).isEqualTo(ZonaCiudad.ESTE);
        assertThat(ZonaCiudad.de(-17.390, -66.190)).isEqualTo(ZonaCiudad.OESTE);
    }

    @Test
    @DisplayName("mas alla del radio de la ciudad es AFUERAS (Quillacollo, Sacaba)")
    void afueras() {
        assertThat(ZonaCiudad.de(-17.392, -66.278)).isEqualTo(ZonaCiudad.AFUERAS);
        assertThat(ZonaCiudad.de(-17.398, -66.040)).isEqualTo(ZonaCiudad.AFUERAS);
    }

    @Test
    @DisplayName("los limites de cuadrante caen en un unico punto cardinal")
    void limitesDeCuadrante() {
        assertThat(ZonaCiudad.puntoCardinal(0)).isEqualTo(ZonaCiudad.NORTE);
        assertThat(ZonaCiudad.puntoCardinal(44.9)).isEqualTo(ZonaCiudad.NORTE);
        assertThat(ZonaCiudad.puntoCardinal(45)).isEqualTo(ZonaCiudad.ESTE);
        assertThat(ZonaCiudad.puntoCardinal(135)).isEqualTo(ZonaCiudad.SUR);
        assertThat(ZonaCiudad.puntoCardinal(225)).isEqualTo(ZonaCiudad.OESTE);
        assertThat(ZonaCiudad.puntoCardinal(315)).isEqualTo(ZonaCiudad.NORTE);
        assertThat(ZonaCiudad.puntoCardinal(359.9)).isEqualTo(ZonaCiudad.NORTE);
    }

    @Test
    @DisplayName("la distancia de Haversine da valores razonables dentro de la ciudad")
    void distancia() {
        // Un grado de latitud mide ~111 km en cualquier parte del globo.
        assertThat(ZonaCiudad.distanciaKm(-17.0, -66.0, -18.0, -66.0)).isCloseTo(111.2, within(0.5));
        assertThat(ZonaCiudad.distanciaKm(-17.39, -66.15, -17.39, -66.15)).isZero();
    }
}
