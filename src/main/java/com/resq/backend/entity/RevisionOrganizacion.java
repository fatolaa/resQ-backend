package com.resq.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * HU-32: historial de la verificacion de una organizacion. Una fila por cada decision del
 * administrador (APROBADA, RECHAZADA) y por cada vez que la organizacion reenvia su solicitud
 * despues de un rechazo (REENVIADA). Nunca se modifica ni se borra: es el registro de quien
 * hizo que y cuando.
 */
@Entity
@Table(name = "revision_organizacion")
public class RevisionOrganizacion {

    public static final String APROBADA = "APROBADA";
    public static final String RECHAZADA = "RECHAZADA";
    public static final String REENVIADA = "REENVIADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_revision")
    private Long idRevision;

    @Column(name = "id_organizacion", nullable = false)
    private Long idOrganizacion;

    @Column(name = "accion", nullable = false, length = 20)
    private String accion;

    @Column(name = "nota", length = 500)
    private String nota;

    /** El administrador que decidio o, en un reenvio, el representante. */
    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public RevisionOrganizacion() {
    }

    public RevisionOrganizacion(Long idOrganizacion, String accion, String nota, Long idUsuario,
                                LocalDateTime fecha) {
        this.idOrganizacion = idOrganizacion;
        this.accion = accion;
        this.nota = nota;
        this.idUsuario = idUsuario;
        this.fecha = fecha;
    }

    public Long getIdRevision() {
        return idRevision;
    }

    public void setIdRevision(Long idRevision) {
        this.idRevision = idRevision;
    }

    public Long getIdOrganizacion() {
        return idOrganizacion;
    }

    public void setIdOrganizacion(Long idOrganizacion) {
        this.idOrganizacion = idOrganizacion;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
