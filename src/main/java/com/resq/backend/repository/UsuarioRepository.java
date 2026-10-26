package com.resq.backend.repository;

import com.resq.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    // HU-34: cuentas creadas dentro de un periodo.
    List<Usuario> findByFechaRegistroBetween(LocalDateTime desde, LocalDateTime hasta);

    // HU-34: cuentas que ya existian al cierre de un periodo. Las que no tienen fecha
    // de registro (creadas antes de que se guardara) se cuentan como anteriores.
    long countByFechaRegistroLessThanEqualOrFechaRegistroIsNull(LocalDateTime hasta);

    // HU-34: voluntarios actuales, para clasificarlos por tipo de ayuda.
    List<Usuario> findByRolIgnoreCase(String rol);
}