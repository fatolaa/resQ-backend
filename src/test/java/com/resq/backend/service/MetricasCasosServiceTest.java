package com.resq.backend.service;

import com.resq.backend.dto.MetricasCasosDTO;
import com.resq.backend.entity.EstadoRevision;
import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MetricasCasosService: indicadores de casos y reportes (HU-33)")
class MetricasCasosServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 31);
    private static final LocalDateTime INICIO = DESDE.atStartOfDay();
    private static final LocalDateTime FIN = HASTA.atTime(LocalTime.MAX);

    // Puntos de Cochabamba ya verificados en ZonaCiudadTest.
    private static final double[] PLAZA = {-17.3892, -66.1567};
    private static final double[] CALA_CALA = {-17.365, -66.160};
    private static final double[] QUILLACOLLO = {-17.392, -66.278};

    /** Lo que "hay en la base": las pruebas lo modifican para simular altas, ediciones y bajas. */
    private List<Reporte> base;
    private ReporteRepository reporteRepository;
    private MetricasCasosService servicio;
    private long siguienteId;

    @BeforeEach
    void setUp() {
        base = new ArrayList<>();
        siguienteId = 1;
        reporteRepository = mock(ReporteRepository.class);
        when(reporteRepository.findByFechaCreacionBetween(any(), any()))
                .thenAnswer(invocacion -> List.copyOf(base));
        servicio = new MetricasCasosService(reporteRepository);
    }

    private Reporte reporte(String tipo, String estado, String revision, double[] punto) {
        Reporte reporte = new Reporte();
        reporte.setIdReporte(siguienteId++);
        reporte.setIdUsuario(1L);
        reporte.setTipoCaso(tipo);
        reporte.setEstado(estado);
        reporte.setEstadoRevision(revision);
        reporte.setFechaCreacion(INICIO.plusDays(3));
        if (punto != null) {
            reporte.setLatitud(punto[0]);
            reporte.setLongitud(punto[1]);
        }
        base.add(reporte);
        return reporte;
    }

    private static Map<String, Long> comoMapa(List<MetricasCasosDTO.Conteo> filas) {
        return filas.stream().collect(Collectors.toMap(MetricasCasosDTO.Conteo::clave,
                MetricasCasosDTO.Conteo::cantidad));
    }

    private static List<String> claves(List<MetricasCasosDTO.Conteo> filas) {
        return filas.stream().map(MetricasCasosDTO.Conteo::clave).toList();
    }

    @Test
    @DisplayName("consulta los reportes creados entre el inicio del primer dia y el final del ultimo")
    void consultaElPeriodoCompleto() {
        servicio.calcular(DESDE, HASTA);

        verify(reporteRepository).findByFechaCreacionBetween(INICIO, FIN);
    }

    @Test
    @DisplayName("cuenta como reporte todo lo registrado y como caso solo lo aprobado en la revision")
    void totalesDeReportesYCasos() {
        reporte("PERDIDA", "PENDIENTE", EstadoRevision.APROBADO, PLAZA);
        reporte("ENCONTRADA", "EN_PROCESO", EstadoRevision.APROBADO, CALA_CALA);
        reporte("ABANDONADA", "PENDIENTE", EstadoRevision.PENDIENTE, PLAZA);
        reporte("PERDIDA", "PENDIENTE", EstadoRevision.RECHAZADO, PLAZA);

        MetricasCasosDTO metricas = servicio.calcular(DESDE, HASTA);

        assertThat(metricas.desde()).isEqualTo(DESDE);
        assertThat(metricas.hasta()).isEqualTo(HASTA);
        assertThat(metricas.reportes().total()).isEqualTo(4);
        assertThat(metricas.casos().total()).isEqualTo(2);
    }

    @Test
    @DisplayName("agrupa los reportes por estado de revision; sin revision cuenta como pendiente")
    void reportesPorEstadoDeRevision() {
        reporte("PERDIDA", "PENDIENTE", EstadoRevision.APROBADO, PLAZA);
        reporte("PERDIDA", "PENDIENTE", EstadoRevision.RECHAZADO, PLAZA);
        reporte("PERDIDA", "PENDIENTE", EstadoRevision.PENDIENTE, PLAZA);
        reporte("PERDIDA", "PENDIENTE", null, PLAZA); // reporte anterior a HU-19

        List<MetricasCasosDTO.Conteo> porEstado = servicio.calcular(DESDE, HASTA).reportes().porEstado();

        assertThat(claves(porEstado)).containsExactly("PENDIENTE_REVISION", "APROBADO", "RECHAZADO");
        assertThat(comoMapa(porEstado)).containsEntry("PENDIENTE_REVISION", 2L)
                .containsEntry("APROBADO", 1L)
                .containsEntry("RECHAZADO", 1L);
    }

    @Test
    @DisplayName("agrupa los reportes por categoria sin importar mayusculas ni espacios")
    void reportesPorCategoria() {
        reporte("Perdida", "PENDIENTE", EstadoRevision.PENDIENTE, PLAZA);
        reporte(" PERDIDA ", "PENDIENTE", EstadoRevision.PENDIENTE, PLAZA);
        reporte("abandonada", "PENDIENTE", EstadoRevision.PENDIENTE, PLAZA);

        Map<String, Long> porCategoria = comoMapa(servicio.calcular(DESDE, HASTA).reportes().porCategoria());

        assertThat(porCategoria).containsEntry("PERDIDA", 2L)
                .containsEntry("ABANDONADA", 1L)
                .containsEntry("ENCONTRADA", 0L);
    }

    @Test
    @DisplayName("agrupa los casos por estado, categoria y zona")
    void casosAgrupados() {
        reporte("PERDIDA", "PENDIENTE", EstadoRevision.APROBADO, PLAZA);
        reporte("PERDIDA", "RESUELTO", EstadoRevision.APROBADO, CALA_CALA);
        reporte("ENCONTRADA", "RESUELTO", EstadoRevision.APROBADO, QUILLACOLLO);
        reporte("ABANDONADA", "EN_PROCESO", EstadoRevision.APROBADO, null);
        reporte("ABANDONADA", "EN_PROCESO", EstadoRevision.RECHAZADO, PLAZA); // no es caso

        MetricasCasosDTO.Casos casos = servicio.calcular(DESDE, HASTA).casos();

        assertThat(comoMapa(casos.porEstado())).containsEntry("PENDIENTE", 1L)
                .containsEntry("EN_PROCESO", 1L)
                .containsEntry("RESUELTO", 2L)
                .containsEntry("CANCELADO", 0L);
        assertThat(comoMapa(casos.porCategoria())).containsEntry("PERDIDA", 2L)
                .containsEntry("ENCONTRADA", 1L)
                .containsEntry("ABANDONADA", 1L);
        assertThat(comoMapa(casos.porZona())).containsEntry(ZonaCiudad.CENTRO, 1L)
                .containsEntry(ZonaCiudad.NORTE, 1L)
                .containsEntry(ZonaCiudad.AFUERAS, 1L)
                .containsEntry(ZonaCiudad.SIN_UBICACION, 1L)
                .containsEntry(ZonaCiudad.SUR, 0L);
    }

    @Test
    @DisplayName("sin datos en el periodo lista las filas conocidas en cero, en orden fijo")
    void periodoVacio() {
        MetricasCasosDTO metricas = servicio.calcular(DESDE, HASTA);

        assertThat(metricas.reportes().total()).isZero();
        assertThat(metricas.casos().total()).isZero();
        assertThat(claves(metricas.casos().porEstado()))
                .containsExactly("PENDIENTE", "EN_PROCESO", "RESUELTO", "CANCELADO");
        assertThat(claves(metricas.casos().porCategoria())).containsExactly("PERDIDA", "ENCONTRADA", "ABANDONADA");
        assertThat(claves(metricas.casos().porZona()))
                .containsExactly("CENTRO", "NORTE", "SUR", "ESTE", "OESTE", "AFUERAS");
        assertThat(metricas.casos().porZona()).allMatch(fila -> fila.cantidad() == 0);
    }

    @Test
    @DisplayName("un valor desconocido se muestra despues de los conocidos y SIN_DATO al final")
    void valoresDesconocidosYVacios() {
        reporte("HERIDA", "PENDIENTE", EstadoRevision.APROBADO, PLAZA);
        reporte("", "PENDIENTE", EstadoRevision.APROBADO, PLAZA);

        List<MetricasCasosDTO.Conteo> porCategoria = servicio.calcular(DESDE, HASTA).casos().porCategoria();

        assertThat(claves(porCategoria))
                .containsExactly("PERDIDA", "ENCONTRADA", "ABANDONADA", "HERIDA", MetricasCasosService.SIN_DATO);
    }

    @Test
    @DisplayName("al registrar, editar o eliminar un reporte la siguiente consulta refleja el cambio")
    void reflejaCambiosAlActualizar() {
        Reporte editado = reporte("PERDIDA", "PENDIENTE", EstadoRevision.APROBADO, PLAZA);
        Reporte eliminado = reporte("ENCONTRADA", "PENDIENTE", EstadoRevision.APROBADO, PLAZA);
        MetricasCasosDTO antes = servicio.calcular(DESDE, HASTA);
        assertThat(antes.casos().total()).isEqualTo(2);
        assertThat(comoMapa(antes.casos().porEstado())).containsEntry("PENDIENTE", 2L);

        editado.setEstado("RESUELTO");                                    // edicion (HU-13)
        base.remove(eliminado);                                           // eliminacion
        reporte("ABANDONADA", "PENDIENTE", EstadoRevision.PENDIENTE, PLAZA); // registro nuevo

        MetricasCasosDTO despues = servicio.calcular(DESDE, HASTA);
        assertThat(despues.reportes().total()).isEqualTo(2);
        assertThat(despues.casos().total()).isEqualTo(1);
        assertThat(comoMapa(despues.casos().porEstado())).containsEntry("PENDIENTE", 0L)
                .containsEntry("RESUELTO", 1L);
        assertThat(comoMapa(despues.reportes().porEstado())).containsEntry("PENDIENTE_REVISION", 1L);
    }

    @Test
    @DisplayName("rechaza los mismos periodos invalidos que las metricas de la comunidad sin consultar la base")
    void periodoInvalido() {
        assertThatThrownBy(() -> servicio.calcular(null, HASTA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.calcular(HASTA, DESDE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior");
        assertThatThrownBy(() -> servicio.calcular(DESDE, DESDE.plusDays(MetricasService.MAX_DIAS_PERIODO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dias");

        verify(reporteRepository, never()).findByFechaCreacionBetween(any(), any());
    }
}
