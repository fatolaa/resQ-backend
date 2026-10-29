package com.resq.backend.repository;

import com.resq.backend.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    // Trae todos los mensajes entre dos usuarios ordenados por fecha
    @Query("SELECT m FROM Mensaje m WHERE (m.idEmisor = :u1 AND m.idReceptor = :u2) OR (m.idEmisor = :u2 AND m.idReceptor = :u1) ORDER BY m.fechaEnvio ASC")
    List<Mensaje> findConversacion(@Param("u1") Long u1, @Param("u2") Long u2);
}