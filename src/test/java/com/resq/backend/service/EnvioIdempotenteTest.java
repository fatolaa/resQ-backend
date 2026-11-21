package com.resq.backend.service;
import com.resq.backend.entity.*;
import com.resq.backend.repository.*;
import com.resq.backend.websocket.EventoWebSocketService;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.web.server.ResponseStatusException;

class EnvioIdempotenteTest {
    @Test void reintentoDevuelveMensajeSinGuardarNiPublicarOtraVez() {
        var mensajes=mock(MensajeRepository.class); var usuarios=mock(UsuarioRepository.class);
        var archivos=mock(AdjuntoMensajeStorageService.class); var eventos=mock(EventoWebSocketService.class);
        var service=new ConversacionService(mensajes,usuarios,mock(OrganizacionRepository.class),archivos,eventos);
        String clave="12345678-1234-1234-1234-123456789abc";
        Mensaje m=new Mensaje(); m.setIdMensaje(4L); m.setIdEmisor(1L); m.setIdReceptor(2L);
        when(usuarios.existsById(anyLong())).thenReturn(true);
        when(usuarios.bloquearParaEnvio(1L)).thenReturn(Optional.of(new Usuario()));
        when(mensajes.findByIdEmisorAndClaveEnvio(1L,clave)).thenReturn(Optional.of(m));
        assertEquals(4L,service.enviarMensaje(1L,2L,"hola",null,clave).idMensaje());
        verify(mensajes,never()).save(any()); verifyNoInteractions(archivos,eventos);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.enviarMensaje(1L,3L,"hola",null,clave)).getStatusCode().value());
    }
}
