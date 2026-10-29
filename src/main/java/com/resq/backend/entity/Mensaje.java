package com.resq.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensaje")
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long idMensaje;

    @Column(name = "id_emisor", nullable = false)
    private Long idEmisor;

    @Column(name = "id_receptor", nullable = false)
    private Long idReceptor;

    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "archivado_emisor", nullable = false)
    private boolean archivadoEmisor = false;

    @Column(name = "archivado_receptor", nullable = false)
    private boolean archivadoReceptor = false;

    @Column(name = "leido", nullable = false)
    private boolean leido = false;

    public Mensaje() {}

    @PrePersist
    public void prePersist() {
        if (fechaEnvio == null) {
            fechaEnvio = LocalDateTime.now();
        }
    }

    // Getters y Setters (Genera los tuyos o pide a tu IDE que los cree)
    public Long getIdMensaje() { return idMensaje; }
    public void setIdMensaje(Long idMensaje) { this.idMensaje = idMensaje; }
    public Long getIdEmisor() { return idEmisor; }
    public void setIdEmisor(Long idEmisor) { this.idEmisor = idEmisor; }
    public Long getIdReceptor() { return idReceptor; }
    public void setIdReceptor(Long idReceptor) { this.idReceptor = idReceptor; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
    public boolean isArchivadoEmisor() { return archivadoEmisor; }
    public void setArchivadoEmisor(boolean archivadoEmisor) { this.archivadoEmisor = archivadoEmisor; }
    public boolean isArchivadoReceptor() { return archivadoReceptor; }
    public void setArchivadoReceptor(boolean archivadoReceptor) { this.archivadoReceptor = archivadoReceptor; }
    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }
}