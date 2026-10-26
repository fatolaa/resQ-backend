package com.resq.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** HU-19: aviso in-app para el usuario (por ahora, resultado de la revisión de su reporte). */
@Entity
@Table(name = "notificacion")
public class Notificacion {

    public static final String SOLICITUD_APROBADA = "SOLICITUD_APROBADA";
    public static final String SOLICITUD_RECHAZADA = "SOLICITUD_RECHAZADA";
    public static final String SOLICITUD_VOLUNTARIO = "SOLICITUD_VOLUNTARIO";
    public static final String SISTEMA = "SISTEMA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long idNotificacion;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "id_reporte")
    private Long idReporte;

    /** HU-37: referencia a la solicitud de voluntario que origino el aviso. */
    @Column(name = "id_solicitud")
    private Long idSolicitud;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "mensaje", nullable = false, length = 700)
    private String mensaje;

    @Column(name = "leida", nullable = false)
    private boolean leida;

    /** HU-37: una notificacion archivada sale de la bandeja, pero no se borra. */
    @Column(name = "archivada")
    private boolean archivada;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    public Notificacion() {
    }

    public Notificacion(Long idUsuario, Long idReporte, String tipo, String mensaje) {
        this.idUsuario = idUsuario;
        this.idReporte = idReporte;
        this.tipo = tipo;
        this.mensaje = mensaje;
    }

    public Notificacion(Long idUsuario, Long idReporte, Long idSolicitud, String tipo, String mensaje) {
        this(idUsuario, idReporte, tipo, mensaje);
        this.idSolicitud = idSolicitud;
    }

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Long getIdNotificacion() {
        return idNotificacion;
    }

    public void setIdNotificacion(Long idNotificacion) {
        this.idNotificacion = idNotificacion;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdReporte() {
        return idReporte;
    }

    public void setIdReporte(Long idReporte) {
        this.idReporte = idReporte;
    }

    public Long getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(Long idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public boolean isArchivada() {
        return archivada;
    }

    public void setArchivada(boolean archivada) {
        this.archivada = archivada;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
