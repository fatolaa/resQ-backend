package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ReporteController: busqueda y ordenamiento de casos (HU-24)")
class ReporteControllerBusquedaTest {

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

    @SuppressWarnings("unchecked")
    private List<Reporte> cuerpo(ResponseEntity<?> respuesta) {
        assertThat(respuesta.getBody()).isInstanceOf(List.class);
        return (List<Reporte>) respuesta.getBody();
    }

    @Test
    @DisplayName("busca por texto cuando no se filtra por estado")
    void debeBuscarPorTextoSinFiltroDeEstado() {
        when(reporteRepository.buscar(eq("perro"), any()))
                .thenReturn(List.of(reporteEn("PENDIENTE")));

        ResponseEntity<?> respuesta = controller.obtenerReportes(null, " perro ", null);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cuerpo(respuesta)).hasSize(1);
        verify(reporteRepository).buscar(eq("perro"), any());
        verify(reporteRepository, never()).findAll();
    }

    @Test
    @DisplayName("combina la busqueda con el filtro por estado")
    void debeCombinarBusquedaYFiltroDeEstado() {
        when(reporteRepository.buscarPorEstados(eq("plaza"), eq(List.of("PENDIENTE")), any()))
                .thenReturn(List.of(reporteEn("PENDIENTE")));

        ResponseEntity<?> respuesta = controller.obtenerReportes("PENDIENTE", "plaza", null);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(reporteRepository).buscarPorEstados(eq("plaza"), eq(List.of("PENDIENTE")), any());
        verify(reporteRepository, never()).findByEstadoIn(anyList());
    }

    @Test
    @DisplayName("ignora una busqueda que solo tiene espacios")
    void debeIgnorarBusquedaVacia() {
        when(reporteRepository.findAll()).thenReturn(List.of(reporteEn("PENDIENTE")));

        ResponseEntity<?> respuesta = controller.obtenerReportes(null, "   ", null);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(reporteRepository).findAll();
        verify(reporteRepository, never()).buscar(any(), any());
    }

    @Test
    @DisplayName("ordena por fecha descendente cuando se piden los mas recientes")
    void debeOrdenarPorRecientes() {
        Sort esperado = Sort.by(Sort.Direction.DESC, "fechaCreacion");
        when(reporteRepository.findAll(esperado)).thenReturn(List.of(reporteEn("PENDIENTE")));

        ResponseEntity<?> respuesta = controller.obtenerReportes(null, null, "RECIENTES");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(reporteRepository).findAll(esperado);
    }

    @Test
    @DisplayName("ordena por fecha ascendente cuando se piden los mas antiguos")
    void debeOrdenarPorAntiguos() {
        Sort esperado = Sort.by(Sort.Direction.ASC, "fechaCreacion");
        when(reporteRepository.findByEstadoIn(List.of("RESUELTO"), esperado))
                .thenReturn(List.of(reporteEn("RESUELTO")));

        ResponseEntity<?> respuesta = controller.obtenerReportes("RESUELTO", null, "ANTIGUOS");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(reporteRepository).findByEstadoIn(List.of("RESUELTO"), esperado);
    }

    @Test
    @DisplayName("no altera el orden de la consulta cuando no se pide uno")
    void debeRespetarElOrdenNaturalSinParametro() {
        when(reporteRepository.findAll()).thenReturn(List.of(reporteEn("PENDIENTE")));

        controller.obtenerReportes(null, null, null);

        verify(reporteRepository).findAll();
        verify(reporteRepository, never()).findAll(any(Sort.class));
    }

    @Test
    @DisplayName("responde 400 cuando el orden pedido no existe")
    void debeRechazarUnOrdenInexistente() {
        ResponseEntity<?> respuesta = controller.obtenerReportes(null, null, "ALFABETICO");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).isInstanceOf(ApiError.class);
        assertThat(((ApiError) respuesta.getBody()).status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        verify(reporteRepository, never()).findAll();
    }

    @Test
    @DisplayName("rechaza la consulta entera si el estado no existe aunque el orden sea valido")
    void debeRechazarElEstadoAntesDeBuscar() {
        ResponseEntity<?> respuesta = controller.obtenerReportes("PERDIDO", "perro", "RECIENTES");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(reporteRepository, never()).buscar(any(), any());
        verify(reporteRepository, never()).buscarPorEstados(any(), anyList(), any());
    }
}