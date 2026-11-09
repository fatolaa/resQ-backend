package com.resq.backend.service;

import com.resq.backend.dto.EnviarMensajeCasoDTO;
import com.resq.backend.dto.MensajeCasoDTO;
import com.resq.backend.dto.ParticipanteChatDTO;
import com.resq.backend.entity.MensajeCaso;
import com.resq.backend.entity.Notificacion;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.MensajeCasoRepository;
import com.resq.backend.repository.ReporteRepository;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.websocket.EventoWebSocket;
import com.resq.backend.websocket.EventoWebSocketService;
import com.resq.backend.websocket.ParticipanteCasoService;
import com.resq.backend.websocket.PresenciaService;
import com.resq.backend.websocket.TipoEventoWebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ChatCasoService: chat en tiempo real de un caso (HU-22)")
class ChatCasoServiceTest {

    private static final long CASO = 101L;
    private static final long REPORTANTE = 7L;
    private static final long ORGANIZACION = 20L;
    private static final long AJENO = 99L;

    private MensajeCasoRepository mensajeRepository;
    private ParticipanteCasoService participantes;
    private PresenciaService presencia;
    private EventoWebSocketService eventos;
    private NotificacionService notificaciones;
    private FotoStorageService fotos;
    private ChatCasoService servicio;

    @BeforeEach
    void setUp() {
        mensajeRepository = mock(MensajeCasoRepository.class);
        ReporteRepository reporteRepository = mock(ReporteRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        participantes = mock(ParticipanteCasoService.class);
        presencia = mock(PresenciaService.class);
        eventos = mock(EventoWebSocketService.class);
        notificaciones = mock(NotificacionService.class);
        fotos = mock(FotoStorageService.class);
        servicio = new ChatCasoService(mensajeRepository, reporteRepository, usuarioRepository, participantes,
                presencia, eventos, notificaciones, fotos);

        when(reporteRepository.existsById(CASO)).thenReturn(true);
        when(participantes.participa(REPORTANTE, CASO)).thenReturn(true);
        when(participantes.participa(ORGANIZACION, CASO)).thenReturn(true);
        Map<Long, String> roles = new LinkedHashMap<>();
        roles.put(REPORTANTE, ParticipanteChatDTO.REPORTANTE);
        roles.put(ORGANIZACION, ParticipanteChatDTO.ORGANIZACION);
        when(participantes.participantes(CASO)).thenReturn(roles);

        when(usuarioRepository.findById(REPORTANTE)).thenReturn(Optional.of(usuario(REPORTANTE, "Ana Quiroga")));
        when(usuarioRepository.findAllById(anyIterable())).thenReturn(
                List.of(usuario(REPORTANTE, "Ana Quiroga"), usuario(ORGANIZACION, "Refugio Patitas")));

        AtomicLong ids = new AtomicLong(500);
        when(mensajeRepository.save(any(MensajeCaso.class))).thenAnswer(inv -> {
            MensajeCaso m = inv.getArgument(0);
            m.setIdMensaje(ids.incrementAndGet());
            return m;
        });
        when(presencia.ultimaVez(anyLong())).thenReturn(Optional.empty());
    }

    private static Usuario usuario(long id, String nombre) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setNombre(nombre);
        return u;
    }

    private static HttpStatus estadoDe(Throwable e) {
        return HttpStatus.valueOf(((ResponseStatusException) e).getStatusCode().value());
    }

    // ---------------------------------------------------------------- acceso

