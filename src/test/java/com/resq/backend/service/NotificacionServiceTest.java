package com.resq.backend.service;

import com.resq.backend.entity.Notificacion;
import com.resq.backend.exception.NotificacionException;
import com.resq.backend.repository.NotificacionRepository;
import com.resq.backend.websocket.EventoWebSocketService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("NotificacionService: bandeja de notificaciones (HU-37)")
class NotificacionServiceTest {

    private NotificacionRepository repositorio;
    private EventoWebSocketService eventos;
    private NotificacionService servicio;

    @BeforeEach
    void setUp() {
        repositorio = mock(NotificacionRepository.class);
        eventos = mock(EventoWebSocketService.class);
        servicio = new NotificacionService(repositorio, eventos);
    }

    private static Notificacion notificacion(Long idUsuario) {
        Notificacion n = new Notificacion(idUsuario, 55L, null, Notificacion.SOLICITUD_APROBADA, "Tu reporte fue aprobado.");
        n.setIdNotificacion(7L);
        n.setFechaCreacion(LocalDateTime.now());
        return n;
    }

    @Test
    @DisplayName("lista las notificaciones sin archivar del usuario")
    void listarSinArchivadas() {
        when(repositorio.findByIdUsuarioAndArchivadaFalseOrderByFechaCreacionDesc(10L))
                .thenReturn(List.of(notificacion(10L)));

        List<Notificacion> lista = servicio.listar(10L);

        assertThat(lista).hasSize(1);
        verify(repositorio).findByIdUsuarioAndArchivadaFalseOrderByFechaCreacionDesc(10L);
    }

    @Test
    @DisplayName("marca como leida una notificacion propia")
    void marcaLeida() {
        Notificacion original = notificacion(10L);
        when(repositorio.findById(7L)).thenReturn(Optional.of(original));
        when(repositorio.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));

        Notificacion actualizada = servicio.marcarLeida(7L, 10L, true);

        assertThat(actualizada.isLeida()).isTrue();
        verify(repositorio).save(original);
    }

    @Test
    @DisplayName("marca como no leida una notificacion propia")
    void marcaNoLeida() {
        Notificacion original = notificacion(10L);
        original.setLeida(true);
        when(repositorio.findById(7L)).thenReturn(Optional.of(original));
        when(repositorio.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));

        Notificacion actualizada = servicio.marcarLeida(7L, 10L, false);

        assertThat(actualizada.isLeida()).isFalse();
    }

    @Test
    @DisplayName("no permite tocar notificaciones de otro usuario")
    void noPermiteAjenas() {
        when(repositorio.findById(7L)).thenReturn(Optional.of(notificacion(10L)));

        assertThatThrownBy(() -> servicio.marcarLeida(7L, 99L, true))
                .isInstanceOfSatisfying(NotificacionException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("responde 404 cuando la notificacion no existe")
    void noExiste() {
        when(repositorio.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.archivar(7L, 10L))
                .isInstanceOfSatisfying(NotificacionException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("archivar la saca de la bandeja y la deja como leida")
    void archiva() {
        Notificacion original = notificacion(10L);
        when(repositorio.findById(7L)).thenReturn(Optional.of(original));
        when(repositorio.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));

        Notificacion archivar = servicio.archivar(7L, 10L);

        assertThat(archivar.isArchivada()).isTrue();
        assertThat(archivar.isLeida()).isTrue();
        verify(repositorio).save(original);
    }

    @Test
    @DisplayName("crear una notificacion de sistema la deja sin leer y avisa por WebSocket")
    void creaNotificacionDeSistema() {
        when(repositorio.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));

        Notificacion creada = servicio.crearNotificacion(10L, "Asunto", "Mensaje");

        assertThat(creada.getTipo()).isEqualTo(Notificacion.SISTEMA);
        assertThat(creada.getMensaje()).isEqualTo("Mensaje");
        assertThat(creada.isLeida()).isFalse();
        assertThat(creada.isArchivada()).isFalse();
        verify(eventos).aUsuario(eq(10L), any());
    }

    @Test
    @DisplayName("crear una notificacion con origen guarda reporte y solicitud")
    void creaNotificacionConOrigen() {
        when(repositorio.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));

        Notificacion creada = servicio.crearNotificacion(10L, 55L, 12L,
                Notificacion.SOLICITUD_RECHAZADA, "Tu solicitud fue rechazada.");

        assertThat(creada.getIdReporte()).isEqualTo(55L);
        assertThat(creada.getIdSolicitud()).isEqualTo(12L);
        assertThat(creada.getTipo()).isEqualTo(Notificacion.SOLICITUD_RECHAZADA);
        verify(repositorio).save(any(Notificacion.class));
        verify(eventos).aUsuario(eq(10L), any());
    }

    @Test
    @DisplayName("usa el asunto como mensaje cuando no llega mensaje")
    void crearNotificacionUsaElAsuntoSiNoHayMensaje() {
        when(repositorio.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));

        Notificacion creada = servicio.crearNotificacion(10L, "Solo asunto", null);

        assertThat(creada.getMensaje()).isEqualTo("Solo asunto");
        verify(repositorio).save(any(Notificacion.class));
        verify(eventos).aUsuario(eq(10L), any());
    }
}
