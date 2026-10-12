package com.resq.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * HU-21: una entrada del historial de seguimiento que una organización deja sobre
 * un caso que gestiona (nota + foto opcional, con fecha). Un Reporte puede tener
 * varias, para ver el progreso a lo largo del tiempo.
 */
@Entity
@Table(name = "seguimiento_caso")
public class SeguimientoCaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seguimiento")
    private Long idSeguimiento;

    @NotNull(message = "idReporte es obligatorio")
    @Column(name = "id_reporte", nullable = false)
    private Long idReporte;

    @NotNull(message = "idOrganizacion es obligatorio")
    @Column(name = "id_organizacion", nullable = false)
    private Long idOrganizacion;

    @Column(name = "nota", columnDefinition = "TEXT")
    private String nota;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    public SeguimientoCaso() {
    }

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Long getIdSeguimiento() {
        return idSeguimiento;
    }

    public void setIdSeguimiento(Long idSeguimiento) {
        this.idSeguimiento = idSeguimiento;
    }

    public Long getIdReporte() {
        return idReporte;
    }

    public void setIdReporte(Long idReporte) {
        this.idReporte = idReporte;
    }

    public Long getIdOrganizacion() {
        return idOrganizacion;
    }

    public void setIdOrganizacion(Long idOrganizacion) {
        this.idOrganizacion = idOrganizacion;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}