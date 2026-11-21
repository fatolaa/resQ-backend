package com.resq.backend.service;

import com.resq.backend.dto.ConversacionResumenDTO;
import com.resq.backend.entity.Mensaje;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.*;
import com.resq.backend.websocket.EventoWebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/** TT-50: busqueda, archivado y separacion de la bandeja. */
class BandejaConversacionesTest {

    private final MensajeRepository mensajes = mock(MensajeRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
    private final AdjuntoMensajeStorageService archivos = mock(AdjuntoMensajeStorageService.class);
    private final EventoWebSocketService eventos = mock(EventoWebSocketService.class);
    private final BloqueoService bloqueos = mock(BloqueoService.class);
    private final ConversacionService servicio =
            new ConversacionService(mensajes, usuarios, organizaciones, archivos, eventos, bloqueos);

    private static Mensaje mensaje(long emisor, long receptor, String texto, int minuto,
                                   boolean archivadoEmisor, boolean archivadoReceptor) {
        Mensaje m = new Mensaje();
        m.setIdEmisor(emisor);
        m.setIdReceptor(receptor);
        m.setContenido(texto);
        m.setTipo(Mensaje.TEXTO);
        m.setFechaEnvio(LocalDateTime.of(2026, 11, 10, 12, minuto));
        m.setArchivadoEmisor(archivadoEmisor);
        m.setArchivadoReceptor(archivadoReceptor);
        return m;
    }

    private static Usuario usuario(long id, String nombre) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setNombre(nombre);
        return u;
    }

    @BeforeEach
    void preparar() {
        when(usuarios.existsById(2L)).thenReturn(true);
        when(usuarios.findById(2L)).thenReturn(java.util.Optional.of(usuario(2L, "Ana Torres")));
        when(usuarios.findById(3L)).thenReturn(java.util.Optional.of(usuario(3L, "Luis Perez")));
        when(usuarios.findById(4L)).thenReturn(java.util.Optional.of(usuario(4L, "Carla Ruiz")));
        when(organizaciones.findByIdRepresentante(anyLong())).thenReturn(java.util.Optional.empty());
        // orden DESC como findTodosDeUsuario
        when(mensajes.findTodosDeUsuario(1L)).thenReturn(List.of(
                mensaje(2L, 1L, "Hola, gracias por adoptar", 30, false, false),
                mensaje(1L, 3L, "Te espero en el refugio", 20, true, false),
                mensaje(1L, 4L, "Mensaje viejo", 10, true, false)));
        when(mensajes.findConversacion(1L, 2L)).thenReturn(List.of(
                mensaje(1L, 2L, "Hola", 5, false, false),
                mensaje(2L, 1L, "Hola, gracias por adoptar", 30, false, false)));
        when(mensajes.saveAll(anyList())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void buscaPorNombreDelOtroParticipante() {
        List<ConversacionResumenDTO> resultado = servicio.obtenerConversaciones(1L, "ana", null);
        assertEquals(List.of(2L), ids(resultado));
    }

    @Test
    void buscaPorContenidoDeCualquierMensaje() {
        List<ConversacionResumenDTO> resultado = servicio.obtenerConversaciones(1L, "REFUGIO", null);
        assertEquals(List.of(3L), ids(resultado));
    }

    @Test
    void separaBandejaActivaDeArchivadas() {
        assertEquals(List.of(2L), ids(servicio.obtenerConversaciones(1L, null, false)));
        assertEquals(List.of(3L, 4L), ids(servicio.obtenerConversaciones(1L, null, true)));
    }

    @Test
    void archivaSoloElLadoDelUsuarioYConservaElHistorial() {
        servicio.archivarConversacion(1L, 2L, true);

        verify(mensajes).saveAll(anyList());
        assertTrue(mensajes.findConversacion(1L, 2L).get(0).isArchivadoEmisor());
        assertTrue(mensajes.findConversacion(1L, 2L).get(1).isArchivadoReceptor());
        verify(mensajes, never()).delete(any(Mensaje.class));
        verify(mensajes, never()).deleteAll(anyList());
    }

    @Test
    void noArchivaConversacionesAjenasNiInexistentes() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> servicio.archivarConversacion(1L, 1L, true)).getStatusCode().value());

        when(usuarios.existsById(99L)).thenReturn(false);
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> servicio.archivarConversacion(1L, 99L, true)).getStatusCode().value());

        when(usuarios.existsById(9L)).thenReturn(true);
        when(mensajes.findConversacion(1L, 9L)).thenReturn(List.of());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> servicio.archivarConversacion(1L, 9L, true)).getStatusCode().value());

        verify(mensajes, never()).saveAll(anyList());
    }

    private static List<Long> ids(List<ConversacionResumenDTO> conversaciones) {
        return conversaciones.stream().map(ConversacionResumenDTO::idConversacion).toList();
    }
}
