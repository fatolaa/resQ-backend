package com.resq.backend.service;

import com.resq.backend.dto.BloqueoDTO;
import com.resq.backend.entity.BloqueoUsuario;
import com.resq.backend.entity.Usuario;
import com.resq.backend.exception.ChatException;
import com.resq.backend.repository.BloqueoUsuarioRepository;
import com.resq.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BloqueoService {

    private static final Logger logger = LoggerFactory.getLogger(BloqueoService.class);

    private final BloqueoUsuarioRepository bloqueoRepository;
    private final UsuarioRepository usuarioRepository;

    public BloqueoService(BloqueoUsuarioRepository bloqueoRepository, UsuarioRepository usuarioRepository) {
        this.bloqueoRepository = bloqueoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public BloqueoUsuario bloquear(Long idBloqueador, Long idBloqueado) {
        if (Objects.equals(idBloqueador, idBloqueado)) {
            throw new ChatException(HttpStatus.BAD_REQUEST, "No puedes bloquearte a ti mismo",
                    Map.of("idBloqueado", "No puedes bloquear tu propia cuenta"));
        }

        Usuario destino = usuarioRepository.findById(idBloqueado)
                .orElseThrow(() -> new ChatException(HttpStatus.NOT_FOUND, "El usuario a bloquear no existe",
                        Map.of("idBloqueado", "Usuario no encontrado")));

        if (bloqueoRepository.existsByIdBloqueadorAndIdBloqueado(idBloqueador, idBloqueado)) {
            throw new ChatException(HttpStatus.CONFLICT, "Ya has bloqueado a este usuario",
                    Map.of("idBloqueado", "El usuario ya se encuentra bloqueado"));
        }

        BloqueoUsuario bloqueo = new BloqueoUsuario(idBloqueador, idBloqueado);
        try {
            BloqueoUsuario guardado = bloqueoRepository.save(bloqueo);
            logger.info("Usuario ID {} bloqueo al usuario ID {}", idBloqueador, idBloqueado);
            return guardado;
        } catch (DataIntegrityViolationException ex) {
            throw new ChatException(HttpStatus.CONFLICT, "Ya has bloqueado a este usuario",
                    Map.of("idBloqueado", "El usuario ya se encuentra bloqueado (restricción de unicidad)"));
        }
    }

    public void desbloquear(Long idBloqueador, Long idBloqueado) {
        BloqueoUsuario bloqueo = bloqueoRepository.findByIdBloqueadorAndIdBloqueado(idBloqueador, idBloqueado)
                .orElseThrow(() -> new ChatException(HttpStatus.NOT_FOUND, "No tienes bloqueado a este usuario",
                        Map.of("idBloqueado", "Bloqueo no encontrado")));

        bloqueoRepository.delete(bloqueo);
        logger.info("Usuario ID {} desbloqueo al usuario ID {}", idBloqueador, idBloqueado);
    }

    public List<BloqueoDTO> listarBloqueados(Long idBloqueador) {
        List<BloqueoUsuario> bloqueos = bloqueoRepository.findByIdBloqueadorOrderByFechaBloqueoDesc(idBloqueador);
        List<BloqueoDTO> resultado = new ArrayList<>();

        for (BloqueoUsuario b : bloqueos) {
            String nombre = usuarioRepository.findById(b.getIdBloqueado())
                    .map(Usuario::getNombre)
                    .orElse("Usuario desconocido");
            resultado.add(new BloqueoDTO(b.getIdBloqueo(), b.getIdBloqueado(), nombre, b.getFechaBloqueo()));
        }

        return resultado;
    }

    public boolean estaBloqueado(Long idBloqueador, Long idBloqueado) {
        return bloqueoRepository.existsByIdBloqueadorAndIdBloqueado(idBloqueador, idBloqueado);
    }
}
