package com.resq.backend.service;

import com.resq.backend.dto.RechazarSolicitudRequestDTO;
import com.resq.backend.entity.EstadoSolicitudVoluntario;
import com.resq.backend.entity.Notificacion;
import com.resq.backend.entity.SolicitudVoluntario;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.SolicitudVoluntarioRepository;
import com.resq.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitudVoluntarioServiceTest {

    @Mock
    private SolicitudVoluntarioRepository solicitudRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private SolicitudVoluntarioService service;

    private Usuario usuario;
    private Usuario admin;
    private SolicitudVoluntario pendiente;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setNombre("Ana");
        usuario.setEmail("ana@correo.com");
        usuario.setRol("USUARIO");

        admin = new Usuario();
        admin.setIdUsuario(99L);
        admin.setNombre("Admin");
        admin.setRol("ADMIN");

        pendiente = new SolicitudVoluntario();
        pendiente.setIdSolicitud(10L);
        pendiente.setUsuario(usuario);
        pendiente.setTipoAyuda("TRANSPORTE");
        pendiente.setEstado(EstadoSolicitudVoluntario.PENDIENTE);
        pendiente.setFechaSolicitud(LocalDateTime.now());
    }

    @Test
    @DisplayName("crearSolicitud guarda una solicitud PENDIENTE valida")
    void crearSolicitudOk() {
        when(solicitudRepository.findTopByUsuarioAndEstadoOrderByFechaSolicitudDesc(usuario, EstadoSolicitudVoluntario.PENDIENTE))
                .thenReturn(Optional.empty());
        when(solicitudRepository.findTopByUsuarioOrderByFechaSolicitudDesc(usuario)).thenReturn(Optional.empty());
        when(solicitudRepository.save(any(SolicitudVoluntario.class))).thenAnswer(i -> i.getArgument(0));

        var dto = service.crearSolicitud(usuario, "TRANSPORTE", "tengo auto");

        assertEquals("PENDIENTE", dto.estado());
        assertEquals("TRANSPORTE", dto.tipoAyuda());
        verify(solicitudRepository).save(any(SolicitudVoluntario.class));
    }

    @Test
    @DisplayName("crearSolicitud rechaza a quien ya es voluntario")
    void crearSiendoVoluntario() {
        usuario.setRol("VOLUNTARIO");
        assertThrows(IllegalStateException.class, () -> service.crearSolicitud(usuario, "A", null));
    }

    @Test
    @DisplayName("crearSolicitud rechaza duplicar pendiente")
    void crearConPendienteExistente() {
        when(solicitudRepository.findTopByUsuarioAndEstadoOrderByFechaSolicitudDesc(usuario, EstadoSolicitudVoluntario.PENDIENTE))
                .thenReturn(Optional.of(pendiente));
        assertThrows(IllegalStateException.class, () -> service.crearSolicitud(usuario, "A", null));
    }

    @Test
    @DisplayName("bloquea nueva solicitud dentro de los 30 dias del rechazo")
    void bloqueo30Dias() {
        SolicitudVoluntario rechazada = new SolicitudVoluntario();
        rechazada.setUsuario(usuario);
        rechazada.setEstado(EstadoSolicitudVoluntario.RECHAZADA);
        rechazada.setFechaResolucion(LocalDateTime.now().minusDays(10));

        when(solicitudRepository.findTopByUsuarioAndEstadoOrderByFechaSolicitudDesc(usuario, EstadoSolicitudVoluntario.PENDIENTE))
                .thenReturn(Optional.empty());
        when(solicitudRepository.findTopByUsuarioOrderByFechaSolicitudDesc(usuario)).thenReturn(Optional.of(rechazada));

        assertFalse(service.puedeSolicitarVoluntario(usuario));
        assertThrows(IllegalStateException.class, () -> service.crearSolicitud(usuario, "A", null));
    }

    @Test
    @DisplayName("permite solicitar pasados 30 dias del rechazo")
    void sinBloqueoTras30Dias() {
        SolicitudVoluntario rechazada = new SolicitudVoluntario();
        rechazada.setUsuario(usuario);
        rechazada.setEstado(EstadoSolicitudVoluntario.RECHAZADA);
        rechazada.setFechaResolucion(LocalDateTime.now().minusDays(31));

        when(solicitudRepository.findTopByUsuarioOrderByFechaSolicitudDesc(usuario)).thenReturn(Optional.of(rechazada));
        assertTrue(service.puedeSolicitarVoluntario(usuario));
    }

    @Test
    @DisplayName("aprobar cambia el rol a VOLUNTARIO, cierra la solicitud y notifica")
    void aprobarOk() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(pendiente));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(solicitudRepository.save(any(SolicitudVoluntario.class))).thenAnswer(i -> i.getArgument(0));

        var dto = service.aprobarSolicitud(10L, 99L);

        assertEquals("APROBADA", dto.estado());
        assertEquals("VOLUNTARIO", usuario.getRol());
        assertEquals(99L, dto.revisadoPorId());
        assertNotNull(dto.fechaResolucion());
        verify(usuarioRepository).save(usuario);
        verify(notificacionService).crearNotificacion(eq(1L), isNull(), eq(10L),
                eq(Notificacion.SOLICITUD_APROBADA), anyString());
        verify(emailService).enviar(eq("ana@correo.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("aprobar falla si la solicitud ya no esta PENDIENTE")
    void aprobarNoPendiente() {
        pendiente.setEstado(EstadoSolicitudVoluntario.APROBADA);
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(pendiente));
        assertThrows(RuntimeException.class, () -> service.aprobarSolicitud(10L, 99L));
    }

    @Test
    @DisplayName("rechazar guarda el motivo, cierra la solicitud y notifica")
    void rechazarOk() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(pendiente));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(solicitudRepository.save(any(SolicitudVoluntario.class))).thenAnswer(i -> i.getArgument(0));

        var dto = service.rechazarSolicitud(10L, 99L, new RechazarSolicitudRequestDTO("documentacion incompleta"));

        assertEquals("RECHAZADA", dto.estado());
        assertEquals("documentacion incompleta", dto.motivoRechazo());
        assertEquals("USUARIO", usuario.getRol());
        verify(notificacionService).crearNotificacion(eq(1L), isNull(), eq(10L),
                eq(Notificacion.SOLICITUD_RECHAZADA), anyString());
    }

    @Test
    @DisplayName("listar pendientes devuelve los resumenes ordenados")
    void listarPendientes() {
        when(solicitudRepository.findByEstadoOrderByFechaSolicitudAsc(EstadoSolicitudVoluntario.PENDIENTE))
                .thenReturn(List.of(pendiente));
        var lista = service.listarPendientes();
        assertEquals(1, lista.size());
        assertEquals("Ana", lista.get(0).nombreUsuario());
    }
}
