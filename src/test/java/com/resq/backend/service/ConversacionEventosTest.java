package com.resq.backend.service;

import com.resq.backend.dto.MensajeEliminadoEventoDTO;
import com.resq.backend.dto.MensajeNuevoEventoDTO;
import com.resq.backend.entity.Mensaje;
import com.resq.backend.repository.MensajeRepository;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.websocket.EventoWebSocket;
import com.resq.backend.websocket.EventoWebSocketService;
import com.resq.backend.websocket.TipoEventoWebSocket;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConversacionEventosTest {

    private final MensajeRepository mensajes = mock(MensajeRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final EventoWebSocketService eventos = mock(EventoWebSocketService.class);
    private final ConversacionService servicio = new ConversacionService(
            mensajes,
            usuarios,
            mock(OrganizacionRepository.class),
            mock(AdjuntoMensajeStorageService.class),
            eventos,
            mock(BloqueoService.class));

    @Test
    void mensajeNuevoSeDifundeAEmisorYReceptorConIdDeConversacionRelativo() {
        when(usuarios.existsById(2L)).thenReturn(true);
        when(mensajes.save(any(Mensaje.class))).thenAnswer(invocacion -> {
            Mensaje guardado = invocacion.getArgument(0);
            guardado.setIdMensaje(15L);
            return guardado;
        });

        servicio.enviarMensaje(1L, 2L, "Hola", null);

        ArgumentCaptor<Long> destinatarios = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<EventoWebSocket> eventosCapturados = ArgumentCaptor.forClass(EventoWebSocket.class);
        verify(eventos, times(2)).aUsuario(destinatarios.capture(), eventosCapturados.capture());

        assertEquals(List.of(2L, 1L), destinatarios.getAllValues());
        assertEquals(List.of(TipoEventoWebSocket.MENSAJE_NUEVO, TipoEventoWebSocket.MENSAJE_NUEVO),
                eventosCapturados.getAllValues().stream().map(EventoWebSocket::tipo).toList());
        assertEquals(1L, ((MensajeNuevoEventoDTO) eventosCapturados.getAllValues().get(0).datos()).idConversacion());
        assertEquals(2L, ((MensajeNuevoEventoDTO) eventosCapturados.getAllValues().get(1).datos()).idConversacion());
    }

    @Test
    void eliminacionSeDifundeAEmisorYReceptorConIdDeConversacionRelativo() {
        Mensaje mensaje = mensaje(15L, 1L, 2L, "Hola");
        when(mensajes.findById(15L)).thenReturn(Optional.of(mensaje));
        when(mensajes.save(mensaje)).thenReturn(mensaje);

        servicio.eliminarMensaje(1L, 15L);

        ArgumentCaptor<Long> destinatarios = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<EventoWebSocket> eventosCapturados = ArgumentCaptor.forClass(EventoWebSocket.class);
        verify(eventos, times(2)).aUsuario(destinatarios.capture(), eventosCapturados.capture());

        assertEquals(List.of(2L, 1L), destinatarios.getAllValues());
        assertEquals(List.of(TipoEventoWebSocket.MENSAJE_ELIMINADO, TipoEventoWebSocket.MENSAJE_ELIMINADO),
                eventosCapturados.getAllValues().stream().map(EventoWebSocket::tipo).toList());
        assertEquals(1L, ((MensajeEliminadoEventoDTO) eventosCapturados.getAllValues().get(0).datos()).idConversacion());
        assertEquals(2L, ((MensajeEliminadoEventoDTO) eventosCapturados.getAllValues().get(1).datos()).idConversacion());
    }

    private static Mensaje mensaje(Long id, Long emisor, Long receptor, String contenido) {
        Mensaje mensaje = new Mensaje();
        mensaje.setIdMensaje(id);
        mensaje.setIdEmisor(emisor);
        mensaje.setIdReceptor(receptor);
        mensaje.setContenido(contenido);
        mensaje.setTipo(Mensaje.TEXTO);
        return mensaje;
    }
}
