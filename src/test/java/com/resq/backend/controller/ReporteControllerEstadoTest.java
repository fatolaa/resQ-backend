package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.ReporteEstadoDTO;
import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ReporteController: cambio de estado administrativo (HU-24)")
class ReporteControllerEstadoTest {

    private ReporteRepository reporteRepository;
    private ReporteController controller;

    @BeforeEach
    void setUp() {
        reporteRepository = mock(ReporteRepository.class);
        controller = new ReporteController(reporteRepository);
    }

    private void givenReporteEn(String estado) {
        Reporte reporte = new Reporte();
        reporte.setIdUsuario(10L);
        reporte.setTipoCaso("PERDIDA");
        reporte.setDescripcion("Perro perdido cerca a la plaza");
        reporte.setEstado(estado);
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(reporte));
        when(reporteRepository.save(any(Reporte.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private ApiError cuerpoError(ResponseEntity<?> respuesta) {
        assertThat(respuesta.getBody()).isInstanceOf(ApiError.class);
        return (ApiError) respuesta.getBody();
    }

    @Test
    @DisplayName("lleva un caso pendiente a en proceso")
    void debeMoverPendienteAEnProceso() {
        givenReporteEn("PENDIENTE");

        ResponseEntity<?> respuesta = controller.cambiarEstado(1L, new ReporteEstadoDTO("EN_PROCESO"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(reporteRepository).save(any(Reporte.class));
    }

    @Test
    @DisplayName("lleva un caso en proceso a resuelto")
    void debeMoverEnProcesoAResuelto() {
        givenReporteEn("EN_PROCESO");

        ResponseEntity<?> respuesta = controller.cambiarEstado(1L, new ReporteEstadoDTO("RESUELTO"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("no deja resolver un caso que todavia no se atendio")
    void debeRechazarResolverUnCasoPendiente() {
        givenReporteEn("PENDIENTE");

        ResponseEntity<?> respuesta = controller.cambiarEstado(1L, new ReporteEstadoDTO("RESUELTO"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(cuerpoError(respuesta).message()).contains("no se puede pasar de PENDIENTE a RESUELTO");
        verify(reporteRepository, never()).save(any(Reporte.class));
    }

    @Test
    @DisplayName("permite reabrir un caso resuelto porque el animal sigue en riesgo")
    void debePermitirReabrirUnCasoResuelto() {
        givenReporteEn("RESUELTO");

        ResponseEntity<?> respuesta = controller.cambiarEstado(1L, new ReporteEstadoDTO("PENDIENTE"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("rechaza dejar el caso en el estado que ya tiene")
    void debeRechazarDejarElMismoEstado() {
        givenReporteEn("PENDIENTE");

        ResponseEntity<?> respuesta = controller.cambiarEstado(1L, new ReporteEstadoDTO("PENDIENTE"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(cuerpoError(respuesta).message()).contains("ya se encuentra en PENDIENTE");
        verify(reporteRepository, never()).save(any(Reporte.class));
    }

    @Test
    @DisplayName("rechaza un estado que no existe")
    void debeRechazarUnEstadoInexistente() {
        givenReporteEn("PENDIENTE");

        ResponseEntity<?> respuesta = controller.cambiarEstado(1L, new ReporteEstadoDTO("PERDIDO"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(reporteRepository, never()).save(any(Reporte.class));
    }

    @Test
    @DisplayName("responde 404 cuando el caso a cambiar no existe")
    void debeResponderNoEncontrado() {
        when(reporteRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> respuesta = controller.cambiarEstado(99L, new ReporteEstadoDTO("EN_PROCESO"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(reporteRepository, never()).save(any(Reporte.class));
    }
}