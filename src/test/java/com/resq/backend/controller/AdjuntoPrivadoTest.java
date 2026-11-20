package com.resq.backend.controller;

import com.resq.backend.repository.MensajeRepository;
import com.resq.backend.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdjuntoPrivadoTest {
    private final MensajeRepository mensajes=mock(MensajeRepository.class);
    private final AdjuntoMensajeStorageService archivos=mock(AdjuntoMensajeStorageService.class);
    private final ConversacionController controller=new ConversacionController(mock(ConversacionService.class), archivos, mensajes);
    private final String url="/api/conversaciones/adjuntos/test.png";
    @AfterEach void limpiar(){SecurityContextHolder.clearContext();}
    private void login(){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("1",null,List.of()));}
    @Test void rechazaAnonimo(){
        assertEquals(401,assertThrows(ResponseStatusException.class,()->controller.adjunto("test.png")).getStatusCode().value());
        verifyNoInteractions(archivos,mensajes);
    }
    @Test void rechazaTercerosYAdjuntosEliminados(){
        login();
        assertEquals(404,assertThrows(ResponseStatusException.class,()->controller.adjunto("test.png")).getStatusCode().value());
        verifyNoInteractions(archivos);
    }
    @ParameterizedTest @ValueSource(booleans={true,false}) void permiteAmbosParticipantes(boolean emisor){
        login();
        if(emisor) when(mensajes.existsByUrlAdjuntoAndEliminadoFalseAndIdEmisor(url,1L)).thenReturn(true);
        else when(mensajes.existsByUrlAdjuntoAndEliminadoFalseAndIdReceptor(url,1L)).thenReturn(true);
        when(archivos.cargar("test.png")).thenReturn(Optional.of(new AdjuntoMensajeStorageService.AdjuntoCargado(new ByteArrayResource(new byte[]{1}),"image/png",true)));
        var respuesta=controller.adjunto("test.png");
        assertEquals(200,respuesta.getStatusCode().value());
        assertEquals("no-store",respuesta.getHeaders().getCacheControl());
    }
}
