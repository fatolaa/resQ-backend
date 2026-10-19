package com.resq.backend.service;

import com.resq.backend.dto.PreferenciaNotificacionRequestDTO;
import com.resq.backend.entity.PreferenciaNotificacion;
import com.resq.backend.repository.PreferenciaNotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PreferenciaNotificacionService: preferencias por defecto y edicion parcial (HU-36a)")
class PreferenciaNotificacionServiceTest {

    private static final long USUARIO = 42L;

    private PreferenciaNotificacionRepository preferenciaRepository;
    private PreferenciaNotificacionService servicio;

    @BeforeEach
    void setUp() {
        preferenciaRepository = mock(PreferenciaNotificacionRepository.class);
        when(preferenciaRepository.save(any(PreferenciaNotificacion.class))).thenAnswer(invocacion -> {
            PreferenciaNotificacion preferencia = invocacion.getArgument(0);
            preferencia.setIdPreferencia(1L);
            return preferencia;
        });
        servicio = new PreferenciaNotificacionService(preferenciaRepository);
    }

    private static PreferenciaNotificacion existente() {
        PreferenciaNotificacion preferencia = new PreferenciaNotificacion(USUARIO);
        preferencia.setIdPreferencia(1L);
        return preferencia;
    }

    // ---------------------------------------------------------------- valores por defecto

    @Test
    @DisplayName("crea las preferencias con todos los eventos activados si el usuario es nuevo")
    void creaConValoresPorDefecto() {
        when(preferenciaRepository.findByIdUsuario(USUARIO)).thenReturn(Optional.empty());

        PreferenciaNotificacion preferencia = servicio.obtenerOCrear(USUARIO);

        assertThat(preferencia.getIdUsuario()).isEqualTo(USUARIO);
        assertThat(preferencia.isNotificarSolicitudAprobada()).isTrue();
        assertThat(preferencia.isNotificarSolicitudRechazada()).isTrue();
        assertThat(preferencia.isNotificarReporteCerca()).isTrue();
        assertThat(preferencia.isNotificarChatMensaje()).isTrue();
        verify(preferenciaRepository).save(any(PreferenciaNotificacion.class));
    }

    @Test
    @DisplayName("si el usuario ya tiene preferencias no crea otra fila")
    void noDuplicaSiYaExisten() {
        when(preferenciaRepository.findByIdUsuario(USUARIO)).thenReturn(Optional.of(existente()));

        servicio.obtenerOCrear(USUARIO);

        verify(preferenciaRepository, never()).save(any(PreferenciaNotificacion.class));
    }

    @Test
    @DisplayName("filtra las preferencias por el id del usuario")
    void buscaPorUsuario() {
        when(preferenciaRepository.findByIdUsuario(USUARIO)).thenReturn(Optional.of(existente()));

        servicio.obtenerOCrear(USUARIO);

        verify(preferenciaRepository).findByIdUsuario(USUARIO);
    }

    // ---------------------------------------------------------------- edicion parcial

    @Test
    @DisplayName("solo cambia los campos enviados y respeta los ausentes (null)")
    void actualizaSoloLoEnviado() {
        when(preferenciaRepository.findByIdUsuario(USUARIO)).thenReturn(Optional.of(existente()));
        PreferenciaNotificacionRequestDTO cambios =
                new PreferenciaNotificacionRequestDTO(false, null, null, null);

        PreferenciaNotificacion preferencia = servicio.actualizar(USUARIO, cambios);

        assertThat(preferencia.isNotificarSolicitudAprobada()).isFalse();
        assertThat(preferencia.isNotificarSolicitudRechazada()).isTrue();
        assertThat(preferencia.isNotificarReporteCerca()).isTrue();
        assertThat(preferencia.isNotificarChatMensaje()).isTrue();
    }

    @Test
    @DisplayName("actualiza varios campos a la vez")
    void actualizaVariosCampos() {
        when(preferenciaRepository.findByIdUsuario(USUARIO)).thenReturn(Optional.of(existente()));
        PreferenciaNotificacionRequestDTO cambios =
                new PreferenciaNotificacionRequestDTO(false, false, true, false);

        PreferenciaNotificacion preferencia = servicio.actualizar(USUARIO, cambios);

        assertThat(preferencia.isNotificarSolicitudAprobada()).isFalse();
        assertThat(preferencia.isNotificarSolicitudRechazada()).isFalse();
        assertThat(preferencia.isNotificarReporteCerca()).isTrue();
        assertThat(preferencia.isNotificarChatMensaje()).isFalse();
    }

    @Test
    @DisplayName("un cuerpo vacio deja las preferencias como estaban")
    void cuerpoVacioNoCambiaNada() {
        when(preferenciaRepository.findByIdUsuario(USUARIO)).thenReturn(Optional.of(existente()));
        PreferenciaNotificacionRequestDTO vacio =
                new PreferenciaNotificacionRequestDTO(null, null, null, null);

        PreferenciaNotificacion preferencia = servicio.actualizar(USUARIO, vacio);

        assertThat(preferencia.isNotificarSolicitudAprobada()).isTrue();
        assertThat(preferencia.isNotificarSolicitudRechazada()).isTrue();
        assertThat(preferencia.isNotificarReporteCerca()).isTrue();
        assertThat(preferencia.isNotificarChatMensaje()).isTrue();
    }

    @Test
    @DisplayName("si el usuario no tenia preferencias, las crea antes de guardar el cambio")
    void creaAntesDeActualizar() {
        when(preferenciaRepository.findByIdUsuario(USUARIO)).thenReturn(Optional.empty());
        PreferenciaNotificacionRequestDTO cambios =
                new PreferenciaNotificacionRequestDTO(false, null, null, null);

        PreferenciaNotificacion preferencia = servicio.actualizar(USUARIO, cambios);

        assertThat(preferencia.getIdUsuario()).isEqualTo(USUARIO);
        assertThat(preferencia.isNotificarSolicitudAprobada()).isFalse();
        // una vez para crearlas por defecto y otra para guardar el cambio
        verify(preferenciaRepository, times(2)).save(any(PreferenciaNotificacion.class));
    }

    @Test
    @DisplayName("guarda el estado final en la base de datos")
    void persisteElEstadoFinal() {
        when(preferenciaRepository.findByIdUsuario(USUARIO)).thenReturn(Optional.of(existente()));
        PreferenciaNotificacionRequestDTO cambios =
                new PreferenciaNotificacionRequestDTO(null, false, null, null);

        servicio.actualizar(USUARIO, cambios);

        ArgumentCaptor<PreferenciaNotificacion> captor = ArgumentCaptor.forClass(PreferenciaNotificacion.class);
        verify(preferenciaRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().isNotificarSolicitudRechazada()).isFalse();
    }
}
