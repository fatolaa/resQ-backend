package com.resq.backend.repository;

import com.resq.backend.entity.BloqueoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloqueoUsuarioRepository extends JpaRepository<BloqueoUsuario, Long> {

    Optional<BloqueoUsuario> findByIdBloqueadorAndIdBloqueado(Long idBloqueador, Long idBloqueado);

    List<BloqueoUsuario> findByIdBloqueadorOrderByFechaBloqueoDesc(Long idBloqueador);

    boolean existsByIdBloqueadorAndIdBloqueado(Long idBloqueador, Long idBloqueado);
}