    @Test
    @DisplayName("quien no participa del caso no puede leer ni escribir (403)")
    void ajenoNoAccede() {
        assertThatThrownBy(() -> servicio.historial(CASO, AJENO))
                .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> servicio.enviar(CASO, AJENO, new EnviarMensajeCasoDTO("hola", null, null)))
                .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.FORBIDDEN));
        verify(mensajeRepository, never()).save(any());
    }

    @Test
    @DisplayName("un caso inexistente responde 404")
    void casoInexistente() {
        assertThatThrownBy(() -> servicio.historial(555L, REPORTANTE))
                .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---------------------------------------------------------------- enviar

    @Test
    @DisplayName("un texto se guarda, se difunde como chat.message a la sala del caso y lleva el nombre del emisor")
    void enviarTexto() {
        MensajeCasoDTO enviado = servicio.enviar(CASO, REPORTANTE, new EnviarMensajeCasoDTO("  Voy en camino  ", null, null));

        assertThat(enviado.tipo()).isEqualTo(MensajeCaso.TEXTO);
        assertThat(enviado.contenido()).isEqualTo("Voy en camino");
        assertThat(enviado.nombreEmisor()).isEqualTo("Ana Quiroga");
        assertThat(enviado.fechaEnvio()).isNotNull();

        ArgumentCaptor<EventoWebSocket> evento = ArgumentCaptor.forClass(EventoWebSocket.class);
        verify(eventos).aCaso(eq(CASO), evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo(TipoEventoWebSocket.CHAT_MENSAJE);
        assertThat(evento.getValue().datos()).isEqualTo(enviado);
    }

    @Test
    @DisplayName("un texto vacio responde 400")
    void textoVacio() {
        assertThatThrownBy(() -> servicio.enviar(CASO, REPORTANTE, new EnviarMensajeCasoDTO("   ", null, null)))
                .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("con latitud y longitud es una ubicacion, con comentario opcional")
    void enviarUbicacion() {
        MensajeCasoDTO enviado = servicio.enviar(CASO, REPORTANTE, new EnviarMensajeCasoDTO(null, -17.39, -66.15));

        assertThat(enviado.tipo()).isEqualTo(MensajeCaso.UBICACION);
        assertThat(enviado.latitud()).isEqualTo(-17.39);
        assertThat(enviado.longitud()).isEqualTo(-66.15);
        assertThat(enviado.contenido()).isNull();
    }

    @Test
    @DisplayName("una ubicacion incompleta responde 400")
    void ubicacionIncompleta() {
        assertThatThrownBy(() -> servicio.enviar(CASO, REPORTANTE, new EnviarMensajeCasoDTO("aqui", -17.39, null)))
                .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("una foto se guarda con FotoStorageService y queda su URL publica")
    void enviarFoto() {
        MockMultipartFile foto = new MockMultipartFile("foto", "perro.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(fotos.guardar(foto)).thenReturn("abc.jpg");

        MensajeCasoDTO enviado = servicio.enviarFoto(CASO, REPORTANTE, foto, "Así está ahora");

        assertThat(enviado.tipo()).isEqualTo(MensajeCaso.FOTO);
        assertThat(enviado.fotoUrl()).isEqualTo("/api/reportes/fotos/abc.jpg");
        assertThat(enviado.contenido()).isEqualTo("Así está ahora");
    }

    @Test
    @DisplayName("enviar una foto vacia responde 400")
    void fotoVacia() {
        MockMultipartFile vacia = new MockMultipartFile("foto", "x.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> servicio.enviarFoto(CASO, REPORTANTE, vacia, null))
                .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ---------------------------------------------------------------- notificar

    @Test
    @DisplayName("se notifica a los participantes que no estan en linea, no al emisor")
    void notificaAusentes() {
        when(presencia.enLinea(ORGANIZACION)).thenReturn(false);

        servicio.enviar(CASO, REPORTANTE, new EnviarMensajeCasoDTO("¿Pueden venir hoy?", null, null));

        verify(notificaciones).crearNotificacion(eq(ORGANIZACION), eq(CASO), isNull(),
                eq(Notificacion.MENSAJE_CASO), contains("¿Pueden venir hoy?"));
        verify(notificaciones, never()).crearNotificacion(eq(REPORTANTE), any(), any(), any(), any());
    }

    @Test
    @DisplayName("a quien esta en linea no se le llena la bandeja: ya ve el mensaje en vivo")
    void noNotificaEnLinea() {
        when(presencia.enLinea(ORGANIZACION)).thenReturn(true);

        servicio.enviar(CASO, REPORTANTE, new EnviarMensajeCasoDTO("hola", null, null));

        verify(notificaciones, never()).crearNotificacion(anyLong(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("si falla la difusion, el mensaje igual queda guardado")
    void difusionFallida() {
        doThrow(new RuntimeException("sin broker")).when(eventos).aCaso(anyLong(), any());

        MensajeCasoDTO enviado = servicio.enviar(CASO, REPORTANTE, new EnviarMensajeCasoDTO("hola", null, null));

        assertThat(enviado.idMensaje()).isNotNull();
        verify(mensajeRepository).save(any());
    }

    @Test
    @DisplayName("la vista previa de la notificacion acorta los textos largos y describe fotos y ubicaciones")
    void vistaPrevia() {
        LocalDateTime ahora = LocalDateTime.now();
        String largo = "x".repeat(200);
        assertThat(ChatCasoService.vistaPrevia(new MensajeCasoDTO(1L, CASO, 1L, null, "TEXTO", largo, null, null, null, ahora)))
                .hasSize(80).endsWith("…");
        assertThat(ChatCasoService.vistaPrevia(new MensajeCasoDTO(1L, CASO, 1L, null, "FOTO", null, null, null, "/x", ahora)))
                .contains("foto");
        assertThat(ChatCasoService.vistaPrevia(new MensajeCasoDTO(1L, CASO, 1L, null, "UBICACION", null, 1.0, 1.0, null, ahora)))
                .contains("ubicación");
    }

    // ---------------------------------------------------------------- historial y participantes

    @Test
    @DisplayName("el historial devuelve los mensajes en orden con el nombre de cada emisor")
    void historial() {
        MensajeCaso m1 = new MensajeCaso();
        m1.setIdMensaje(1L);
        m1.setIdCaso(CASO);
        m1.setIdEmisor(REPORTANTE);
        m1.setTipo(MensajeCaso.TEXTO);
        m1.setContenido("Hola");
        MensajeCaso m2 = new MensajeCaso();
        m2.setIdMensaje(2L);
        m2.setIdCaso(CASO);
        m2.setIdEmisor(ORGANIZACION);
        m2.setTipo(MensajeCaso.TEXTO);
        m2.setContenido("Vamos en camino");
        when(mensajeRepository.findByIdCasoOrderByFechaEnvioAscIdMensajeAsc(CASO)).thenReturn(List.of(m1, m2));

        List<MensajeCasoDTO> historial = servicio.historial(CASO, ORGANIZACION);

        assertThat(historial).extracting(MensajeCasoDTO::nombreEmisor).containsExactly("Ana Quiroga", "Refugio Patitas");
    }

    @Test
    @DisplayName("los participantes llevan su rol y si estan en linea o su ultima conexion")
    void participantesConPresencia() {
        LocalDateTime ayer = LocalDateTime.now().minusDays(1);
        when(presencia.enLinea(REPORTANTE)).thenReturn(true);
        when(presencia.ultimaVez(ORGANIZACION)).thenReturn(Optional.of(ayer));

        List<ParticipanteChatDTO> lista = servicio.participantes(CASO, REPORTANTE);

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0)).satisfies(p -> {
            assertThat(p.nombre()).isEqualTo("Ana Quiroga");
            assertThat(p.rol()).isEqualTo(ParticipanteChatDTO.REPORTANTE);
            assertThat(p.enLinea()).isTrue();
        });
        assertThat(lista.get(1)).satisfies(p -> {
            assertThat(p.enLinea()).isFalse();
            assertThat(p.ultimaVez()).isEqualTo(ayer);
        });
    }
}
