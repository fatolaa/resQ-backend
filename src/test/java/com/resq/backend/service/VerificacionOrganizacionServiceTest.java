package com.resq.backend.service;

import com.resq.backend.dto.OrganizacionAdminDTO;
import com.resq.backend.dto.RevisionOrganizacionDTO;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Notificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.RevisionOrganizacion;
import com.resq.backend.entity.Usuario;
import com.resq.backend.exception.OrganizacionException;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.RevisionOrganizacionRepository;
import com.resq.backend.repository.UsuarioRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HU-32: verificacion de organizaciones: motivo, historial, notificacion y reenvio.
 */
@DisplayName("Verificacion de organizaciones: motivo, historial, aviso y reenvio (HU-32)")
class VerificacionOrganizacionServiceTest {

    private static final long ID_ADMIN = 1L;
    private static final long ID_REPRESENTANTE = 20L;

    private OrganizacionRepository organizacionRepository;
    private RevisionOrganizacionRepository revisionRepository;
    private NotificacionService notificacionService;
    private EmailService emailService;
    private AdminOrganizacionService admin;
    private OrganizacionService organizaciones;

    @BeforeEach
    void setUp() {
        organizacionRepository = mock(OrganizacionRepository.class);
        revisionRepository = mock(RevisionOrganizacionRepository.class);
        notificacionService = mock(NotificacionService.class);
        emailService = mock(EmailService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

        when(usuarioRepository.findById(ID_ADMIN)).thenReturn(Optional.of(usuario(ID_ADMIN, "Admin Demo")));
        when(usuarioRepository.findById(ID_REPRESENTANTE))
                .thenReturn(Optional.of(usuario(ID_REPRESENTANTE, "Ana Refugio")));
        when(usuarioRepository.findAllById(anyIterable())).thenReturn(
                List.of(usuario(ID_ADMIN, "Admin Demo"), usuario(ID_REPRESENTANTE, "Ana Refugio")));
        when(organizacionRepository.save(any(Organizacion.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentoStorageService documentos = mock(DocumentoStorageService.class);
        admin = new AdminOrganizacionService(organizacionRepository, usuarioRepository, documentos,
                revisionRepository, notificacionService, emailService);
        organizaciones = new OrganizacionService(organizacionRepository, usuarioRepository,
                mock(FotoStorageService.class), documentos, emailService,
                Validation.buildDefaultValidatorFactory().getValidator(), revisionRepository);
    }

    private static Usuario usuario(long id, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombre(nombre);
        usuario.setEmail("usuario" + id + "@resq.com");
        return usuario;
    }

    private Organizacion organizacion(long id, String estado) {
        Organizacion o = new Organizacion();
        o.setIdOrganizacion(id);
        o.setIdRepresentante(ID_REPRESENTANTE);
        o.setNombre("Refugio Patitas");
        o.setEmail("patitas@resq.com");
        o.setEstadoVerificacion(estado);
        when(organizacionRepository.findById(id)).thenReturn(Optional.of(o));
        return o;
    }

    // ---------------------------------------------------------------- aprobar / rechazar

    @Test
    @DisplayName("rechazar sin motivo responde 400 y no cambia nada")
    void rechazarSinMotivo() {
        Organizacion o = organizacion(7, EstadoVerificacion.PENDIENTE);

        assertThatThrownBy(() -> admin.rechazar(7L, ID_ADMIN, "   "))
                .isInstanceOf(OrganizacionException.class)
                .satisfies(e -> assertThat(((OrganizacionException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(o.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE);
        verify(revisionRepository, never()).save(any());
    }

    @Test
    @DisplayName("rechazar guarda el motivo, lo registra en el historial y avisa al representante")
    void rechazarConMotivo() {
        Organizacion o = organizacion(7, EstadoVerificacion.PENDIENTE);

        OrganizacionAdminDTO resultado = admin.rechazar(7L, ID_ADMIN, "  Falta la personería jurídica ");

        assertThat(o.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.RECHAZADA);
        assertThat(o.getNotaRevision()).isEqualTo("Falta la personería jurídica");
        assertThat(resultado.notaRevision()).isEqualTo("Falta la personería jurídica");

        ArgumentCaptor<RevisionOrganizacion> revision = ArgumentCaptor.forClass(RevisionOrganizacion.class);
        verify(revisionRepository).save(revision.capture());
        assertThat(revision.getValue().getAccion()).isEqualTo(RevisionOrganizacion.RECHAZADA);
        assertThat(revision.getValue().getIdUsuario()).isEqualTo(ID_ADMIN);
        assertThat(revision.getValue().getNota()).isEqualTo("Falta la personería jurídica");
        assertThat(revision.getValue().getFecha()).isNotNull();

        verify(notificacionService).crearNotificacion(eq(ID_REPRESENTANTE), isNull(), isNull(),
                eq(Notificacion.ORGANIZACION_RECHAZADA), contains("Falta la personería jurídica"));
        verify(emailService).enviar(eq("patitas@resq.com"), anyString(), contains("rechazada"));
    }

    @Test
    @DisplayName("aprobar acepta una nota opcional, registra APROBADA y avisa que ya puede gestionar casos")
    void aprobar() {
        Organizacion o = organizacion(8, EstadoVerificacion.PENDIENTE);

        admin.aprobar(8L, ID_ADMIN, null);

        assertThat(o.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.VERIFICADA);
        assertThat(o.getNotaRevision()).isNull();
        ArgumentCaptor<RevisionOrganizacion> revision = ArgumentCaptor.forClass(RevisionOrganizacion.class);
        verify(revisionRepository).save(revision.capture());
        assertThat(revision.getValue().getAccion()).isEqualTo(RevisionOrganizacion.APROBADA);
        verify(notificacionService).crearNotificacion(eq(ID_REPRESENTANTE), isNull(), isNull(),
                eq(Notificacion.ORGANIZACION_VERIFICADA), contains("gestionar casos"));
    }

    @Test
    @DisplayName("si la notificacion falla, la decision igual queda guardada")
    void avisoFallido() {
        Organizacion o = organizacion(9, EstadoVerificacion.PENDIENTE);
        when(notificacionService.crearNotificacion(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("sin websocket"));

        admin.aprobar(9L, ID_ADMIN, "Documentación en regla");

        assertThat(o.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.VERIFICADA);
        verify(revisionRepository).save(any());
    }

    @Test
    @DisplayName("el historial devuelve cada accion con quien la hizo, lo mas reciente primero")
    void historial() {
        organizacion(7, EstadoVerificacion.PENDIENTE);
        when(revisionRepository.findByIdOrganizacionOrderByFechaDescIdRevisionDesc(7L)).thenReturn(List.of(
                new RevisionOrganizacion(7L, RevisionOrganizacion.REENVIADA, null, ID_REPRESENTANTE,
                        LocalDateTime.of(2026, 10, 22, 9, 0)),
                new RevisionOrganizacion(7L, RevisionOrganizacion.RECHAZADA, "Falta documento", ID_ADMIN,
                        LocalDateTime.of(2026, 10, 21, 18, 0))));

        List<RevisionOrganizacionDTO> historial = admin.historial(7L);

        assertThat(historial).extracting(RevisionOrganizacionDTO::accion)
                .containsExactly(RevisionOrganizacion.REENVIADA, RevisionOrganizacion.RECHAZADA);
        assertThat(historial).extracting(RevisionOrganizacionDTO::nombreUsuario)
                .containsExactly("Ana Refugio", "Admin Demo");
        assertThat(historial.get(1).nota()).isEqualTo("Falta documento");
    }

    // ---------------------------------------------------------------- reenviar

    @Test
    @DisplayName("una organizacion rechazada se reenvia: vuelve a pendiente y queda en el historial")
    void reenviar() {
        Organizacion o = organizacion(7, EstadoVerificacion.RECHAZADA);

        organizaciones.reenviar(7L, ID_REPRESENTANTE);

        assertThat(o.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE);
        ArgumentCaptor<RevisionOrganizacion> revision = ArgumentCaptor.forClass(RevisionOrganizacion.class);
        verify(revisionRepository).save(revision.capture());
        assertThat(revision.getValue().getAccion()).isEqualTo(RevisionOrganizacion.REENVIADA);
        assertThat(revision.getValue().getIdUsuario()).isEqualTo(ID_REPRESENTANTE);
    }

    @Test
    @DisplayName("solo el representante puede reenviar (403)")
    void reenviarAjena() {
        organizacion(7, EstadoVerificacion.RECHAZADA);

        assertThatThrownBy(() -> organizaciones.reenviar(7L, 99L))
                .isInstanceOf(OrganizacionException.class)
                .satisfies(e -> assertThat(((OrganizacionException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("solo se reenvia una organizacion rechazada (409)")
    void reenviarNoRechazada() {
        organizacion(7, EstadoVerificacion.PENDIENTE);

        assertThatThrownBy(() -> organizaciones.reenviar(7L, ID_REPRESENTANTE))
                .isInstanceOf(OrganizacionException.class)
                .satisfies(e -> assertThat(((OrganizacionException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(revisionRepository, never()).save(any());
    }
}
