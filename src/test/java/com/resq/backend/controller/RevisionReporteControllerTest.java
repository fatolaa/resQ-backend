package com.resq.backend.controller;

import com.resq.backend.dto.ApiError;
import com.resq.backend.dto.DecisionRevisionDTO;
import com.resq.backend.entity.EstadoRevision;
import com.resq.backend.entity.Notificacion;
import com.resq.backend.entity.Reporte;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.NotificacionRepository;
import com.resq.backend.repository.ReporteRepository;
import com.resq.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RevisionReporteController: aceptar o rechazar solicitudes (HU-19)")
class RevisionReporteControllerTest {

    private static final long VOLUNTARIO = 1L;
    private static final long CIUDADANO = 2L;
    private static final long DUENO = 10L;
    private static final long REPORTE = 55L;

    private ReporteRepository reporteRepository;
    private UsuarioRepository usuarioRepository;
    private NotificacionRepository notificacionRepository;
    private RevisionReporteController controller;

    @BeforeEach
    void setUp() {
        reporteRepository = mock(ReporteRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        notificacionRepository = mock(NotificacionRepository.class);
        controller = new RevisionReporteController(reporteRepository, usuarioRepository, notificacionRepository);

        when(usuarioRepository.findById(VOLUNTARIO)).thenReturn(Optional.of(usuario(VOLUNTARIO, "VOLUNTARIO")));
        when(usuarioRepository.findById(CIUDADANO)).thenReturn(Optional.of(usuario(CIUDADANO, "USUARIO")));
        when(reporteRepository.findById(REPORTE)).thenReturn(Optional.of(reportePendiente()));
    }

    private static Usuario usuario(long id, String rol) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setRol(rol);
        return usuario;
    }

    private static Reporte reportePendiente() {
        Reporte reporte = new Reporte();
        reporte.setIdReporte(REPORTE);
        reporte.setIdUsuario(DUENO);
        reporte.setTipoCaso("PERDIDA");
        reporte.setDescripcion("Perro café visto por última vez en la plaza");
        reporte.setEstado("PENDIENTE");
        reporte.setEstadoRevision(EstadoRevision.PENDIENTE);
        return reporte;
    }

    private static DecisionRevisionDTO decision(Long revisor, String accion, String nota) {
        return new DecisionRevisionDTO(revisor, accion, nota);
    }

    @Test
    @DisplayName("el voluntario aprueba un caso pendiente y queda registrado quién y cuándo")
    void apruebaCasoPendiente() {
        ResponseEntity<?> respuesta = controller.decidir(REPORTE, decision(VOLUNTARIO, "APROBAR", null));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Reporte guardado = (Reporte) respuesta.getBody();
        assertThat(guardado.getEstadoRevision()).isEqualTo(EstadoRevision.APROBADO);
        assertThat(guardado.getIdRevisor()).isEqualTo(VOLUNTARIO);
        assertThat(guardado.getFechaRevision()).isNotNull();
        assertThat(guardado.getNotaRevision()).isNull();
        verify(reporteRepository).save(guardado);
    }

    @Test
    @DisplayName("el voluntario rechaza un caso dejando una nota, y la nota queda guardada")
    void rechazaCasoConNota() {
        ResponseEntity<?> respuesta =
                controller.decidir(REPORTE, decision(VOLUNTARIO, "rechazar", "  Foto no corresponde al animal  "));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Reporte guardado = (Reporte) respuesta.getBody();
        assertThat(guardado.getEstadoRevision()).isEqualTo(EstadoRevision.RECHAZADO);
        assertThat(guardado.getNotaRevision()).isEqualTo("Foto no corresponde al animal");
    }

    @Test
    @DisplayName("al aprobar se puede dejar una nota opcional")
    void apruebaConNotaOpcional() {
        ResponseEntity<?> respuesta = controller.decidir(REPORTE, decision(VOLUNTARIO, "APROBAR", "Datos verificados"));

        assertThat(((Reporte) respuesta.getBody()).getNotaRevision()).isEqualTo("Datos verificados");
    }

    @Test
    @DisplayName("al rechazar se notifica al dueño del reporte con el motivo")
    void notificaAlDuenoAlRechazar() {
        controller.decidir(REPORTE, decision(VOLUNTARIO, "RECHAZAR", "Información incompleta"));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion notificacion = captor.getValue();
        assertThat(notificacion.getIdUsuario()).isEqualTo(DUENO);
        assertThat(notificacion.getIdReporte()).isEqualTo(REPORTE);
        assertThat(notificacion.getTipo()).isEqualTo(Notificacion.SOLICITUD_RECHAZADA);
        assertThat(notificacion.getMensaje()).contains("#55").contains("rechazado").contains("Información incompleta");
        assertThat(notificacion.isLeida()).isFalse();
    }

