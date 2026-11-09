package com.resq.backend.service;

import com.resq.backend.dto.DenunciaRequestDTO;
import com.resq.backend.dto.ResolucionDenunciaDTO;
import com.resq.backend.entity.DenunciaMensaje;
import com.resq.backend.entity.EstadoDenuncia;
import com.resq.backend.entity.Mensaje;
import com.resq.backend.exception.ChatException;
import com.resq.backend.repository.DenunciaMensajeRepository;
import com.resq.backend.repository.MensajeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DenunciaService {

    private static final Logger logger = LoggerFactory.getLogger(DenunciaService.class);

    private final DenunciaMensajeRepository denunciaRepository;
    private final MensajeRepository mensajeRepository;

    public DenunciaService(DenunciaMensajeRepository denunciaRepository, MensajeRepository mensajeRepository) {
        this.denunciaRepository = denunciaRepository;
        this.mensajeRepository = mensajeRepository;
    }

    public DenunciaMensaje denunciar(Long idDenunciante, DenunciaRequestDTO dto) {
        Mensaje mensaje = mensajeRepository.findById(dto.idMensaje())
                .orElseThrow(() -> new ChatException(HttpStatus.NOT_FOUND, "El mensaje a denunciar no existe",
                        Map.of("idMensaje", "Mensaje no encontrado")));

        boolean esParticipante = Objects.equals(mensaje.getIdEmisor(), idDenunciante)
                || Objects.equals(mensaje.getIdReceptor(), idDenunciante);

        if (!esParticipante) {
            throw new ChatException(HttpStatus.FORBIDDEN, "No puedes denunciar un mensaje de una conversación ajena",
                    Map.of("idMensaje", "No eres participante de esta conversación"));
        }

        if (Objects.equals(mensaje.getIdEmisor(), idDenunciante)) {
            throw new ChatException(HttpStatus.BAD_REQUEST, "No puedes denunciar tu propio mensaje",
                    Map.of("idMensaje", "El denunciante es el emisor del mensaje"));
        }

        if (denunciaRepository.existsByIdMensajeAndIdDenunciante(dto.idMensaje(), idDenunciante)) {
            throw new ChatException(HttpStatus.CONFLICT, "Ya has denunciado este mensaje previamente",
                    Map.of("idMensaje", "Denuncia ya registrada por el usuario"));
        }

        Long idDenunciado = mensaje.getIdEmisor();

        DenunciaMensaje denuncia = new DenunciaMensaje(dto.idMensaje(), idDenunciante, idDenunciado, dto.motivo());
        try {
            DenunciaMensaje guardada = denunciaRepository.save(denuncia);
            logger.info("Usuario ID {} denuncio el mensaje ID {} del usuario ID {}",
                    idDenunciante, dto.idMensaje(), idDenunciado);
            return guardada;
        } catch (DataIntegrityViolationException ex) {
            throw new ChatException(HttpStatus.CONFLICT, "Ya has denunciado este mensaje previamente",
                    Map.of("idMensaje", "Denuncia ya registrada por restricción de unicidad"));
        }
    }

    public List<DenunciaMensaje> listarDenuncias(EstadoDenuncia estado) {
        if (estado != null) {
            return denunciaRepository.findByEstadoOrderByFechaDenunciaDesc(estado);
        }
        return denunciaRepository.findAllByOrderByFechaDenunciaDesc();
    }

    public DenunciaMensaje resolver(Long idDenuncia, Long idAdmin, ResolucionDenunciaDTO dto) {
        DenunciaMensaje denuncia = denunciaRepository.findById(idDenuncia)
                .orElseThrow(() -> new ChatException(HttpStatus.NOT_FOUND, "Denuncia no encontrada",
                        Map.of("idDenuncia", "Denuncia no encontrada")));

        if (denuncia.getEstado() != EstadoDenuncia.PENDIENTE) {
            throw new ChatException(HttpStatus.CONFLICT, "La denuncia ya ha sido resuelta previamente",
                    Map.of("estado", "La denuncia no está en estado PENDIENTE"));
        }

        EstadoDenuncia nuevoEstado = EstadoDenuncia.valueOf(dto.estado().trim().toUpperCase());
        denuncia.setEstado(nuevoEstado);
        denuncia.setNotaAdmin(dto.notaAdmin());
        denuncia.setIdAdminRevisor(idAdmin);
        denuncia.setFechaResolucion(LocalDateTime.now());

        DenunciaMensaje guardada = denunciaRepository.save(denuncia);
        logger.info("Admin ID {} resolvio la denuncia ID {} como {}", idAdmin, idDenuncia, nuevoEstado);
        return guardada;
    }
}
