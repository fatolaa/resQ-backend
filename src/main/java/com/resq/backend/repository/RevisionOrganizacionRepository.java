package com.resq.backend.repository;

import com.resq.backend.entity.RevisionOrganizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RevisionOrganizacionRepository extends JpaRepository<RevisionOrganizacion, Long> {

    /** HU-32: historial de una organizacion, lo mas reciente primero. */
    List<RevisionOrganizacion> findByIdOrganizacionOrderByFechaDescIdRevisionDesc(Long idOrganizacion);
}
