package com.resq.backend.websocket;

import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipanteCasoService: quien participa de un caso (HU-38a)")
class ParticipanteCasoServiceTest {

    @Mock
    private ReporteRepository reporteRepository;
    @Mock
    private OrganizacionRepository organizacionRepository;

    private ParticipanteCasoService servicio;

    @BeforeEach
    void setUp() {
        servicio = new ParticipanteCasoService(reporteRepository, organizacionRepository);
    }

    private Reporte reporteDe(Long idReporte, Long idUsuario, Long idOrganizacion) {
        Reporte reporte = new Reporte();
        reporte.setIdReporte(idReporte);
        reporte.setIdUsuario(idUsuario);
        reporte.setIdOrganizacion(idOrganizacion);
        return reporte;
    }

    private Organizacion organizacionDe(Long idOrganizacion, Long idRepresentante) {
        Organizacion organizacion = new Organizacion();
        organizacion.setIdOrganizacion(idOrganizacion);
        organizacion.setIdRepresentante(idRepresentante);
        return organizacion;
    }

    @Test
    @DisplayName("el reportante del caso participa")
    void elReportanteParticipa() {
        when(reporteRepository.findById(101L))
                .thenReturn(Optional.of(reporteDe(101L, 7L, null)));

        assertThat(servicio.participa(7L, 101L)).isTrue();
    }

    @Test
    @DisplayName("el representante de la organizacion asignada participa")
    void elRepresentanteDeLaOrganizacionParticipa() {
        when(reporteRepository.findById(101L))
                .thenReturn(Optional.of(reporteDe(101L, 7L, 3L)));
        when(organizacionRepository.findById(3L))
                .thenReturn(Optional.of(organizacionDe(3L, 9L)));

        assertThat(servicio.participa(9L, 101L)).isTrue();
    }

    @Test
    @DisplayName("un usuario ajeno al caso no participa")
    void unUsuarioAjenoNoParticipa() {
        when(reporteRepository.findById(101L))
                .thenReturn(Optional.of(reporteDe(101L, 7L, null)));

        assertThat(servicio.participa(99L, 101L)).isFalse();
    }

    @Test
    @DisplayName("un caso inexistente o datos nulos no dan participacion")
    void casoInexistenteONulosNoParticipan() {
        when(reporteRepository.findById(404L)).thenReturn(Optional.empty());

        assertThat(servicio.participa(7L, 404L)).isFalse();
        assertThat(servicio.participa(null, 101L)).isFalse();
        assertThat(servicio.participa(7L, null)).isFalse();
    }

    @Test
    @DisplayName("casosDe reune los reportes propios y los de la organizacion, sin repetir")
    void casosDeReunePropiosYDeLaOrganizacion() {
        when(reporteRepository.findByIdUsuario(7L))
                .thenReturn(List.of(reporteDe(101L, 7L, null), reporteDe(102L, 7L, 3L)));
        when(organizacionRepository.findByIdRepresentante(7L))
                .thenReturn(Optional.of(organizacionDe(3L, 7L)));
        when(reporteRepository.findByIdOrganizacion(3L))
                .thenReturn(List.of(reporteDe(102L, 7L, 3L), reporteDe(103L, 5L, 3L)));

        assertThat(servicio.casosDe(7L)).containsExactly(101L, 102L, 103L);
    }
}
