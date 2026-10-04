package com.resq.backend.repository;

import com.resq.backend.entity.Reporte;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    /**
     * HU-24: el texto se busca contra la descripcion y el tipo de caso, que son los
     * dos campos por los que el administrador reconoce un reporte. La comparacion va
     * en minusculas para que "perro" encuentre "Perro".
     */
    String COINCIDENCIA_TEXTO = """
            (LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(r.tipoCaso) LIKE LOWER(CONCAT('%', :texto, '%')))""";

    List<Reporte> findByIdUsuario(Long idUsuario);

    List<Reporte> findByEstadoIn(List<String> estados);

    List<Reporte> findByEstadoIn(List<String> estados, Sort sort);

    @Query("SELECT r FROM Reporte r WHERE " + COINCIDENCIA_TEXTO)
    List<Reporte> buscar(@Param("texto") String texto, Sort sort);

    @Query("SELECT r FROM Reporte r WHERE r.estado IN :estados AND " + COINCIDENCIA_TEXTO)
    List<Reporte> buscarPorEstados(@Param("texto") String texto,
                                   @Param("estados") List<String> estados,
                                   Sort sort);
}