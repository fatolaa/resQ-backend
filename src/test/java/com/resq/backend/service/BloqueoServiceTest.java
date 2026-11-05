package com.resq.backend.service;

import com.resq.backend.dto.BloqueoDTO;
import com.resq.backend.entity.BloqueoUsuario;
import com.resq.backend.entity.Usuario;
import com.resq.backend.exception.ChatException;
import com.resq.backend.repository.BloqueoUsuarioRepository;
import com.resq.backend.repository.UsuarioRepository;
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
import static org.mockito.Mockito.*;

@DisplayName("BloqueoService: lógica de bloqueo y desbloqueo de usuarios (HU-40)")
class BloqueoServiceTest {

    private BloqueoUsuarioRepository bloqueoRepository;
    private UsuarioRepository usuarioRepository;
    private BloqueoService servicio;

    @BeforeEach
    void setUp() {
        bloqueoRepository = mock(BloqueoUsuarioRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        servicio = new BloqueoService(bloqueoRepository, usuarioRepository);
    }

    @Test
    @DisplayName("bloquear a otro usuario exitosamente")
    void bloquearUsuarioExitoso() {
        Usuario destino = new Usuario();
        destino.setIdUsuario(2L);
        destino.setNombre("Carlos Lopez");

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(destino));
        when(bloqueoRepository.existsByIdBloqueadorAndIdBloqueado(1L, 2L)).thenReturn(false);
        when(bloqueoRepository.save(any(BloqueoUsuario.class))).thenAnswer(i -> {
            BloqueoUsuario b = i.getArgument(0);
            b.setIdBloqueo(10L);
            return b;
        });

        BloqueoUsuario resultado = servicio.bloquear(1L, 2L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdBloqueador()).isEqualTo(1L);
        assertThat(resultado.getIdBloqueado()).isEqualTo(2L);
        verify(bloqueoRepository).save(any(BloqueoUsuario.class));
    }

    @Test
    @DisplayName("no permite bloquearse a si mismo (BAD_REQUEST)")
    void noPermiteBloquearseASiMismo() {
        assertThatThrownBy(() -> servicio.bloquear(1L, 1L))
                .isInstanceOfSatisfying(ChatException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(bloqueoRepository, never()).save(any());
    }

    @Test
    @DisplayName("no permite bloquear usuario inexistente (NOT_FOUND)")
    void noPuedeBloquearUsuarioInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.bloquear(1L, 99L))
                .isInstanceOfSatisfying(ChatException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(bloqueoRepository, never()).save(any());
    }

    @Test
    @DisplayName("no permite duplicar bloqueo ya existente (CONFLICT)")
    void noPermiteDuplicarBloqueo() {
        Usuario destino = new Usuario();
        destino.setIdUsuario(2L);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(destino));
        when(bloqueoRepository.existsByIdBloqueadorAndIdBloqueado(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.bloquear(1L, 2L))
                .isInstanceOfSatisfying(ChatException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(bloqueoRepository, never()).save(any());
    }

    @Test
    @DisplayName("desbloquear a un usuario previamente bloqueado")
    void desbloquearExitoso() {
        BloqueoUsuario existente = new BloqueoUsuario(1L, 2L);
        existente.setIdBloqueo(5L);

        when(bloqueoRepository.findByIdBloqueadorAndIdBloqueado(1L, 2L)).thenReturn(Optional.of(existente));

        servicio.desbloquear(1L, 2L);

        verify(bloqueoRepository).delete(existente);
    }

    @Test
    @DisplayName("desbloquear usuario no bloqueado lanza NOT_FOUND")
    void desbloquearInexistenteLanza404() {
        when(bloqueoRepository.findByIdBloqueadorAndIdBloqueado(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.desbloquear(1L, 2L))
                .isInstanceOfSatisfying(ChatException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(bloqueoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("listar usuarios bloqueados con sus nombres")
    void listarBloqueadosDevuelveLista() {
        BloqueoUsuario b1 = new BloqueoUsuario(1L, 2L);
        b1.setIdBloqueo(10L);
        b1.setFechaBloqueo(LocalDateTime.now());

        Usuario u2 = new Usuario();
        u2.setIdUsuario(2L);
        u2.setNombre("Carlos");

        when(bloqueoRepository.findByIdBloqueadorOrderByFechaBloqueoDesc(1L)).thenReturn(List.of(b1));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(u2));

        List<BloqueoDTO> resultado = servicio.listarBloqueados(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombreBloqueado()).isEqualTo("Carlos");
        assertThat(resultado.get(0).idBloqueado()).isEqualTo(2L);
    }

    @Test
    @DisplayName("estaBloqueado devuelve true o false segun BD")
    void estaBloqueadoConsulta() {
        when(bloqueoRepository.existsByIdBloqueadorAndIdBloqueado(2L, 1L)).thenReturn(true);
        when(bloqueoRepository.existsByIdBloqueadorAndIdBloqueado(3L, 1L)).thenReturn(false);

        assertThat(servicio.estaBloqueado(2L, 1L)).isTrue();
        assertThat(servicio.estaBloqueado(3L, 1L)).isFalse();
    }
}
