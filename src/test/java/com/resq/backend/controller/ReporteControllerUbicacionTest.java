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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ReporteController: asociacion de la ubicacion al reporte (HU-16)")
class ReporteControllerUbicacionTest {

    private ReporteRepository reporteRepository;
    private ReporteController controller;

    @BeforeEach
    void setUp() {
        reporteRepository = mock(ReporteRepository.class);
        controller = new ReporteController(reporteRepository);
    }

    private static Reporte reporteUbicado(Double latitud, Double longitud) {
        Reporte reporte = new Reporte();
        reporte.setIdUsuario(10L);
        reporte.setTipoCaso("PERDIDA");
        reporte.setDescripcion("Perro perdido cerca a la plaza");
        reporte.setEstado("PENDIENTE");
        reporte.setLatitud(latitud);
        reporte.setLongitud(longitud);
        return reporte;
    }

    private void simularGuardado() {
        when(reporteRepository.save(any(Reporte.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private Reporte reportePersistido() {
        ArgumentCaptor<Reporte> captor = ArgumentCaptor.forClass(Reporte.class);
        verify(reporteRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("asocia la ubicacion actual al reporte creado y la devuelve")
    void debeAsociarLaUbicacionActualAlReporteCreado() {
        simularGuardado();

        ResponseEntity<Reporte> respuesta =
                controller.crearReporte(reporteUbicado(-17.3895, -66.1568));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getLatitud()).isEqualTo(-17.3895);
        assertThat(respuesta.getBody().getLongitud()).isEqualTo(-66.1568);

        Reporte persistido = reportePersistido();
        assertThat(persistido.getLatitud()).isEqualTo(-17.3895);
        assertThat(persistido.getLongitud()).isEqualTo(-66.1568);
    }

    @Test
    @DisplayName("devuelve la ubicacion asociada al consultar el reporte por id")
    void debeDevolverLaUbicacionAlConsultarElReporte() {
        when(reporteRepository.findById(7L))
                .thenReturn(Optional.of(reporteUbicado(-17.3895, -66.1568)));

        ResponseEntity<Reporte> respuesta = controller.obtenerReportePorId(7L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getLatitud()).isEqualTo(-17.3895);
        assertThat(respuesta.getBody().getLongitud()).isEqualTo(-66.1568);
    }

    @Test
    @DisplayName("entrega las coordenadas de cada reporte para poder representarlos en el mapa")
    void debeEntregarLasCoordenadasParaElMapa() {
        when(reporteRepository.findAll()).thenReturn(List.of(
                reporteUbicado(-17.3895, -66.1568),
                reporteUbicado(null, null)));

        ResponseEntity<List<Reporte>> respuesta = controller.obtenerReportes();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(2);
        assertThat(respuesta.getBody())
                .extracting(Reporte::getLatitud, Reporte::getLongitud)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(-17.3895, -66.1568),
                        org.assertj.core.groups.Tuple.tuple(null, null));
    }

    @Test
    @DisplayName("reemplaza la ubicacion de un reporte existente al actualizarlo")
    void debeReemplazarLaUbicacionAlActualizarElReporte() {
        when(reporteRepository.findById(7L))
                .thenReturn(Optional.of(reporteUbicado(-17.3895, -66.1568)));
        simularGuardado();

        ResponseEntity<Reporte> respuesta =
                controller.actualizarReporte(7L, reporteUbicado(-17.4140, -66.1700));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getLatitud()).isEqualTo(-17.4140);
        assertThat(respuesta.getBody().getLongitud()).isEqualTo(-66.1700);

        Reporte persistido = reportePersistido();
        assertThat(persistido.getLatitud()).isEqualTo(-17.4140);
        assertThat(persistido.getLongitud()).isEqualTo(-66.1700);
    }

    @Test
    @DisplayName("borra la ubicacion asociada cuando el reporte se guarda sin ella")
    void debeBorrarLaUbicacionCuandoElReporteYaNoLaTiene() {
        when(reporteRepository.findById(7L))
                .thenReturn(Optional.of(reporteUbicado(-17.3895, -66.1568)));
        simularGuardado();

        ResponseEntity<Reporte> respuesta = controller.actualizarReporte(7L, reporteUbicado(null, null));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getLatitud()).isNull();
        assertThat(respuesta.getBody().getLongitud()).isNull();

        Reporte persistido = reportePersistido();
        assertThat(persistido.getLatitud()).isNull();
        assertThat(persistido.getLongitud()).isNull();
    }
}
