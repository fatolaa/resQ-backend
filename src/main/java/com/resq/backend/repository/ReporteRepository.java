package com.resq.backend.repository;

import com.resq.backend.entity.Reporte;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    // HU-19: cola de revision. Los reportes anteriores a HU-19 (estado_revision null)
    // cuentan como pendientes; los mas antiguos salen primero.
    @Query("SELECT r FROM Reporte r WHERE r.estadoRevision IS NULL "
            + "OR r.estadoRevision = 'PENDIENTE_REVISION' ORDER BY r.fechaCreacion ASC")
    List<Reporte> findPendientesDeRevision();

    // ============================================================
    // HU-21: casos (reportes) asignados a una organización
    // ============================================================

    /** Casos que una organización ya tomó, para su panel de "mis casos". */
    List<Reporte> findByIdOrganizacion(Long idOrganizacion);

    List<Reporte> findByIdOrganizacion(Long idOrganizacion, Sort sort);

    /** Igual, pero filtrado a ciertos estados (filtro por estado del panel). */
    List<Reporte> findByIdOrganizacionAndEstadoIn(Long idOrganizacion, List<String> estados);

    List<Reporte> findByIdOrganizacionAndEstadoIn(Long idOrganizacion, List<String> estados, Sort sort);

    /** Casos que ninguna organización ha tomado todavía (para "Aceptar caso"). */
    List<Reporte> findByIdOrganizacionIsNull();

    // ============================================================
    // HU-33: métricas de casos y reportes
    // ============================================================

    /** Reportes creados dentro de un periodo (ambos extremos inclusive). */
    List<Reporte> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);
}