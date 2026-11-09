package com.resq.backend.repository;

import com.resq.backend.entity.MensajeCaso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeCasoRepository extends JpaRepository<MensajeCaso, Long> {

    /** HU-22: historial del chat de un caso, del mas antiguo al mas reciente. */
    List<MensajeCaso> findByIdCasoOrderByFechaEnvioAscIdMensajeAsc(Long idCaso);
}
