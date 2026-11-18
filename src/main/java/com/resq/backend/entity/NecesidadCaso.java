package com.resq.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Rediseño: algo que necesita un caso (transporte, veterinario...) y quien lo cubre.
 *
 * Ciclo: BUSCADA → OFRECIDA (alguien se ofrecio) → CUBIERTA (quien reporto o la
 * organizacion lo confirmo). Desde OFRECIDA o CUBIERTA se puede liberar y vuelve a
 * BUSCADA. Un caso tiene como mucho una necesidad de cada tipo.
 */
@Entity
@Table(name = "necesidad_caso",
        uniqueConstraints = @UniqueConstraint(name = "uk_necesidad_caso_tipo", columnNames = {"id_reporte", "tipo"}),
        indexes = @Index(name = "idx_necesidad_caso_reporte", columnList = "id_reporte"))
public class NecesidadCaso {

    // Catalogo: los cuatro tipos de ayuda de los voluntarios (HU-17) mas veterinario.
    public static final String TRANSPORTE = "TRANSPORTE";
    public static final String VETERINARIO = "VETERINARIO";
    public static final String HOGAR_TEMPORAL = "HOGAR_TEMPORAL";
    public static final String ALIMENTO = "ALIMENTO";
    public static final String RESCATE = "RESCATE";
    public static final List<String> TIPOS = List.of(TRANSPORTE, VETERINARIO, HOGAR_TEMPORAL, ALIMENTO, RESCATE);

    public static final String BUSCADA = "BUSCADA";
    public static final String OFRECIDA = "OFRECIDA";
    public static final String CUBIERTA = "CUBIERTA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_necesidad")
    private Long idNecesidad;

    @Column(name = "id_reporte", nullable = false)
    private Long idReporte;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    /** Quien se ofrecio o la cubre; null mientras se busca. */
    @Column(name = "id_ayudante")
    private Long idAyudante;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    void marcarFecha() {
        fechaActualizacion = LocalDateTime.now();
        if (estado == null) {
            estado = BUSCADA;
        }
    }

    public Long getIdNecesidad() {
        return idNecesidad;
    }

    public void setIdNecesidad(Long idNecesidad) {
        this.idNecesidad = idNecesidad;
    }

    public Long getIdReporte() {
        return idReporte;
    }

    public void setIdReporte(Long idReporte) {
        this.idReporte = idReporte;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Long getIdAyudante() {
        return idAyudante;
    }

    public void setIdAyudante(Long idAyudante) {
        this.idAyudante = idAyudante;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
