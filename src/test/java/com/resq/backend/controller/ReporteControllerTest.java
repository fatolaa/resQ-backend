package com.resq.backend.controller;

import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del ReporteController enfocadas en el manejo de las coordenadas.
 *
 * El controller es una clase_plana con inyeccion por constructor, asi que se
 * instancia a mano con un repositorio mockeado. No hace falta levantar el
 * contexto de Spring ni conectar a la base de datos.
 */
@DisplayName("ReporteController: coordenadas del reporte (HU-14)")
class ReporteControllerTest {

    private ReporteRepository reporteRepository;
    private ReporteController controller;

    @BeforeEach
    void setUp() {
        reporteRepository = mock(ReporteRepository.class);
        controller = new ReporteController(reporteRepository);
    }

    private static Reporte reporteCon(Double latitud, Double longitud) {
        Reporte reporte = new Reporte();
        reporte.setIdUsuario(10L);
        reporte.setTipoCaso("PERDIDA");
        reporte.setDescripcion("Perro perdido cerca a la plaza");
        reporte.setEstado("PENDIENTE");
        reporte.setLatitud(latitud);
        reporte.setLongitud(longitud);
        return reporte;
    }

    private Reporte reporteGuardado() {
        ArgumentCaptor<Reporte> captor = ArgumentCaptor.forClass(Reporte.class);
        verify(reporteRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("guarda la latitud y la longitud al crear un reporte")
    void debeGuardarLatitudYLongitudAlCrearElReporte() {
        when(reporteRepository.save(any(Reporte.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Reporte entrada = reporteCon(-17.3895, -66.1568);

        ResponseEntity<Reporte> respuesta = controller.crearReporte(entrada);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getLatitud()).isEqualTo(-17.3895);
        assertThat(respuesta.getBody().getLongitud()).isEqualTo(-66.1568);

        Reporte guardado = reporteGuardado();
        assertThat(guardado.getLatitud()).isEqualTo(-17.3895);
        assertThat(guardado.getLongitud()).isEqualTo(-66.1568);
    }

    @Test
    @DisplayName("acepta un reporte sin coordenadas porque la ubicacion es opcional")
    void debeAceptarReporteSinCoordenadas() {
        when(reporteRepository.save(any(Reporte.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        ResponseEntity<Reporte> respuesta = controller.crearReporte(reporteCon(null, null));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody().getLatitud()).isNull();
        assertThat(respuesta.getBody().getLongitud()).isNull();
    }

    @Test
    @DisplayName("devuelve los reportes con sus coordenadas")
    void debeListarLosReportesConSusCoordenadas() {
        List<Reporte> esperados = List.of(
                reporteCon(-17.3895, -66.1568),
                reporteCon(-17.3540, -66.1480),
                reporteCon(null, null));
        when(reporteRepository.findAll()).thenReturn(esperados);

        ResponseEntity<List<Reporte>> respuesta = controller.obtenerReportes();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(3);
        assertThat(respuesta.getBody())
                .extracting(Reporte::getLatitud)
                .containsExactly(-17.3895, -17.3540, null);
    }

    @Test
    @DisplayName("actualiza la latitud y la longitud de un reporte existente")
    void debeActualizarLatitudYLongitud() {
        Reporte existente = reporteCon(-17.3895, -66.1568);
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(reporteRepository.save(any(Reporte.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Reporte cambios = reporteCon(-17.4140, -66.1700);
        cambios.setEstado("EN_PROCESO");

        ResponseEntity<Reporte> respuesta = controller.actualizarReporte(1L, cambios);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getLatitud()).isEqualTo(-17.4140);
        assertThat(respuesta.getBody().getLongitud()).isEqualTo(-66.1700);

        Reporte guardado = reporteGuardado();
        assertThat(guardado.getLatitud()).isEqualTo(-17.4140);
        assertThat(guardado.getLongitud()).isEqualTo(-66.1700);
        assertThat(guardado.getEstado()).isEqualTo("EN_PROCESO");
    }

    @Test
    @DisplayName("responde 404 al actualizar un reporte que no existe")
    void debeDevolverNoEncontradoAlActualizarUnReporteInexistente() {
        when(reporteRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<Reporte> respuesta = controller.actualizarReporte(99L, reporteCon(-17.39, -66.15));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(reporteRepository, never()).save(any(Reporte.class));
    }
}
