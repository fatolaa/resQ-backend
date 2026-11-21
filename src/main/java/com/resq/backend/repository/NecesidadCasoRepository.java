package com.resq.backend.repository;

import com.resq.backend.entity.NecesidadCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface NecesidadCasoRepository extends JpaRepository<NecesidadCaso, Long> {

    List<NecesidadCaso> findByIdReporteOrderByIdNecesidadAsc(Long idReporte);

    boolean existsByIdReporteAndTipo(Long idReporte, String tipo);

    /** Casos que todavia buscan este tipo de ayuda (filtro "Necesita" de Casos). */
    @Query("SELECT n.idReporte FROM NecesidadCaso n WHERE n.tipo = :tipo AND n.estado = 'BUSCADA'")
    Set<Long> idsDeCasosQueBuscan(@Param("tipo") String tipo);
}
