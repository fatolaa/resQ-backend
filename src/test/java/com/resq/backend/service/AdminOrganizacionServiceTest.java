package com.resq.backend.service;

import com.resq.backend.dto.OrganizacionAdminDTO;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.Usuario;
import com.resq.backend.exception.OrganizacionException;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AdminOrganizacionService: administrar organizaciones (HU-25)")
class AdminOrganizacionServiceTest {

    private static final long ID_ADMIN = 1L;
    private static final long ID_REPRESENTANTE = 20L;

    private OrganizacionRepository organizacionRepository;
    private UsuarioRepository usuarioRepository;
    private AdminOrganizacionService servicio;

    @BeforeEach
    void setUp() {
        organizacionRepository = mock(OrganizacionRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        servicio = new AdminOrganizacionService(organizacionRepository, usuarioRepository,
                mock(DocumentoStorageService.class));

        when(usuarioRepository.findById(ID_ADMIN)).thenReturn(Optional.of(usuario(ID_ADMIN, "Admin Demo")));
        when(usuarioRepository.findById(ID_REPRESENTANTE))
                .thenReturn(Optional.of(usuario(ID_REPRESENTANTE, "Ana Refugio")));
        when(usuarioRepository.findAllById(anyIterable())).thenReturn(
                List.of(usuario(ID_ADMIN, "Admin Demo"), usuario(ID_REPRESENTANTE, "Ana Refugio")));
        when(organizacionRepository.save(any(Organizacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Usuario usuario(long id, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombre(nombre);
        usuario.setEmail("usuario" + id + "@resq.com");
        return usuario;
    }

    private static Organizacion organizacion(long id, String estado) {
        Organizacion o = new Organizacion();
        o.setIdOrganizacion(id);
        o.setIdRepresentante(ID_REPRESENTANTE);
        o.setNombre("Refugio " + id);
        o.setTipo("REFUGIO");
        o.setEstadoVerificacion(estado);
        o.setFechaRegistro(LocalDateTime.of(2026, 10, 1, 10, 0).plusDays(id));
        return o;
    }

    // ---------------------------------------------------------------- consultar

    @Test
    @DisplayName("sin filtro lista todas las organizaciones con el nombre de su representante")
    void listarTodas() {
        when(organizacionRepository.findAllByOrderByFechaRegistroDesc()).thenReturn(List.of(
                organizacion(2, EstadoVerificacion.PENDIENTE), organizacion(1, EstadoVerificacion.VERIFICADA)));

        List<OrganizacionAdminDTO> lista = servicio.listar(null);

        assertThat(lista).extracting(OrganizacionAdminDTO::idOrganizacion).containsExactly(2L, 1L);
        assertThat(lista).allMatch(o -> "Ana Refugio".equals(o.nombreRepresentante()));
    }

    @Test
    @DisplayName("filtra por estado sin importar mayusculas ni espacios")
    void listarPendientes() {
        when(organizacionRepository.findByEstadoVerificacionOrderByFechaRegistroDesc(EstadoVerificacion.PENDIENTE))
                .thenReturn(List.of(organizacion(3, EstadoVerificacion.PENDIENTE)));

        List<OrganizacionAdminDTO> lista = servicio.listar(" pendiente_verificacion ");

        assertThat(lista).singleElement()
                .satisfies(o -> assertThat(o.estadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE));
        verify(organizacionRepository, never()).findAllByOrderByFechaRegistroDesc();
    }

    @Test
    @DisplayName("un estado desconocido responde 400 en vez de una lista vacia")
    void estadoInvalido() {
        assertThatThrownBy(() -> servicio.listar("ACTIVA"))
                .isInstanceOf(OrganizacionException.class)
                .satisfies(e -> assertThat(((OrganizacionException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("el detalle trae la organizacion en cualquier estado, con representante y revisor")
    void detalle() {
        Organizacion rechazada = organizacion(5, EstadoVerificacion.RECHAZADA);
        rechazada.setIdRevisor(ID_ADMIN);
        rechazada.setFechaRevision(LocalDateTime.of(2026, 10, 20, 9, 30));
        when(organizacionRepository.findById(5L)).thenReturn(Optional.of(rechazada));

        OrganizacionAdminDTO detalle = servicio.detalle(5L);

        assertThat(detalle.estadoVerificacion()).isEqualTo(EstadoVerificacion.RECHAZADA);
        assertThat(detalle.emailRepresentante()).isEqualTo("usuario20@resq.com");
        assertThat(detalle.nombreRevisor()).isEqualTo("Admin Demo");
        assertThat(detalle.fechaRevision()).isEqualTo(LocalDateTime.of(2026, 10, 20, 9, 30));
    }

    @Test
    @DisplayName("una organizacion inexistente responde 404")
    void inexistente() {
        when(organizacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.detalle(99L))
                .isInstanceOf(OrganizacionException.class)
                .satisfies(e -> assertThat(((OrganizacionException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---------------------------------------------------------------- aprobar / rechazar

    @Test
    @DisplayName("aprobar deja la organizacion VERIFICADA y registra quien y cuando")
    void aprobar() {
        Organizacion pendiente = organizacion(7, EstadoVerificacion.PENDIENTE);
        when(organizacionRepository.findById(7L)).thenReturn(Optional.of(pendiente));
        LocalDateTime antes = LocalDateTime.now();

        OrganizacionAdminDTO resultado = servicio.aprobar(7L, ID_ADMIN);

        assertThat(resultado.estadoVerificacion()).isEqualTo(EstadoVerificacion.VERIFICADA);
        assertThat(pendiente.puedeGestionarCasos()).isTrue();
        assertThat(pendiente.getIdRevisor()).isEqualTo(ID_ADMIN);
        assertThat(pendiente.getFechaRevision()).isAfterOrEqualTo(antes);
        assertThat(resultado.nombreRevisor()).isEqualTo("Admin Demo");
        verify(organizacionRepository).save(pendiente);
    }

    @Test
    @DisplayName("rechazar deja la organizacion RECHAZADA, sin poder gestionar casos")
    void rechazar() {
        Organizacion pendiente = organizacion(8, EstadoVerificacion.PENDIENTE);
        when(organizacionRepository.findById(8L)).thenReturn(Optional.of(pendiente));

        OrganizacionAdminDTO resultado = servicio.rechazar(8L, ID_ADMIN);

        assertThat(resultado.estadoVerificacion()).isEqualTo(EstadoVerificacion.RECHAZADA);
        assertThat(pendiente.puedeGestionarCasos()).isFalse();
        assertThat(pendiente.getIdRevisor()).isEqualTo(ID_ADMIN);
    }

    @Test
    @DisplayName("no se puede decidir sobre una organizacion ya revisada (409) y no se guarda nada")
    void yaRevisada() {
        when(organizacionRepository.findById(9L))
                .thenReturn(Optional.of(organizacion(9, EstadoVerificacion.VERIFICADA)));

        assertThatThrownBy(() -> servicio.rechazar(9L, ID_ADMIN))
                .isInstanceOf(OrganizacionException.class)
                .satisfies(e -> assertThat(((OrganizacionException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(organizacionRepository, never()).save(any());
    }
}
