package com.resq.backend.service;

import com.resq.backend.dto.ConversacionDTO;
import com.resq.backend.entity.Mensaje;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.MensajeRepository;
import com.resq.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;

    public ChatService(MensajeRepository mensajeRepository, UsuarioRepository usuarioRepository) {
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Obtiene la lista de conversaciones del usuario actual
    public List<ConversacionDTO> obtenerConversaciones(Long idUsuario, String busqueda) {
        // 1. Traer todos los mensajes donde el usuario participa
        List<Mensaje> todos = mensajeRepository.findAll().stream()
                .filter(m -> m.getIdEmisor().equals(idUsuario) || m.getIdReceptor().equals(idUsuario))
                .toList();

        // 2. Agrupar por el ID del "otro" usuario
        Map<Long, List<Mensaje>> agrupados = todos.stream()
                .collect(Collectors.groupingBy(m -> m.getIdEmisor().equals(idUsuario) ? m.getIdReceptor() : m.getIdEmisor()));

        List<ConversacionDTO> resultado = new ArrayList<>();

        for (Map.Entry<Long, List<Mensaje>> entry : agrupados.entrySet()) {
            Long otroId = entry.getKey();
            List<Mensaje> mensajes = entry.getValue();
            
            // Ordenar para sacar el último
            mensajes.sort(Comparator.comparing(Mensaje::getFechaEnvio));
            Mensaje ultimo = mensajes.get(mensajes.size() - 1);

            // Ver si YO lo he archivado
            boolean archivada = ultimo.getIdEmisor().equals(idUsuario) 
                ? ultimo.isArchivadoEmisor() 
                : ultimo.isArchivadoReceptor();

            // Filtro de búsqueda (por nombre o por texto del mensaje)
            Optional<Usuario> otroUsuario = usuarioRepository.findById(otroId);
            if (otroUsuario.isPresent()) {
                String nombreOtro = otroUsuario.get().getNombre();
                
                if (busqueda != null && !busqueda.isEmpty()) {
                    boolean coincideNombre = nombreOtro.toLowerCase().contains(busqueda.toLowerCase());
                    boolean coincideTexto = mensajes.stream().anyMatch(m -> m.getContenido().toLowerCase().contains(busqueda.toLowerCase()));
                    if (!coincideNombre && !coincideTexto) continue; // No cumple el filtro
                }

                resultado.add(new ConversacionDTO(
                        otroId,
                        nombreOtro,
                        ultimo.getContenido(),
                        ultimo.getFechaEnvio(),
                        archivada
                ));
            }
        }

        // 3. Ordenar la lista final: primero los que tienen mensajes más recientes
        resultado.sort((a, b) -> b.fechaUltimoMensaje().compareTo(a.fechaUltimoMensaje()));

        return resultado;
    }

    // Archivar / Desarchivar una conversación
    public void archivarConversacion(Long idUsuario, Long idOtroUsuario, boolean archivar) {
        List<Mensaje> mensajes = mensajeRepository.findConversacion(idUsuario, idOtroUsuario);
        for (Mensaje m : mensajes) {
            if (m.getIdEmisor().equals(idUsuario)) {
                m.setArchivadoEmisor(archivar);
            } else {
                m.setArchivadoReceptor(archivar);
            }
        }
        mensajeRepository.saveAll(mensajes);
    }
}