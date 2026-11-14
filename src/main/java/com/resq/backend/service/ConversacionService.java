package com.resq.backend.service;

import com.resq.backend.dto.AbrirConversacionRequestDTO;
import com.resq.backend.dto.ConversacionResumenDTO;
import com.resq.backend.dto.DirectorioUsuarioDTO;
import com.resq.backend.dto.MensajeDTO;
import com.resq.backend.dto.MensajeEliminadoEventoDTO;
import com.resq.backend.dto.MensajeNuevoEventoDTO;
import com.resq.backend.entity.Mensaje;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.MensajeRepository;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.service.AdjuntoMensajeStorageService.AdjuntoGuardado;
import com.resq.backend.websocket.EventoWebSocket;
import com.resq.backend.websocket.EventoWebSocketService;
import com.resq.backend.websocket.TipoEventoWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ConversacionService {

    private static final Logger logger = LoggerFactory.getLogger(ConversacionService.class);

    private static final String RUTA_ADJUNTOS = "/api/conversaciones/adjuntos/";
    private static final int VISTA_PREVIA_MAX = 120;

    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacionRepository organizacionRepository;
    private final AdjuntoMensajeStorageService adjuntoStorage;
    private final EventoWebSocketService eventos;
    private final BloqueoService bloqueos;

    public ConversacionService(MensajeRepository mensajeRepository,
                               UsuarioRepository usuarioRepository,
                               OrganizacionRepository organizacionRepository,
                               AdjuntoMensajeStorageService adjuntoStorage,
                               EventoWebSocketService eventos,
                               BloqueoService bloqueos) {
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.organizacionRepository = organizacionRepository;
        this.adjuntoStorage = adjuntoStorage;
        this.eventos = eventos;
        this.bloqueos = bloqueos;
    }

    @Transactional(readOnly = true)
    public List<ConversacionResumenDTO> obtenerConversaciones(Long idUsuario) {
        return obtenerConversaciones(idUsuario, null, null);
    }

    /**
     * TT-50: bandeja con busqueda y archivado.
     *
     * @param busqueda   filtra por nombre del otro participante o por el texto
     *                   de cualquier mensaje de la conversacion (case-insensitive).
     * @param archivadas null = todas, true = solo archivadas, false = solo activas.
     */
    @Transactional(readOnly = true)
    public List<ConversacionResumenDTO> obtenerConversaciones(Long idUsuario, String busqueda, Boolean archivadas) {
        Map<Long, List<Mensaje>> porOtro = new LinkedHashMap<>();
        for (Mensaje mensaje : mensajeRepository.findTodosDeUsuario(idUsuario)) {
            Long idOtro = mensaje.getIdEmisor().equals(idUsuario)
                    ? mensaje.getIdReceptor()
                    : mensaje.getIdEmisor();
            porOtro.computeIfAbsent(idOtro, clave -> new ArrayList<>()).add(mensaje);
        }

        String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
        List<ConversacionResumenDTO> resultado = new ArrayList<>();
        for (Map.Entry<Long, List<Mensaje>> entrada : porOtro.entrySet()) {
            Mensaje ultimo = entrada.getValue().get(0);
            Optional<ConversacionResumenDTO> resumen = resumen(idUsuario, entrada.getKey(), ultimo);
            if (resumen.isEmpty()) {
                continue;
            }
            ConversacionResumenDTO conversacion = resumen.get();
            if (archivadas != null && conversacion.archivada() != archivadas) {
                continue;
            }
            if (!filtro.isEmpty() && !coincide(conversacion.nombre(), entrada.getValue(), filtro)) {
                continue;
            }
            resultado.add(conversacion);
        }

        resultado.sort((a, b) -> {
            if (a.fechaUltimoMensaje() == null) {
                return 1;
            }
            if (b.fechaUltimoMensaje() == null) {
                return -1;
            }
            return b.fechaUltimoMensaje().compareTo(a.fechaUltimoMensaje());
        });
        return resultado;
    }

    /**
     * TT-50: archiva o desarchiva una conversacion propia.
     * Solo se puede archivar el lado del usuario autenticado y la conversacion
     * tiene que existir, de lo contrario responde 404.
     */
    @Transactional
    public void archivarConversacion(Long idUsuario, Long idOtroUsuario, boolean archivar) {
        if (idOtroUsuario == null || idOtroUsuario <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La conversacion no es valida");
        }
        if (Objects.equals(idUsuario, idOtroUsuario)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes archivar tu propia conversacion");
        }
        if (!usuarioRepository.existsById(idOtroUsuario)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe");
        }
        List<Mensaje> mensajes = mensajeRepository.findConversacion(idUsuario, idOtroUsuario);
        if (mensajes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La conversacion no existe");
        }
        for (Mensaje mensaje : mensajes) {
            if (mensaje.getIdEmisor().equals(idUsuario)) {
                mensaje.setArchivadoEmisor(archivar);
            } else {
                mensaje.setArchivadoReceptor(archivar);
            }
        }
        mensajeRepository.saveAll(mensajes);
    }

    private static boolean coincide(String nombre, List<Mensaje> mensajes, String filtro) {
        if (nombre != null && nombre.toLowerCase().contains(filtro)) {
            return true;
        }
        return mensajes.stream()
                .filter(mensaje -> !mensaje.isEliminado())
                .map(Mensaje::getContenido)
                .filter(Objects::nonNull)
                .anyMatch(contenido -> contenido.toLowerCase().contains(filtro));
    }

    /**
     * TT-49: directorio minimo de usuarios para iniciar una conversacion.
     * Excluye al propio usuario y a quienes tienen un bloqueo en cualquier
     * direccion. El filtro exige al menos dos caracteres para no volcar todo
     * el padron en cada pulsacion.
     */
    @Transactional(readOnly = true)
    public List<DirectorioUsuarioDTO> buscarUsuarios(Long idUsuario, String nombre) {
        String filtro = nombre == null ? "" : nombre.trim();
        if (filtro.length() < 2) {
            return List.of();
        }
        return usuarioRepository
                .findTop20ByNombreContainingIgnoreCaseAndIdUsuarioNotOrderByNombreAsc(filtro, idUsuario)
                .stream()
                .filter(otro -> !bloqueos.estaBloqueado(idUsuario, otro.getIdUsuario())
                        && !bloqueos.estaBloqueado(otro.getIdUsuario(), idUsuario))
                .map(otro -> new DirectorioUsuarioDTO(otro.getIdUsuario(), otro.getNombre(), otro.getRol()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversacionResumenDTO abrirConversacion(Long idUsuario, AbrirConversacionRequestDTO datos) {
        Long idOtro;
        Optional<Organizacion> organizacion = Optional.empty();

        if (datos != null && datos.idOrganizacion() != null) {
            organizacion = organizacionRepository.findById(datos.idOrganizacion());
            if (organizacion.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La organizacion no existe");
            }
            idOtro = organizacion.get().getIdRepresentante();
        } else if (datos != null && datos.idUsuario() != null) {
            idOtro = datos.idUsuario();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica el usuario o la organizacion");
        }

        if (Objects.equals(idUsuario, idOtro)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No puedes abrir una conversacion contigo mismo");
        }
        if (!usuarioRepository.existsById(idOtro)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe");
        }
        exigirSinBloqueo(idUsuario, idOtro);
        if (organizacion.isEmpty()) {
            organizacion = organizacionRepository.findByIdRepresentante(idOtro);
        }

        String nombre = organizacion.map(Organizacion::getNombre)
                .orElseGet(() -> usuarioRepository.findById(idOtro).map(Usuario::getNombre).orElse(""));
        List<Mensaje> mensajes = mensajeRepository.findConversacionDesc(idUsuario, idOtro);
        Mensaje ultimo = mensajes.isEmpty() ? null : mensajes.get(0);

        return new ConversacionResumenDTO(
                idOtro,
                idOtro,
                nombre,
                organizacion.isPresent(),
                ultimo == null ? "" : vistaPrevia(ultimo),
                ultimo == null ? null : ultimo.getFechaEnvio(),
                ultimo != null && archivadaPara(ultimo, idUsuario));
    }

    @Transactional(readOnly = true)
    public List<MensajeDTO> obtenerMensajes(Long idUsuario, Long idConversacion) {
        exigirConversacion(idUsuario, idConversacion);
        return mensajeRepository.findConversacionDesc(idUsuario, idConversacion).stream()
                .map(mensaje -> MensajeDTO.de(mensaje, idUsuario))
                .toList();
    }

    @Transactional
    public MensajeDTO enviarMensaje(Long idUsuario, Long idConversacion, String contenido, MultipartFile archivo) {
        exigirConversacion(idUsuario, idConversacion);
        exigirSinBloqueo(idUsuario, idConversacion);

        String texto = contenido == null ? "" : contenido.trim();
        if (texto.length() > Mensaje.CONTENIDO_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El mensaje no puede superar los " + Mensaje.CONTENIDO_MAX + " caracteres");
        }
        boolean hayArchivo = archivo != null && !archivo.isEmpty();
        if (texto.isEmpty() && !hayArchivo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Escribe un mensaje o adjunta un archivo");
        }

        Mensaje mensaje = new Mensaje();
        mensaje.setIdEmisor(idUsuario);
        mensaje.setIdReceptor(idConversacion);
        mensaje.setContenido(texto);
        mensaje.setFechaEnvio(LocalDateTime.now());
        mensaje.setLeido(false);
        mensaje.setEliminado(false);

        if (hayArchivo) {
            AdjuntoGuardado guardado = adjuntoStorage.guardar(archivo);
            mensaje.setTipo(AdjuntoMensajeStorageService.esImagen(guardado.extension())
                    ? Mensaje.IMAGEN
                    : Mensaje.ARCHIVO);
            mensaje.setUrlAdjunto(RUTA_ADJUNTOS + guardado.nombre());
            mensaje.setNombreAdjunto(guardado.nombreOriginal());
        } else {
            mensaje.setTipo(Mensaje.TEXTO);
        }

        Mensaje guardado = mensajeRepository.save(mensaje);
        publicarNuevo(guardado);
        return MensajeDTO.de(guardado, idUsuario);
    }

    @Transactional
    public void eliminarMensaje(Long idUsuario, Long idMensaje) {
        Mensaje mensaje = mensajeRepository.findById(idMensaje)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El mensaje no existe"));
        if (!mensaje.getIdEmisor().equals(idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo puedes eliminar tus propios mensajes");
        }
        if (mensaje.isEliminado()) {
            return;
        }

        mensaje.setEliminado(true);
        mensaje.setContenido("");
        mensaje.setUrlAdjunto(null);
        mensaje.setNombreAdjunto(null);
        mensajeRepository.save(mensaje);

        MensajeEliminadoEventoDTO datos = new MensajeEliminadoEventoDTO(
                mensaje.getIdEmisor(), mensaje.getIdMensaje());
        try {
            eventos.aUsuario(mensaje.getIdReceptor(),
                    EventoWebSocket.de(TipoEventoWebSocket.MENSAJE_ELIMINADO, datos));
        } catch (RuntimeException e) {
            logger.error("No se pudo difundir la eliminacion del mensaje ID {}", mensaje.getIdMensaje(), e);
        }
    }

    private Optional<ConversacionResumenDTO> resumen(Long idUsuario, Long idOtro, Mensaje ultimo) {
        Optional<Usuario> usuario = usuarioRepository.findById(idOtro);
        if (usuario.isEmpty()) {
            return Optional.empty();
        }
        Optional<Organizacion> organizacion = organizacionRepository.findByIdRepresentante(idOtro);
        String nombre = organizacion.map(Organizacion::getNombre).orElse(usuario.get().getNombre());
        return Optional.of(new ConversacionResumenDTO(
                idOtro,
                idOtro,
                nombre,
                organizacion.isPresent(),
                vistaPrevia(ultimo),
                ultimo.getFechaEnvio(),
                archivadaPara(ultimo, idUsuario)));
    }

    private static boolean archivadaPara(Mensaje ultimo, Long idUsuario) {
        return ultimo.getIdEmisor().equals(idUsuario)
                ? ultimo.isArchivadoEmisor()
                : ultimo.isArchivadoReceptor();
    }

    private void publicarNuevo(Mensaje mensaje) {
        MensajeNuevoEventoDTO datos = new MensajeNuevoEventoDTO(
                mensaje.getIdEmisor(),
                mensaje.getIdMensaje(),
                mensaje.getIdEmisor(),
                mensaje.getTipo(),
                mensaje.getContenido(),
                mensaje.getUrlAdjunto(),
                mensaje.getNombreAdjunto(),
                mensaje.getFechaEnvio());
        try {
            eventos.aUsuario(mensaje.getIdReceptor(),
                    EventoWebSocket.de(TipoEventoWebSocket.MENSAJE_NUEVO, datos));
        } catch (RuntimeException e) {
            logger.error("No se pudo difundir el mensaje ID {}", mensaje.getIdMensaje(), e);
        }
    }

    private void exigirConversacion(Long idUsuario, Long idConversacion) {
        if (idConversacion == null || idConversacion <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La conversacion no es valida");
        }
        if (Objects.equals(idUsuario, idConversacion)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes conversar contigo mismo");
        }
        if (!usuarioRepository.existsById(idConversacion)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe");
        }
    }

    private void exigirSinBloqueo(Long emisor, Long receptor) {
        if (bloqueos.estaBloqueado(emisor, receptor) || bloqueos.estaBloqueado(receptor, emisor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No puedes iniciar ni enviar mensajes mientras exista un bloqueo entre los participantes");
        }
    }

    private static String vistaPrevia(Mensaje mensaje) {
        if (mensaje.isEliminado()) {
            return "";
        }
        String contenido = mensaje.getContenido();
        if (contenido == null || contenido.isBlank()) {
            return mensaje.getNombreAdjunto() == null ? "" : mensaje.getNombreAdjunto();
        }
        return contenido.length() <= VISTA_PREVIA_MAX
                ? contenido
                : contenido.substring(0, VISTA_PREVIA_MAX - 3) + "...";
    }
}
