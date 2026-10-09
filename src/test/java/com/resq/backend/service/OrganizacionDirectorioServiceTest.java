package com.resq.backend.service;

import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.exception.OrganizacionException;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.UsuarioRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OrganizacionService: directorio de organizaciones (HU-29)")
class OrganizacionDirectorioServiceTest {

    private OrganizacionRepository organizacionRepository;
    private OrganizacionService servicio;

    @BeforeEach
    void setUp() {
        organizacionRepository = mock(OrganizacionRepository.class);
        servicio = new OrganizacionService(
                organizacionRepository,
                mock(UsuarioRepository.class),
                mock(FotoStorageService.class),
                mock(EmailService.class),
                mock(Validator.class));
    }

    private static Organizacion organizacion(String nombre, String tipo, String zonas, String estado) {
        Organizacion o = new Organizacion();
        o.setIdRepresentante(1L);
        o.setNombre(nombre);
        o.setTipo(tipo);
        o.setZonasCobertura(zonas);
        o.setEstadoVerificacion(estado);
        return o;
    }

    private void verificadas(Organizacion... organizaciones) {
        when(organizacionRepository.findByEstadoVerificacion(EstadoVerificacion.VERIFICADA))
                .thenReturn(List.of(organizaciones));
    }

    // ---------------------------------------------------------------- listado

    @Test
    @DisplayName("solo pide las organizaciones verificadas y las ordena por nombre")
    void soloVerificadasYOrdenadas() {
        verificadas(
                organizacion("Zeta Refugio", "REFUGIO", null, EstadoVerificacion.VERIFICADA),
                organizacion("alfa Vet", "VETERINARIA", null, EstadoVerificacion.VERIFICADA),
                organizacion("Beta Rescate", "RESCATISTA_INDEPENDIENTE", null, EstadoVerificacion.VERIFICADA));

        List<Organizacion> directorio = servicio.consultarDirectorio(null, null);

        verify(organizacionRepository).findByEstadoVerificacion(EstadoVerificacion.VERIFICADA);
        assertThat(directorio).extracting(Organizacion::getNombre)
                .containsExactly("alfa Vet", "Beta Rescate", "Zeta Refugio");
    }

    @Test
    @DisplayName("sin filtros devuelve todas las verificadas")
    void sinFiltrosDevuelveTodo() {
        verificadas(
                organizacion("Refugio A", "REFUGIO", "Cochabamba", EstadoVerificacion.VERIFICADA),
                organizacion("Vet B", "VETERINARIA", "La Paz", EstadoVerificacion.VERIFICADA));

        assertThat(servicio.consultarDirectorio(null, null)).hasSize(2);
    }

    @Test
    @DisplayName("devuelve lista vacía cuando no hay organizaciones verificadas")
    void sinVerificadasDevuelveVacio() {
        verificadas();

        assertThat(servicio.consultarDirectorio(null, null)).isEmpty();
    }

    @Test
    @DisplayName("filtra por tipo sin distinguir mayúsculas y espacios")
    void filtraPorTipo() {
        verificadas(
                organizacion("Refugio A", "REFUGIO", null, EstadoVerificacion.VERIFICADA),
                organizacion("Vet B", "VETERINARIA", null, EstadoVerificacion.VERIFICADA));

        assertThat(servicio.consultarDirectorio(" veterinaria ", null))
                .extracting(Organizacion::getNombre)
                .containsExactly("Vet B");
    }

