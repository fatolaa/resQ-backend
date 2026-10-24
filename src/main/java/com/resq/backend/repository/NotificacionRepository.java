package com.resq.backend.repository;

import com.resq.backend.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByIdUsuarioOrderByFechaCreacionDesc(Long idUsuario);

    /** HU-37: la bandeja muestra solo las notificaciones sin archivar, de la mas reciente a la mas antigua. */
    List<Notificacion> findByIdUsuarioAndArchivadaFalseOrderByFechaCreacionDesc(Long idUsuario);
}
