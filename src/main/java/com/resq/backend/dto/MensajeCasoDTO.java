package com.resq.backend.dto;

import com.resq.backend.entity.MensajeCaso;
import com.resq.backend.entity.Usuario;

import java.time.LocalDateTime;

/** HU-22: un mensaje del chat del caso, con el nombre de quien lo envio. */
public record MensajeCasoDTO(
        Long idMensaje,
        Long idCaso,
        Long idEmisor,
        String nombreEmisor,
        String tipo,
        String contenido,
        Double latitud,
        Double longitud,
        String fotoUrl,
        LocalDateTime fechaEnvio) {

    public static MensajeCasoDTO de(MensajeCaso m, Usuario emisor) {
        return new MensajeCasoDTO(m.getIdMensaje(), m.getIdCaso(), m.getIdEmisor(),
                emisor == null ? null : emisor.getNombre(), m.getTipo(), m.getContenido(),
                m.getLatitud(), m.getLongitud(), m.getFotoUrl(), m.getFechaEnvio());
    }
}