    @Test
    @DisplayName("un tipo fuera del catálogo se rechaza con 400 y no consulta el repositorio")
    void tipoInvalidoEsBadRequest() {
        assertThatThrownBy(() -> servicio.consultarDirectorio("TIENDA", null))
                .isInstanceOfSatisfying(OrganizacionException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrores()).containsKey("tipo");
                });
        verify(organizacionRepository, never()).findByEstadoVerificacion(any());
    }

    @Test
    @DisplayName("filtra por zona de cobertura de forma parcial y sin distinguir mayúsculas")
    void filtraPorZona() {
        verificadas(
                organizacion("Refugio A", "REFUGIO", "Cochabamba, Quillacollo", EstadoVerificacion.VERIFICADA),
                organizacion("Vet B", "VETERINARIA", "La Paz", EstadoVerificacion.VERIFICADA));

        assertThat(servicio.consultarDirectorio(null, "quillacol"))
                .extracting(Organizacion::getNombre)
                .containsExactly("Refugio A");
    }

    @Test
    @DisplayName("una zona en blanco no filtra")
    void zonaEnBlancoNoFiltra() {
        verificadas(
                organizacion("Refugio A", "REFUGIO", "Cochabamba", EstadoVerificacion.VERIFICADA),
                organizacion("Vet B", "VETERINARIA", "La Paz", EstadoVerificacion.VERIFICADA));

        assertThat(servicio.consultarDirectorio(null, "   ")).hasSize(2);
    }

    @Test
    @DisplayName("una organización sin zonas de cobertura no coincide con el filtro de zona")
    void sinZonasNoCoincideConZona() {
        verificadas(organizacion("Refugio A", "REFUGIO", null, EstadoVerificacion.VERIFICADA));

        assertThat(servicio.consultarDirectorio(null, "Cochabamba")).isEmpty();
    }

    @Test
    @DisplayName("combina los filtros de tipo y zona")
    void combinaTipoYZona() {
        verificadas(
                organizacion("Refugio A", "REFUGIO", "Cochabamba", EstadoVerificacion.VERIFICADA),
                organizacion("Refugio B", "REFUGIO", "La Paz", EstadoVerificacion.VERIFICADA),
                organizacion("Vet C", "VETERINARIA", "Cochabamba", EstadoVerificacion.VERIFICADA));

        assertThat(servicio.consultarDirectorio("REFUGIO", "Cochabamba"))
                .extracting(Organizacion::getNombre)
                .containsExactly("Refugio A");
    }

    // ---------------------------------------------------------------- ficha

    @Test
    @DisplayName("la ficha de una organización verificada se devuelve completa")
    void fichaDeVerificada() {
        Organizacion verificada = organizacion("Refugio A", "REFUGIO", "Cochabamba", EstadoVerificacion.VERIFICADA);
        verificada.setIdOrganizacion(5L);
        verificada.setTelefono("70123456");
        verificada.setEmail("contacto@refugio.org");
        verificada.setHorarios("Lun a Vie 9-18");
        when(organizacionRepository.findById(5L)).thenReturn(Optional.of(verificada));

        Optional<Organizacion> ficha = servicio.obtenerFicha(5L);

        assertThat(ficha).containsSame(verificada);
    }

    @Test
    @DisplayName("la ficha de una organización pendiente no es pública (vacío)")
    void fichaDePendienteEsVacia() {
        when(organizacionRepository.findById(5L))
                .thenReturn(Optional.of(organizacion("Pendiente", "REFUGIO", null, EstadoVerificacion.PENDIENTE)));

        assertThat(servicio.obtenerFicha(5L)).isEmpty();
    }

    @Test
    @DisplayName("la ficha de una organización rechazada no es pública (vacío)")
    void fichaDeRechazadaEsVacia() {
        when(organizacionRepository.findById(5L))
                .thenReturn(Optional.of(organizacion("Rechazada", "REFUGIO", null, EstadoVerificacion.RECHAZADA)));

        assertThat(servicio.obtenerFicha(5L)).isEmpty();
    }

    @Test
    @DisplayName("una organización inexistente no tiene ficha")
    void fichaDeInexistenteEsVacia() {
        when(organizacionRepository.findById(5L)).thenReturn(Optional.empty());

        assertThat(servicio.obtenerFicha(5L)).isEmpty();
    }

    @Test
    @DisplayName("sin id no se consulta la base de datos")
    void fichaSinIdEsVacia() {
        assertThat(servicio.obtenerFicha(null)).isEmpty();
        verify(organizacionRepository, never()).findById(any());
    }
}
