package com.resq.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * HU-40: Denuncia de un mensaje inapropiado dentro del chat para revisión administrativa.
 */
@Entity
@Table(name = "denuncia_mensaje",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_denuncia_mensaje_denunciante",
                columnNames = {"id_mensaje", "id_denunciante"}
        ))
public class DenunciaMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_denuncia")
    private Long idDenuncia;

    @Column(name = "id_mensaje", nullable = false)
    private Long idMensaje;

    @Column(name = "id_denunciante", nullable = false)
    private Long idDenunciante;

    @Column(name = "id_denunciado", nullable = false)
    private Long idDenunciado;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoDenuncia estado;

    @Column(name = "nota_admin", length = 500)
    private String notaAdmin;

    @Column(name = "id_admin_revisor")
    private Long idAdminRevisor;

    @Column(name = "fecha_denuncia")
    private LocalDateTime fechaDenuncia;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    public DenunciaMensaje() {
    }

    public DenunciaMensaje(Long idMensaje, Long idDenunciante, Long idDenunciado, String motivo) {
        this.idMensaje = idMensaje;
        this.idDenunciante = idDenunciante;
        this.idDenunciado = idDenunciado;
        this.motivo = motivo;
    }

    @PrePersist
    public void prePersist() {
        if (fechaDenuncia == null) {
            fechaDenuncia = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoDenuncia.PENDIENTE;
        }
    }

    public Long getIdDenuncia() {
        return idDenuncia;
    }

    public void setIdDenuncia(Long idDenuncia) {
        this.idDenuncia = idDenuncia;
    }

    public Long getIdMensaje() {
        return idMensaje;
    }

    public void setIdMensaje(Long idMensaje) {
        this.idMensaje = idMensaje;
    }

    public Long getIdDenunciante() {
        return idDenunciante;
    }

    public void setIdDenunciante(Long idDenunciante) {
        this.idDenunciante = idDenunciante;
    }

    public Long getIdDenunciado() {
        return idDenunciado;
    }

    public void setIdDenunciado(Long idDenunciado) {
        this.idDenunciado = idDenunciado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public EstadoDenuncia getEstado() {
        return estado;
    }

    public void setEstado(EstadoDenuncia estado) {
        this.estado = estado;
    }

    public String getNotaAdmin() {
        return notaAdmin;
    }

    public void setNotaAdmin(String notaAdmin) {
        this.notaAdmin = notaAdmin;
    }

    public Long getIdAdminRevisor() {
        return idAdminRevisor;
    }

    public void setIdAdminRevisor(Long idAdminRevisor) {
        this.idAdminRevisor = idAdminRevisor;
    }

    public LocalDateTime getFechaDenuncia() {
        return fechaDenuncia;
    }

    public void setFechaDenuncia(LocalDateTime fechaDenuncia) {
        this.fechaDenuncia = fechaDenuncia;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(LocalDateTime fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }
}
