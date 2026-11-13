package com.resq.backend.controller;

import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.ReporteRepository;
import com.resq.backend.repository.SeguimientoCasoRepository;
import com.resq.backend.service.NecesidadCasoService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Reportes: datos del animal, referencia y necesidades (rediseño)")
class ReporteDatosRedisenoTest {

    private ReporteRepository reporteRepository;
    private NecesidadCasoService necesidadService;
    private ReporteController controller;

    @BeforeEach
    void setUp() {
        reporteRepository = mock(ReporteRepository.class);
        necesidadService = mock(NecesidadCasoService.class);
        controller = new ReporteController(reporteRepository, mock(OrganizacionRepository.class),
                mock(SeguimientoCasoRepository.class), null, necesidadService);
    }

    private static Reporte reporte(long id, String estado) {
        Reporte r = new Reporte();
        r.setIdReporte(id);
        r.setIdUsuario(2L);
        r.setTipoCaso("PERDIDA");
        r.setDescripcion("Perro");
        r.setEstado(estado);
        return r;
    }

    @Test
    @DisplayName("especie, edad y tamano solo aceptan los valores del catalogo")
    void validaDatosDelAnimal() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Reporte bueno = reporte(1, "PENDIENTE");
        bueno.setEspecie("PERRO");
        bueno.setEdad("ADULTO");
        bueno.setTamano("MEDIANO");
        assertThat(validator.validate(bueno)).isEmpty();

        Reporte malo = reporte(1, "PENDIENTE");
        malo.setEspecie("DRAGON");
        malo.setReferencia("x".repeat(201));
        assertThat(validator.validate(malo)).hasSize(2);
    }

    @Test
    @DisplayName("al crear guarda la referencia limpia y crea las necesidades elegidas")
    void creaConNecesidades() {
        Reporte nuevo = reporte(0, "PENDIENTE");
        nuevo.setIdReporte(null);
        nuevo.setReferencia("  frente a la farmacia ");
        nuevo.setNecesidades(List.of("transporte"));
        when(necesidadService.validarTipos(List.of("transporte"))).thenReturn(List.of("TRANSPORTE"));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> {
            Reporte r = inv.getArgument(0);
            r.setIdReporte(42L);
            return r;
        });

        ResponseEntity<Reporte> respuesta = controller.crearReporte(nuevo);

        assertThat(respuesta.getBody().getReferencia()).isEqualTo("frente a la farmacia");
        verify(necesidadService).crearIniciales(42L, List.of("TRANSPORTE"));
    }

    @Test
    @DisplayName("el filtro necesidad deja solo los casos que todavia la buscan")
    void filtraPorNecesidad() {
        when(reporteRepository.findByEstadoIn(List.of("PENDIENTE"))).thenReturn(List.of(reporte(1, "PENDIENTE"), reporte(2, "PENDIENTE")));
        when(necesidadService.casosQueBuscan("TRANSPORTE")).thenReturn(Set.of(2L));

        ResponseEntity<?> respuesta = controller.obtenerReportes("PENDIENTE", null, null, "TRANSPORTE");

        assertThat((List<?>) respuesta.getBody()).extracting(r -> ((Reporte) r).getIdReporte()).containsExactly(2L);
    }
}
