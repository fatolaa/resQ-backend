package com.resq.backend.repository;

import com.resq.backend.entity.EstadoSolicitudVoluntario;
import com.resq.backend.entity.SolicitudVoluntario;
import com.resq.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SolicitudVoluntarioRepository extends JpaRepository<SolicitudVoluntario, Long> {
    List<SolicitudVoluntario> findByEstadoOrderByFechaSolicitudAsc(EstadoSolicitudVoluntario estado);
    List<SolicitudVoluntario> findByUsuarioOrderByFechaSolicitudDesc(Usuario usuario);
    Optional<SolicitudVoluntario> findTopByUsuarioAndEstadoOrderByFechaSolicitudDesc(Usuario usuario, EstadoSolicitudVoluntario estado);
    boolean existsByUsuarioAndEstadoAndFechaResolucionAfter(Usuario usuario, EstadoSolicitudVoluntario estado, LocalDateTime fechaLimite);
    Optional<SolicitudVoluntario> findTopByUsuarioOrderByFechaSolicitudDesc(Usuario usuario);

    // HU-34: solicitudes aprobadas (nuevos voluntarios y su tipo de ayuda).
    List<SolicitudVoluntario> findByEstado(EstadoSolicitudVoluntario estado);
}