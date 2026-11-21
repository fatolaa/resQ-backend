package com.resq.backend.service;

import com.resq.backend.entity.Organizacion;
import com.resq.backend.repository.OrganizacionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** TT-49: filtro por nombre del directorio de organizaciones. */
class DirectorioOrganizacionesNombreTest {

    private final OrganizacionRepository repositorio = mock(OrganizacionRepository.class);

    private static Organizacion organizacion(Long id, String nombre) {
        Organizacion o = new Organizacion();
        o.setIdOrganizacion(id);
        o.setNombre(nombre);
        return o;
    }

    private List<Organizacion> buscar(List<Organizacion> datos, String nombre) {
        when(repositorio.findByEstadoVerificacion("VERIFICADA")).thenReturn(datos);
        OrganizacionService servicio = new OrganizacionService(
                repositorio,
                mock(com.resq.backend.repository.UsuarioRepository.class),
                mock(FotoStorageService.class),
                mock(EmailService.class),
                mock(jakarta.validation.Validator.class));
        return servicio.consultarDirectorio(null, null, nombre);
    }

    @Test
    void filtraPorNombreSinDistinguirMayusculas() {
        var resultado = buscar(List.of(
                organizacion(1L, "Patitas Felices"),
                organizacion(2L, "Refugio Central")), "PATITAS");
        assertEquals(List.of(1L), resultado.stream().map(Organizacion::getIdOrganizacion).toList());
    }

    @Test
    void sinNombreConservaElDirectorioCompleto() {
        assertEquals(2, buscar(List.of(
                organizacion(1L, "Patitas Felices"),
                organizacion(2L, "Refugio Central")), null).size());
    }
}
