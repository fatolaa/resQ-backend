package com.resq.backend.repository;

import com.resq.backend.entity.Adopcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdopcionRepository extends JpaRepository<Adopcion, Long> {

    Optional<Adopcion> findByIdReporte(Long idReporte);

    boolean existsByIdReporte(Long idReporte);
}
