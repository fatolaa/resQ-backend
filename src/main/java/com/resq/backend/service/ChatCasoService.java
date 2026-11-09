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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HU-22: chat en tiempo real de un caso entre sus participantes (reportante, organizacion
 * que lo tomo y voluntario que lo reviso).
 *
 * Cada mensaje se guarda (historial al volver a entrar) y se difunde como chat.message a la
 * sala /topic/casos/{id}. A los participantes que no estan en linea se les deja una
 * notificacion en su bandeja (HU-37), para no llenar la bandeja de quien ya lo ve en vivo.
 */
@Service
public class ChatCasoService {

    private static final Logger logger = LoggerFactory.getLogger(ChatCasoService.class);

    private static final String RUTA_FOTOS = "/api/reportes/fotos/";
    private static final int VISTA_PREVIA_MAX = 80;

    private final MensajeCasoRepository mensajeRepository;
    private final ReporteRepository reporteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ParticipanteCasoService participanteCasoService;
    private final PresenciaService presenciaService;
    private final EventoWebSocketService eventos;
    private final NotificacionService notificacionService;
    private final FotoStorageService fotoStorage;

    public ChatCasoService(MensajeCasoRepository mensajeRepository,
                           ReporteRepository reporteRepository,
                           UsuarioRepository usuarioRepository,
                           ParticipanteCasoService participanteCasoService,
                           PresenciaService presenciaService,
                           EventoWebSocketService eventos,
                           NotificacionService notificacionService,
                           FotoStorageService fotoStorage) {
        this.mensajeRepository = mensajeRepository;
        this.reporteRepository = reporteRepository;
        this.usuarioRepository = usuarioRepository;
        this.participanteCasoService = participanteCasoService;
        this.presenciaService = presenciaService;
        this.eventos = eventos;
        this.notificacionService = notificacionService;
        this.fotoStorage = fotoStorage;
    }

    /** Historial completo del chat, del mas antiguo al mas reciente. */
    @Transactional(readOnly = true)
    public List<MensajeCasoDTO> historial(Long idCaso, Long idUsuario) {
        exigirParticipante(idCaso, idUsuario);
        List<MensajeCaso> mensajes = mensajeRepository.findByIdCasoOrderByFechaEnvioAscIdMensajeAsc(idCaso);
        Set<Long> emisores = mensajes.stream().map(MensajeCaso::getIdEmisor).collect(Collectors.toSet());
        Map<Long, Usuario> usuarios = usuarios(emisores);
        return mensajes.stream().map(m -> MensajeCasoDTO.de(m, usuarios.get(m.getIdEmisor()))).toList();
    }

    /**
     * Texto o ubicacion: con latitud y longitud es una ubicacion (el contenido es un comentario
     * opcional); sin ellas es texto y el contenido es obligatorio.
     *
     * @throws ResponseStatusException 400 si el mensaje esta vacio o la ubicacion incompleta.
     */
    @Transactional
    public MensajeCasoDTO enviar(Long idCaso, Long idUsuario, EnviarMensajeCasoDTO datos) {
        exigirParticipante(idCaso, idUsuario);
        String contenido = limpiar(datos.contenido());
        boolean tieneLatitud = datos.latitud() != null;
        boolean tieneLongitud = datos.longitud() != null;
        if (tieneLatitud != tieneLongitud) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ubicación necesita latitud y longitud");
        }

