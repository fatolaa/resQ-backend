package com.resq.backend.entity;

import com.resq.backend.validation.CoordenadasCoherentes;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

@Entity
@Table(name = "reporte")
@CoordenadasCoherentes
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Long idReporte;

    @NotNull(message = "idUsuario es obligatorio")
    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @NotBlank(message = "tipoCaso es obligatorio")
    @Column(name = "tipo_caso", length = 100)
    private String tipoCaso;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @NotBlank(message = "estado es obligatorio")
    @Pattern(regexp = "PENDIENTE|EN_PROCESO|RESUELTO|CANCELADO",
            message = "estado debe ser PENDIENTE, EN_PROCESO, RESUELTO o CANCELADO")
    @Column(name = "estado", length = 50)
    private String estado;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @DecimalMin(value = "-90.0", message = "latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "latitud debe estar entre -90 y 90")
    @Column(name = "latitud")
    private Double latitud;

    @DecimalMin(value = "-180.0", message = "longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "longitud debe estar entre -180 y 180")
    @Column(name = "longitud")
    private Double longitud;

    public Reporte() {
    }

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Long getIdReporte() {
        return idReporte;
    }

    public void setIdReporte(Long idReporte) {
        this.idReporte = idReporte;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getTipoCaso() {
        return tipoCaso;
    }

    public void setTipoCaso(String tipoCaso) {
        this.tipoCaso = tipoCaso;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }
}