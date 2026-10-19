package com.resq.backend.repository;

import com.resq.backend.entity.PreferenciaNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PreferenciaNotificacionRepository extends JpaRepository<PreferenciaNotificacion, Long> {

    Optional<PreferenciaNotificacion> findByIdUsuario(Long idUsuario);
}
