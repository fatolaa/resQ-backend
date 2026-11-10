package com.resq.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensaje")
public class Mensaje {

    public static final String TEXTO = "TEXTO";
    public static final String IMAGEN = "IMAGEN";
    public static final String ARCHIVO = "ARCHIVO";

    public static final int CONTENIDO_MAX = 2000;

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

    @Column(name = "tipo", length = 20)
    private String tipo = TEXTO;

    @Column(name = "url_adjunto", length = 300)
    private String urlAdjunto;

    @Column(name = "nombre_adjunto", length = 255)
    private String nombreAdjunto;

    @Column(name = "eliminado")
    private boolean eliminado = false;

    public Mensaje() {}

    @PrePersist
    public void prePersist() {
        if (fechaEnvio == null) {
            fechaEnvio = LocalDateTime.now();
        }
        if (tipo == null) {
            tipo = TEXTO;
        }
        if (contenido == null) {
            contenido = "";
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
    public String getTipo() { return tipo == null ? TEXTO : tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getUrlAdjunto() { return urlAdjunto; }
    public void setUrlAdjunto(String urlAdjunto) { this.urlAdjunto = urlAdjunto; }
    public String getNombreAdjunto() { return nombreAdjunto; }
    public void setNombreAdjunto(String nombreAdjunto) { this.nombreAdjunto = nombreAdjunto; }
    public boolean isEliminado() { return eliminado; }
    public void setEliminado(boolean eliminado) { this.eliminado = eliminado; }
}