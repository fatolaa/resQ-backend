package com.resq.backend.service;

import com.resq.backend.dto.DenunciaRequestDTO;
import com.resq.backend.dto.ResolucionDenunciaDTO;
import com.resq.backend.entity.DenunciaMensaje;
import com.resq.backend.entity.EstadoDenuncia;
import com.resq.backend.entity.Mensaje;
import com.resq.backend.exception.ChatException;
import com.resq.backend.repository.DenunciaMensajeRepository;
import com.resq.backend.repository.MensajeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DenunciaService: lógica de denuncias de mensajes en el chat (HU-40)")
class DenunciaServiceTest {

    private DenunciaMensajeRepository denunciaRepository;
    private MensajeRepository mensajeRepository;
    private DenunciaService servicio;

    @BeforeEach
    void setUp() {
        denunciaRepository = mock(DenunciaMensajeRepository.class);
        mensajeRepository = mock(MensajeRepository.class);
        servicio = new DenunciaService(denunciaRepository, mensajeRepository);
    }

    @Test
    @DisplayName("denunciar un mensaje recibido exitosamente")
    void denunciarMensajeExitoso() {
        Mensaje m = new Mensaje();
        m.setIdMensaje(100L);
        m.setIdEmisor(2L);
        m.setIdReceptor(1L);
        m.setContenido("Texto ofensivo");

        when(mensajeRepository.findById(100L)).thenReturn(Optional.of(m));
        when(denunciaRepository.existsByIdMensajeAndIdDenunciante(100L, 1L)).thenReturn(false);
        when(denunciaRepository.save(any(DenunciaMensaje.class))).thenAnswer(i -> {
            DenunciaMensaje d = i.getArgument(0);
            d.setIdDenuncia(1L);
            d.setEstado(EstadoDenuncia.PENDIENTE);
            return d;
        });

        DenunciaRequestDTO dto = new DenunciaRequestDTO(100L, "Lenguaje inapropiado");
        DenunciaMensaje resultado = servicio.denunciar(1L, dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdDenunciante()).isEqualTo(1L);
        assertThat(resultado.getIdDenunciado()).isEqualTo(2L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoDenuncia.PENDIENTE);
        assertThat(resultado.getMotivo()).isEqualTo("Lenguaje inapropiado");
        verify(denunciaRepository).save(any(DenunciaMensaje.class));
    }

