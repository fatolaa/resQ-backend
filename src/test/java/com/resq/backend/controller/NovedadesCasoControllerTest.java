package com.resq.backend.controller;

import com.resq.backend.dto.NovedadCasoDTO;
import com.resq.backend.entity.EstadoRevision;
import com.resq.backend.entity.Reporte;
import com.resq.backend.entity.SeguimientoCaso;
import com.resq.backend.repository.ReporteRepository;
import com.resq.backend.repository.SeguimientoCasoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Novedades publicas del caso (rediseño)")
class NovedadesCasoControllerTest {

    private ReporteRepository reporteRepository;
    private SeguimientoCasoRepository seguimientoRepository;
    private NovedadesCasoController controller;

    @BeforeEach
    void setUp() {
        reporteRepository = mock(ReporteRepository.class);
        seguimientoRepository = mock(SeguimientoCasoRepository.class);
        controller = new NovedadesCasoController(reporteRepository, seguimientoRepository);
    }

    private static Reporte reporte(String revision) {
        Reporte r = new Reporte();
        r.setIdReporte(7L);
        r.setEstadoRevision(revision);
        return r;
    }

    @Test
    @DisplayName("devuelve fecha, nota y foto de cada seguimiento")
    void devuelveNovedades() {
        SeguimientoCaso s = new SeguimientoCaso();
        s.setIdReporte(7L);
        s.setIdOrganizacion(3L);
        s.setNota("Lo llevamos al veterinario");
        s.setFotoUrl("/api/reportes/fotos/vet.jpg");
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte(EstadoRevision.APROBADO)));
        when(seguimientoRepository.findByIdReporteOrderByFechaCreacionDesc(7L)).thenReturn(List.of(s));

        ResponseEntity<List<NovedadCasoDTO>> respuesta = controller.novedades(7L);

        assertThat(respuesta.getBody()).extracting(NovedadCasoDTO::nota).containsExactly("Lo llevamos al veterinario");
        assertThat(respuesta.getBody().get(0).fotoUrl()).isEqualTo("/api/reportes/fotos/vet.jpg");
    }

    @Test
    @DisplayName("un caso rechazado o inexistente responde 404")
    void rechazadoOInexistente() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte(EstadoRevision.RECHAZADO)));
        when(reporteRepository.findById(8L)).thenReturn(Optional.empty());

        assertThat(controller.novedades(7L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.novedades(8L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
