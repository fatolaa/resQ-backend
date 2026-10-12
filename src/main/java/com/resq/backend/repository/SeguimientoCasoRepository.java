package com.resq.backend.repository;

import com.resq.backend.entity.SeguimientoCaso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeguimientoCasoRepository extends JpaRepository<SeguimientoCaso, Long> {

    List<SeguimientoCaso> findByIdReporteOrderByFechaCreacionDesc(Long idReporte);
}