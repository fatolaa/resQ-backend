package com.resq.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

@Entity
@Table(name = "reporte")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Long idReporte;

    @NotNull(message = "idUsuario es obligatorio")
    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @NotBlank(message = "El tipo de caso es obligatorio")
    @Column(name = "tipo_caso", nullable = false)
    private String tipoCaso;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "PENDIENTE|EN_PROCESO|RESUELTO|CANCELADO", message = "Estado inválido")
    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @DecimalMin(value = "-90.0", message = "Latitud inválida")
    @DecimalMax(value = "90.0", message = "Latitud inválida")
    @Column(name = "latitud")
    private Double latitud;

    @DecimalMin(value = "-180.0", message = "Longitud inválida")
    @DecimalMax(value = "180.0", message = "Longitud inválida")
    @Column(name = "longitud")
    private Double longitud;

    // HU-19: campos de revisión, controlados solo por el backend
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Pattern(regexp = "PENDIENTE|APROBADO|RECHAZADO", message = "Estado de revisión inválido")
    @Column(name = "estado_revision")
    private String estadoRevision;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "nota_revision", columnDefinition = "TEXT")
    private String notaRevision;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "id_revisor")
    private Long idRevisor;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    // ---- HU-21: asignación a una organización ----
    // Null mientras el caso no ha sido tomado por ninguna organización.
    // Solo el backend lo asigna (al aceptar un caso), nunca el cliente.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "id_organizacion")
    private Long idOrganizacion;

    public Reporte() {
    }

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estadoRevision == null) {
            estadoRevision = EstadoRevision.PENDIENTE;
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

    public String getEstadoRevision() {
        return estadoRevision;
    }

    public void setEstadoRevision(String estadoRevision) {
        this.estadoRevision = estadoRevision;
    }

    public String getNotaRevision() {
        return notaRevision;
    }

    public void setNotaRevision(String notaRevision) {
        this.notaRevision = notaRevision;
    }

    public Long getIdRevisor() {
        return idRevisor;
    }

    public void setIdRevisor(Long idRevisor) {
        this.idRevisor = idRevisor;
    }

    public LocalDateTime getFechaRevision() {
        return fechaRevision;
    }

    public void setFechaRevision(LocalDateTime fechaRevision) {
        this.fechaRevision = fechaRevision;
    }

    // HU-21
    public Long getIdOrganizacion() {
        return idOrganizacion;
    }

    // HU-21
    public void setIdOrganizacion(Long idOrganizacion) {
        this.idOrganizacion = idOrganizacion;
    }
}