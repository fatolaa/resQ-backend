package com.resq.backend.repository;

import com.resq.backend.entity.DenunciaMensaje;
import com.resq.backend.entity.EstadoDenuncia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DenunciaMensajeRepository extends JpaRepository<DenunciaMensaje, Long> {

    List<DenunciaMensaje> findByEstadoOrderByFechaDenunciaDesc(EstadoDenuncia estado);

    List<DenunciaMensaje> findAllByOrderByFechaDenunciaDesc();

    boolean existsByIdMensajeAndIdDenunciante(Long idMensaje, Long idDenunciante);
}
