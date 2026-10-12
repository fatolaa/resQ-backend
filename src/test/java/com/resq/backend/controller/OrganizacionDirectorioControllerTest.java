package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.exception.OrganizacionException;
import com.resq.backend.service.OrganizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OrganizacionController: directorio de organizaciones (HU-29)")
class OrganizacionDirectorioControllerTest {

    private OrganizacionService servicio;
    private OrganizacionController controller;

    @BeforeEach
    void setUp() {
        servicio = mock(OrganizacionService.class);
        controller = new OrganizacionController(servicio);
    }

    private static Organizacion organizacion(String nombre, String estado) {
        Organizacion o = new Organizacion();
        o.setIdOrganizacion(5L);
        o.setNombre(nombre);
        o.setTipo("REFUGIO");
        o.setEstadoVerificacion(estado);
        return o;
    }

    // ---------------------------------------------------------------- GET directorio

    @Test
    @DisplayName("devuelve 200 con las organizaciones del directorio")
    void listadoDevuelve200() {
        List<Organizacion> directorio = List.of(
                organizacion("Refugio A", EstadoVerificacion.VERIFICADA),
                organizacion("Vet B", EstadoVerificacion.VERIFICADA));
        when(servicio.consultarDirectorio(null, null)).thenReturn(directorio);

        ResponseEntity<List<Organizacion>> respuesta = controller.consultarDirectorio(null, null);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(directorio);
    }

    @Test
    @DisplayName("pasa los filtros de tipo y zona al servicio")
    void listadoPasaFiltros() {
        when(servicio.consultarDirectorio(any(), any())).thenReturn(List.of());

        controller.consultarDirectorio("REFUGIO", "Cochabamba");

        ArgumentCaptor<String> tipo = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> zona = ArgumentCaptor.forClass(String.class);
        verify(servicio).consultarDirectorio(tipo.capture(), zona.capture());
        assertThat(tipo.getValue()).isEqualTo("REFUGIO");
        assertThat(zona.getValue()).isEqualTo("Cochabamba");
    }

    @Test
    @DisplayName("sin resultados el directorio responde 200 con lista vacía")
    void listadoVacioEs200() {
        when(servicio.consultarDirectorio(null, null)).thenReturn(List.of());

        ResponseEntity<List<Organizacion>> respuesta = controller.consultarDirectorio(null, null);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEmpty();
    }

    // ---------------------------------------------------------------- GET ficha

    @Test
    @DisplayName("devuelve 200 con la ficha de una organización verificada")
    void fichaDevuelve200() {
        Organizacion verificada = organizacion("Refugio A", EstadoVerificacion.VERIFICADA);
        when(servicio.obtenerFicha(5L)).thenReturn(Optional.of(verificada));

        ResponseEntity<Organizacion> respuesta = controller.obtenerFicha(5L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(verificada);
    }

    @Test
    @DisplayName("responde 404 si la organización no existe o no está verificada")
    void fichaNoPublicaEs404() {
        when(servicio.obtenerFicha(5L)).thenReturn(Optional.empty());

        assertThat(controller.obtenerFicha(5L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------------------------------------------------------------- errores

    @Test
    @DisplayName("un tipo inválido se convierte en ApiError 400")
    void tipoInvalidoEsApiError() {
        OrganizacionException error = new OrganizacionException(HttpStatus.BAD_REQUEST,
                "Tipo inválido: TIENDA. Permitidos: REFUGIO, VETERINARIA, RESCATISTA_INDEPENDIENTE",
                Map.of("tipo", "El tipo debe ser REFUGIO, VETERINARIA o RESCATISTA_INDEPENDIENTE"));

        ResponseEntity<ApiError> respuesta = controller.manejarError(error);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().status()).isEqualTo(400);
        assertThat(respuesta.getBody().errors()).containsKey("tipo");
    }

    @Test
    @DisplayName("el error del servicio se propaga para ser manejado por el controlador")
    void elErrorDelServicioSePropaga() {
        when(servicio.consultarDirectorio(eq("TIENDA"), any()))
                .thenThrow(new OrganizacionException(HttpStatus.BAD_REQUEST, "Tipo inválido", Map.of("tipo", "x")));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.consultarDirectorio("TIENDA", null))
                .isInstanceOf(OrganizacionException.class);
    }
}
