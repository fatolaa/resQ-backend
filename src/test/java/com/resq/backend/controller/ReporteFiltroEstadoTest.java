package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ReporteController: filtro de casos por estado (HU-15)")
class ReporteFiltroEstadoTest {

    private ReporteRepository reporteRepository;
    private ReporteController controller;

    @BeforeEach
    void setUp() {
        reporteRepository = mock(ReporteRepository.class);
        controller = new ReporteController(reporteRepository);
    }

    private static Reporte reporteEn(String estado) {
        Reporte reporte = new Reporte();
        reporte.setIdUsuario(10L);
        reporte.setTipoCaso("PERDIDA");
        reporte.setDescripcion("Caso de prueba");
        reporte.setEstado(estado);
        return reporte;
    }

    private static List<Reporte> catalogo() {
        return List.of(
                reporteEn("PENDIENTE"),
                reporteEn("PENDIENTE"),
                reporteEn("EN_PROCESO"),
                reporteEn("RESUELTO"),
                reporteEn("CANCELADO"));
    }

    private void simularConsultas() {
        when(reporteRepository.findAll()).thenReturn(catalogo());
        when(reporteRepository.findByEstadoIn(anyList()))
                .thenAnswer(invocacion -> {
                    List<String> estados = invocacion.getArgument(0);
                    return catalogo().stream()
                            .filter(reporte -> estados.contains(reporte.getEstado()))
                            .toList();
                });
    }

    @SuppressWarnings("unchecked")
    private List<Reporte> cuerpo(ResponseEntity<?> respuesta) {
        assertThat(respuesta.getBody()).isInstanceOf(List.class);
        return (List<Reporte>) respuesta.getBody();
    }

    @Test
    @DisplayName("muestra todos los casos cuando no se envia ningun filtro")
    void debeMostrarTodosLosCasosSinFiltro() {
        simularConsultas();

        ResponseEntity<?> respuesta = controller.obtenerReportes(null);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cuerpo(respuesta)).hasSize(5);
        verify(reporteRepository).findAll();
        verify(reporteRepository, never()).findByEstadoIn(anyList());
    }

    @Test
    @DisplayName("muestra todos los casos cuando el filtro viene vacio")
    void debeMostrarTodosLosCasosConFiltroVacio() {
        simularConsultas();

        ResponseEntity<?> respuesta = controller.obtenerReportes("   ");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cuerpo(respuesta)).hasSize(5);
        verify(reporteRepository).findAll();
        verify(reporteRepository, never()).findByEstadoIn(anyList());
    }

    @Test
    @DisplayName("filtra los casos pendientes que son los urgentes")
    void debeFiltrarCasosUrgentes() {
        simularConsultas();

        ResponseEntity<?> respuesta = controller.obtenerReportes("PENDIENTE");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cuerpo(respuesta)).isNotEmpty();
        assertThat(cuerpo(respuesta)).allMatch(reporte -> "PENDIENTE".equals(reporte.getEstado()));
        verify(reporteRepository).findByEstadoIn(List.of("PENDIENTE"));
    }

    @Test
    @DisplayName("filtra los casos activos que son los pendientes y en proceso")
    void debeFiltrarCasosActivos() {
        simularConsultas();

        ResponseEntity<?> respuesta = controller.obtenerReportes("PENDIENTE,EN_PROCESO");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cuerpo(respuesta))
                .extracting(Reporte::getEstado)
                .containsExactlyInAnyOrder("PENDIENTE", "PENDIENTE", "EN_PROCESO");
        verify(reporteRepository).findByEstadoIn(List.of("PENDIENTE", "EN_PROCESO"));
    }

    @Test
    @DisplayName("filtra los casos resueltos")
    void debeFiltrarCasosResueltos() {
        simularConsultas();

        ResponseEntity<?> respuesta = controller.obtenerReportes("RESUELTO");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cuerpo(respuesta))
                .extracting(Reporte::getEstado)
                .containsExactly("RESUELTO");
        verify(reporteRepository).findByEstadoIn(List.of("RESUELTO"));
    }

    @Test
    @DisplayName("devuelve una lista vacia cuando ningun caso tiene el estado pedido")
    void debeDevolverListaVaciaSinCoincidencias() {
        when(reporteRepository.findByEstadoIn(List.of("RESUELTO"))).thenReturn(List.of());

        ResponseEntity<?> respuesta = controller.obtenerReportes("RESUELTO");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cuerpo(respuesta)).isEmpty();
    }

    @Test
    @DisplayName("ignora espacios y repeticiones en el filtro")
    void debeNormalizarElFiltro() {
        simularConsultas();

        controller.obtenerReportes(" PENDIENTE , PENDIENTE ");

        verify(reporteRepository).findByEstadoIn(List.of("PENDIENTE"));
    }

    @Test
    @DisplayName("responde 400 cuando el estado pedido no existe")
    void debeRechazarUnEstadoInexistente() {
        simularConsultas();

        ResponseEntity<?> respuesta = controller.obtenerReportes("PERDIDO");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).isInstanceOf(ApiError.class);
        assertThat(((ApiError) respuesta.getBody()).status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        verify(reporteRepository, never()).findAll();
        verify(reporteRepository, never()).findByEstadoIn(anyList());
    }

    @Test
    @DisplayName("rechaza el filtro completo si uno de los estados no existe")
    void debeRechazarSiUnEstadoDelFiltroNoExiste() {
        simularConsultas();

        ResponseEntity<?> respuesta = controller.obtenerReportes("PENDIENTE,PERDIDO");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(reporteRepository, never()).findByEstadoIn(anyList());
    }
}
