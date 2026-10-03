package com.resq.backend.controller;

import com.resq.backend.entity.EstadoRevision;
import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ReporteController: los casos rechazados no son visibles para otros usuarios (HU-19)")
class ReporteVisibilidadRechazadosTest {

    private ReporteRepository reporteRepository;
    private ReporteController controller;

    private Reporte aprobado;
    private Reporte pendiente;
    private Reporte sinRevisar;
    private Reporte rechazado;

    @BeforeEach
    void setUp() {
        reporteRepository = mock(ReporteRepository.class);
        controller = new ReporteController(reporteRepository);

        aprobado = reporte(1L, EstadoRevision.APROBADO);
        pendiente = reporte(2L, EstadoRevision.PENDIENTE);
        sinRevisar = reporte(3L, null); // reporte anterior a HU-19
        rechazado = reporte(4L, EstadoRevision.RECHAZADO);
    }

    private static Reporte reporte(long id, String estadoRevision) {
        Reporte reporte = new Reporte();
        reporte.setIdReporte(id);
        reporte.setIdUsuario(10L);
        reporte.setTipoCaso("PERDIDA");
        reporte.setEstado("PENDIENTE");
        reporte.setEstadoRevision(estadoRevision);
        return reporte;
    }

    @Test
    @DisplayName("el listado general excluye los rechazados")
    void listadoGeneralExcluyeRechazados() {
        when(reporteRepository.findAll()).thenReturn(List.of(aprobado, pendiente, sinRevisar, rechazado));

        ResponseEntity<?> respuesta = controller.obtenerReportes(null);

        assertThat((List<Reporte>) respuesta.getBody()).containsExactly(aprobado, pendiente, sinRevisar);
    }

    @Test
    @DisplayName("el listado filtrado por estado también excluye los rechazados")
    void listadoFiltradoExcluyeRechazados() {
        when(reporteRepository.findByEstadoIn(List.of("PENDIENTE"))).thenReturn(List.of(aprobado, rechazado));

        ResponseEntity<?> respuesta = controller.obtenerReportes("PENDIENTE");

        assertThat((List<Reporte>) respuesta.getBody()).containsExactly(aprobado);
    }

    @Test
    @DisplayName("obtenerReportes() sin argumentos también excluye los rechazados")
    void listadoSinArgumentosExcluyeRechazados() {
        when(reporteRepository.findAll()).thenReturn(List.of(rechazado, aprobado));

        assertThat(controller.obtenerReportes().getBody()).containsExactly(aprobado);
    }

    @Test
    @DisplayName("el dueño sigue viendo sus reportes rechazados para conocer el motivo")
    void duenoVeSusRechazados() {
        when(reporteRepository.findByIdUsuario(10L)).thenReturn(List.of(aprobado, rechazado));

        assertThat(controller.obtenerReportesPorUsuario(10L).getBody()).containsExactly(aprobado, rechazado);
    }
}
