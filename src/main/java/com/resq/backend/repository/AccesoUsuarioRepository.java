package com.resq.backend.repository;

import com.resq.backend.entity.AccesoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccesoUsuarioRepository extends JpaRepository<AccesoUsuario, Long> {

    // HU-34: los accesos de un periodo, para contar usuarios activos y su evolucion.
    List<AccesoUsuario> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);
}
