package com.resq.backend.repository;

import com.resq.backend.entity.Organizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizacionRepository extends JpaRepository<Organizacion, Long> {

    Optional<Organizacion> findByIdRepresentante(Long idRepresentante);

    Optional<Organizacion> findByEmailIgnoreCase(String email);

    boolean existsByIdRepresentante(Long idRepresentante);

    boolean existsByEmailIgnoreCase(String email);

    // HU-29: el directorio solo expone organizaciones verificadas por un administrador.
    List<Organizacion> findByEstadoVerificacion(String estadoVerificacion);
}
