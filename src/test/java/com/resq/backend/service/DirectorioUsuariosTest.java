package com.resq.backend.service;

import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.*;
import com.resq.backend.websocket.EventoWebSocketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** TT-49: directorio de usuarios para iniciar conversaciones. */
class DirectorioUsuariosTest {

    private final MensajeRepository mensajes = mock(MensajeRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
    private final AdjuntoMensajeStorageService archivos = mock(AdjuntoMensajeStorageService.class);
    private final EventoWebSocketService eventos = mock(EventoWebSocketService.class);
    private final BloqueoService bloqueos = mock(BloqueoService.class);
    private final ConversacionService servicio =
            new ConversacionService(mensajes, usuarios, organizaciones, archivos, eventos, bloqueos);

    private static Usuario usuario(Long id, String nombre, String rol) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setNombre(nombre);
        u.setRol(rol);
        return u;
    }

    @Test
    void devuelveSoloDatosMinimosYOmiteBloqueosEnAmbasDirecciones() {
        when(usuarios.findTop20ByNombreContainingIgnoreCaseAndIdUsuarioNotOrderByNombreAsc("ana", 1L))
                .thenReturn(List.of(usuario(2L, "Ana Torres", "USUARIO"),
                        usuario(3L, "Ana Perez", "VOLUNTARIO"),
                        usuario(4L, "Ana Ruiz", "USUARIO")));
        when(bloqueos.estaBloqueado(1L, 3L)).thenReturn(true);
        when(bloqueos.estaBloqueado(4L, 1L)).thenReturn(true);

        var resultado = servicio.buscarUsuarios(1L, "  ana  ");

        assertEquals(1, resultado.size());
        assertEquals(2L, resultado.get(0).idUsuario());
        assertEquals("Ana Torres", resultado.get(0).nombre());
        assertEquals("USUARIO", resultado.get(0).rol());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "a"})
    void exigeAlMenosDosCaracteres(String filtro) {
        assertTrue(servicio.buscarUsuarios(1L, filtro).isEmpty());
        verifyNoInteractions(usuarios);
    }
}