    @Test
    @DisplayName("denunciar mensaje inexistente lanza NOT_FOUND")
    void noPuedeDenunciarMensajeInexistente() {
        when(mensajeRepository.findById(999L)).thenReturn(Optional.empty());

        DenunciaRequestDTO dto = new DenunciaRequestDTO(999L, "Motivo");
        assertThatThrownBy(() -> servicio.denunciar(1L, dto))
                .isInstanceOfSatisfying(ChatException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(denunciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("no puede denunciar mensaje de conversacion ajena (FORBIDDEN)")
    void noPuedeDenunciarMensajeAjeno() {
        Mensaje m = new Mensaje();
        m.setIdMensaje(100L);
        m.setIdEmisor(2L);
        m.setIdReceptor(3L);

        when(mensajeRepository.findById(100L)).thenReturn(Optional.of(m));

        DenunciaRequestDTO dto = new DenunciaRequestDTO(100L, "Motivo");
        assertThatThrownBy(() -> servicio.denunciar(1L, dto))
                .isInstanceOfSatisfying(ChatException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(denunciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("no puede denunciar dos veces el mismo mensaje (CONFLICT)")
    void noPuedeDenunciarDosVeces() {
        Mensaje m = new Mensaje();
        m.setIdMensaje(100L);
        m.setIdEmisor(2L);
        m.setIdReceptor(1L);

        when(mensajeRepository.findById(100L)).thenReturn(Optional.of(m));
        when(denunciaRepository.existsByIdMensajeAndIdDenunciante(100L, 1L)).thenReturn(true);

        DenunciaRequestDTO dto = new DenunciaRequestDTO(100L, "Motivo");
        assertThatThrownBy(() -> servicio.denunciar(1L, dto))
                .isInstanceOfSatisfying(ChatException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(denunciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("listar denuncias filtrando por estado o todas")
    void listarDenuncias() {
        DenunciaMensaje d1 = new DenunciaMensaje(100L, 1L, 2L, "Acoso");
        d1.setEstado(EstadoDenuncia.PENDIENTE);

        when(denunciaRepository.findByEstadoOrderByFechaDenunciaDesc(EstadoDenuncia.PENDIENTE))
                .thenReturn(List.of(d1));
        when(denunciaRepository.findAllByOrderByFechaDenunciaDesc())
                .thenReturn(List.of(d1));

        List<DenunciaMensaje> pendientes = servicio.listarDenuncias(EstadoDenuncia.PENDIENTE);
        assertThat(pendientes).hasSize(1);

        List<DenunciaMensaje> todas = servicio.listarDenuncias(null);
        assertThat(todas).hasSize(1);
    }

    @Test
    @DisplayName("admin resuelve denuncia pendiente exitosamente")
    void resolverDenunciaExitosa() {
        DenunciaMensaje d = new DenunciaMensaje(100L, 1L, 2L, "Acoso");
        d.setIdDenuncia(5L);
        d.setEstado(EstadoDenuncia.PENDIENTE);

        when(denunciaRepository.findById(5L)).thenReturn(Optional.of(d));
        when(denunciaRepository.save(any(DenunciaMensaje.class))).thenAnswer(i -> i.getArgument(0));

        ResolucionDenunciaDTO resolucion = new ResolucionDenunciaDTO("REVISADA", "Usuario advertido");
        DenunciaMensaje resuelta = servicio.resolver(5L, 99L, resolucion);

        assertThat(resuelta.getEstado()).isEqualTo(EstadoDenuncia.REVISADA);
        assertThat(resuelta.getNotaAdmin()).isEqualTo("Usuario advertido");
        assertThat(resuelta.getIdAdminRevisor()).isEqualTo(99L);
        assertThat(resuelta.getFechaResolucion()).isNotNull();
        verify(denunciaRepository).save(d);
    }

    @Test
    @DisplayName("no puede resolver denuncia que no esta PENDIENTE (CONFLICT)")
    void resolverDenunciaYaResueltaLanzaConflict() {
        DenunciaMensaje d = new DenunciaMensaje(100L, 1L, 2L, "Acoso");
        d.setIdDenuncia(5L);
        d.setEstado(EstadoDenuncia.REVISADA);

        when(denunciaRepository.findById(5L)).thenReturn(Optional.of(d));

        ResolucionDenunciaDTO resolucion = new ResolucionDenunciaDTO("DESCARTADA", "Nota");
        assertThatThrownBy(() -> servicio.resolver(5L, 99L, resolucion))
                .isInstanceOfSatisfying(ChatException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(denunciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("no puede denunciar su propio mensaje (BAD_REQUEST)")
    void noPuedeDenunciarMensajePropio() {
        Mensaje m = new Mensaje();
        m.setIdMensaje(100L);
        m.setIdEmisor(1L);
        m.setIdReceptor(2L);

        when(mensajeRepository.findById(100L)).thenReturn(Optional.of(m));

        DenunciaRequestDTO dto = new DenunciaRequestDTO(100L, "Motivo");
        assertThatThrownBy(() -> servicio.denunciar(1L, dto))
                .isInstanceOfSatisfying(ChatException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(denunciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("DataIntegrityViolationException al guardar denuncia devuelve CONFLICT")
    void denunciaRaceConditionDevuelveConflict() {
        Mensaje m = new Mensaje();
        m.setIdMensaje(100L);
        m.setIdEmisor(2L);
        m.setIdReceptor(1L);

        when(mensajeRepository.findById(100L)).thenReturn(Optional.of(m));
        when(denunciaRepository.existsByIdMensajeAndIdDenunciante(100L, 1L)).thenReturn(false);
        when(denunciaRepository.save(any(DenunciaMensaje.class)))
                .thenThrow(new DataIntegrityViolationException("uk_denuncia_mensaje_denunciante"));

        DenunciaRequestDTO dto = new DenunciaRequestDTO(100L, "Motivo");
        assertThatThrownBy(() -> servicio.denunciar(1L, dto))
                .isInstanceOfSatisfying(ChatException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }
}