    @Test
    @DisplayName("al aprobar se notifica al dueño del reporte")
    void notificaAlDuenoAlAprobar() {
        controller.decidir(REPORTE, decision(VOLUNTARIO, "APROBAR", null));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getIdUsuario()).isEqualTo(DUENO);
        assertThat(captor.getValue().getTipo()).isEqualTo(Notificacion.SOLICITUD_APROBADA);
        assertThat(captor.getValue().getMensaje()).contains("#55").contains("aprobado");
    }

    @Test
    @DisplayName("un administrador también puede revisar")
    void adminPuedeRevisar() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(usuario(99L, "ADMIN")));

        ResponseEntity<?> respuesta = controller.decidir(REPORTE, decision(99L, "APROBAR", null));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("un reporte anterior a HU-19 (sin estado de revisión) se puede revisar")
    void reporteViejoSeTrataComoPendiente() {
        Reporte viejo = reportePendiente();
        viejo.setEstadoRevision(null);
        when(reporteRepository.findById(REPORTE)).thenReturn(Optional.of(viejo));

        ResponseEntity<?> respuesta = controller.decidir(REPORTE, decision(VOLUNTARIO, "APROBAR", null));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("rechazar sin nota devuelve 400 y no cambia nada")
    void rechazarSinNotaEs400() {
        ResponseEntity<?> respuesta = controller.decidir(REPORTE, decision(VOLUNTARIO, "RECHAZAR", "   "));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(reporteRepository, never()).save(any());
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("una decisión desconocida devuelve 400")
    void decisionInvalidaEs400() {
        ResponseEntity<?> respuesta = controller.decidir(REPORTE, decision(VOLUNTARIO, "TAL_VEZ", null));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((ApiError) respuesta.getBody()).errors()).containsKey("decision");
    }

    @Test
    @DisplayName("una nota de más de 500 caracteres devuelve 400")
    void notaMuyLargaEs400() {
        ResponseEntity<?> respuesta = controller.decidir(REPORTE, decision(VOLUNTARIO, "RECHAZAR", "x".repeat(501)));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("un ciudadano común no puede revisar (403)")
    void ciudadanoNoPuedeRevisar() {
        ResponseEntity<?> respuesta = controller.decidir(REPORTE, decision(CIUDADANO, "APROBAR", null));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(reporteRepository, never()).save(any());
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("sin revisor o con un revisor que no existe devuelve 403")
    void revisorInexistenteEs403() {
        when(usuarioRepository.findById(404L)).thenReturn(Optional.empty());

        assertThat(controller.decidir(REPORTE, decision(null, "APROBAR", null)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(controller.decidir(REPORTE, decision(404L, "APROBAR", null)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("un reporte que no existe devuelve 404")
    void reporteInexistenteEs404() {
        when(reporteRepository.findById(777L)).thenReturn(Optional.empty());

        ResponseEntity<?> respuesta = controller.decidir(777L, decision(VOLUNTARIO, "APROBAR", null));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("una solicitud ya revisada no se puede revisar de nuevo (409)")
    void solicitudYaRevisadaEs409() {
        Reporte yaAprobado = reportePendiente();
        yaAprobado.setEstadoRevision(EstadoRevision.APROBADO);
        when(reporteRepository.findById(REPORTE)).thenReturn(Optional.of(yaAprobado));

        ResponseEntity<?> respuesta = controller.decidir(REPORTE, decision(VOLUNTARIO, "RECHAZAR", "Cambio de opinión"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(yaAprobado.getEstadoRevision()).isEqualTo(EstadoRevision.APROBADO);
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("el voluntario ve la cola de casos pendientes")
    void voluntarioListaPendientes() {
        List<Reporte> cola = List.of(reportePendiente());
        when(reporteRepository.findPendientesDeRevision()).thenReturn(cola);

        ResponseEntity<?> respuesta = controller.listarPendientes(VOLUNTARIO);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(cola);
    }

    @Test
    @DisplayName("un ciudadano común no puede ver la cola de pendientes (403)")
    void ciudadanoNoVeLaCola() {
        ResponseEntity<?> respuesta = controller.listarPendientes(CIUDADANO);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(reporteRepository, never()).findPendientesDeRevision();
    }
}