        MensajeCaso mensaje = nuevo(idCaso, idUsuario);
        if (tieneLatitud) {
            mensaje.setTipo(MensajeCaso.UBICACION);
            mensaje.setLatitud(datos.latitud());
            mensaje.setLongitud(datos.longitud());
        } else {
            if (contenido == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mensaje no puede estar vacío");
            }
            mensaje.setTipo(MensajeCaso.TEXTO);
        }
        mensaje.setContenido(contenido);
        return publicar(mensaje);
    }

    /**
     * Foto del caso (JPG, PNG o WEBP; la valida FotoStorageService). El comentario es opcional.
     *
     * @throws ResponseStatusException 400 si no se envio una foto.
     */
    @Transactional
    public MensajeCasoDTO enviarFoto(Long idCaso, Long idUsuario, MultipartFile foto, String comentario) {
        exigirParticipante(idCaso, idUsuario);
        if (foto == null || foto.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una foto");
        }
        String comentarioLimpio = limpiar(comentario);
        if (comentarioLimpio != null && comentarioLimpio.length() > MensajeCaso.CONTENIDO_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mensaje no puede superar los 1000 caracteres");
        }
        MensajeCaso mensaje = nuevo(idCaso, idUsuario);
        mensaje.setTipo(MensajeCaso.FOTO);
        mensaje.setFotoUrl(RUTA_FOTOS + fotoStorage.guardar(foto));
        mensaje.setContenido(comentarioLimpio);
        return publicar(mensaje);
    }

    /** Participantes del caso con su presencia, para el indicador "en linea / ultima vez". */
    @Transactional(readOnly = true)
    public List<ParticipanteChatDTO> participantes(Long idCaso, Long idUsuario) {
        exigirParticipante(idCaso, idUsuario);
        Map<Long, String> roles = participanteCasoService.participantes(idCaso);
        Map<Long, Usuario> usuarios = usuarios(roles.keySet());
        return roles.entrySet().stream()
                .map(e -> new ParticipanteChatDTO(e.getKey(),
                        usuarios.containsKey(e.getKey()) ? usuarios.get(e.getKey()).getNombre() : null,
                        e.getValue(),
                        presenciaService.enLinea(e.getKey()),
                        presenciaService.ultimaVez(e.getKey()).orElse(null)))
                .toList();
    }

    // ============================================================

    private MensajeCasoDTO publicar(MensajeCaso mensaje) {
        MensajeCaso guardado = mensajeRepository.save(mensaje);
        Usuario emisor = usuarioRepository.findById(guardado.getIdEmisor()).orElse(null);
        MensajeCasoDTO dto = MensajeCasoDTO.de(guardado, emisor);
        logger.info("Mensaje {} ID {} en el chat del caso {} por usuario ID {}",
                guardado.getTipo(), guardado.getIdMensaje(), guardado.getIdCaso(), guardado.getIdEmisor());

        // La difusion y el aviso son efectos secundarios: si fallan, el mensaje ya quedo guardado
        // y el resto lo vera al recargar el historial.
        try {
            eventos.aCaso(guardado.getIdCaso(), EventoWebSocket.de(TipoEventoWebSocket.CHAT_MENSAJE, dto));
        } catch (RuntimeException e) {
            logger.error("No se pudo difundir el mensaje ID {} del caso {}", guardado.getIdMensaje(),
                    guardado.getIdCaso(), e);
        }
        avisarAusentes(dto);
        return dto;
    }

    private void avisarAusentes(MensajeCasoDTO mensaje) {
        String nombre = mensaje.nombreEmisor() == null ? "Alguien" : mensaje.nombreEmisor();
        String texto = nombre + " escribió en el chat del caso #" + mensaje.idCaso() + ": " + vistaPrevia(mensaje);
        for (Long idParticipante : participanteCasoService.participantes(mensaje.idCaso()).keySet()) {
            if (idParticipante.equals(mensaje.idEmisor()) || presenciaService.enLinea(idParticipante)) {
                continue;
            }
            try {
                notificacionService.crearNotificacion(idParticipante, mensaje.idCaso(), null,
                        Notificacion.MENSAJE_CASO, texto);
            } catch (RuntimeException e) {
                logger.error("No se pudo notificar al usuario ID {} del mensaje ID {}", idParticipante,
                        mensaje.idMensaje(), e);
            }
        }
    }

    static String vistaPrevia(MensajeCasoDTO mensaje) {
        return switch (mensaje.tipo()) {
            case MensajeCaso.UBICACION -> "📍 compartió una ubicación";
            case MensajeCaso.FOTO -> "📷 envió una foto";
            default -> mensaje.contenido().length() <= VISTA_PREVIA_MAX
                    ? mensaje.contenido()
                    : mensaje.contenido().substring(0, VISTA_PREVIA_MAX - 1) + "…";
        };
    }

    /**
     * 404 si el caso no existe; 403 si el usuario no participa. Asi nadie ajeno al caso puede
     * leer ni escribir en el chat aunque conozca el id.
     */
    private void exigirParticipante(Long idCaso, Long idUsuario) {
        if (!reporteRepository.existsById(idCaso)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El caso no existe");
        }
        if (!participanteCasoService.participa(idUsuario, idCaso)) {
            logger.warn("Acceso al chat del caso {} rechazado para el usuario ID {}", idCaso, idUsuario);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los involucrados en el caso pueden usar este chat");
        }
    }

    private MensajeCaso nuevo(Long idCaso, Long idUsuario) {
        MensajeCaso mensaje = new MensajeCaso();
        mensaje.setIdCaso(idCaso);
        mensaje.setIdEmisor(idUsuario);
        mensaje.setFechaEnvio(LocalDateTime.now());
        return mensaje;
    }

    private Map<Long, Usuario> usuarios(Set<Long> ids) {
        return usuarioRepository.findAllById(new HashSet<>(ids)).stream()
                .collect(Collectors.toMap(Usuario::getIdUsuario, Function.identity()));
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
