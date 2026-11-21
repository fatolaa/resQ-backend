package com.resq.backend.service;

import com.resq.backend.dto.AbrirConversacionRequestDTO;
import com.resq.backend.entity.Mensaje;
import com.resq.backend.repository.*;
import com.resq.backend.websocket.EventoWebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConversacionBloqueosTest {
    private final MensajeRepository mensajes = mock(MensajeRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
    private final AdjuntoMensajeStorageService archivos = mock(AdjuntoMensajeStorageService.class);
    private final EventoWebSocketService eventos = mock(EventoWebSocketService.class);
    private final BloqueoService bloqueos = mock(BloqueoService.class);
    private final ConversacionService servicio = new ConversacionService(mensajes, usuarios, organizaciones, archivos, eventos, bloqueos);

    @BeforeEach void preparar() { when(usuarios.existsById(2L)).thenReturn(true); }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void bloqueoEnCualquierDireccionImpideAbrirYEnviar(boolean saliente) {
        when(bloqueos.estaBloqueado(saliente ? 1L : 2L, saliente ? 2L : 1L)).thenReturn(true);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> servicio.abrirConversacion(1L, new AbrirConversacionRequestDTO(2L, null))).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> servicio.enviarMensaje(1L, 2L, "Hola", null)).getStatusCode().value());
        verifyNoInteractions(mensajes, archivos, eventos);
    }

    @Test void permiteEnviarTrasDesbloquear() {
        when(bloqueos.estaBloqueado(2L, 1L)).thenReturn(true, false);
        assertThrows(ResponseStatusException.class, () -> servicio.enviarMensaje(1L, 2L, "Hola", null));
        when(mensajes.save(any(Mensaje.class))).thenAnswer(i -> { Mensaje m=i.getArgument(0); m.setIdMensaje(9L); return m; });
        assertEquals(9L, servicio.enviarMensaje(1L, 2L, "Hola", null).idMensaje());
        verify(mensajes, times(1)).save(any(Mensaje.class));
        verify(eventos, times(1)).aUsuario(eq(2L), any());
    }
}
