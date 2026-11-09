package com.resq.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * HU-22: mensaje del chat de un caso. Solo lo ven y lo escriben los participantes del caso
 * (ver ParticipanteCasoService). Puede ser texto, una ubicacion o una foto.
 */
@Entity
@Table(name = "mensaje_caso", indexes = @Index(name = "idx_mensaje_caso_caso_fecha", columnList = "id_caso, fecha_envio"))
public class MensajeCaso {

    public static final String TEXTO = "TEXTO";
    public static final String UBICACION = "UBICACION";
    public static final String FOTO = "FOTO";

    public static final int CONTENIDO_MAX = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long idMensaje;

    /** Id del reporte (caso) al que pertenece el chat. */
    @Column(name = "id_caso", nullable = false)
    private Long idCaso;

    @Column(name = "id_emisor", nullable = false)
    private Long idEmisor;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    /** Texto del mensaje; en una foto o ubicacion es un comentario opcional. */
    @Column(name = "contenido", length = CONTENIDO_MAX)
    private String contenido;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    public MensajeCaso() {
    }

    public Long getIdMensaje() {
        return idMensaje;
    }

    public void setIdMensaje(Long idMensaje) {
        this.idMensaje = idMensaje;
    }

    public Long getIdCaso() {
        return idCaso;
    }

    public void setIdCaso(Long idCaso) {
        this.idCaso = idCaso;
    }

    public Long getIdEmisor() {
        return idEmisor;
    }

    public void setIdEmisor(Long idEmisor) {
        this.idEmisor = idEmisor;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
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

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }
}
