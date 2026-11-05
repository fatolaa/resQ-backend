package com.resq.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * HU-40: Registro de bloqueo entre usuarios para evitar la recepción de mensajes en el chat.
 */
@Entity
@Table(name = "bloqueo_usuario",
       uniqueConstraints = @UniqueConstraint(columnNames = {"id_bloqueador", "id_bloqueado"}))
public class BloqueoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bloqueo")
    private Long idBloqueo;

    @Column(name = "id_bloqueador", nullable = false)
    private Long idBloqueador;

    @Column(name = "id_bloqueado", nullable = false)
    private Long idBloqueado;

    @Column(name = "fecha_bloqueo")
    private LocalDateTime fechaBloqueo;

    public BloqueoUsuario() {
    }

    public BloqueoUsuario(Long idBloqueador, Long idBloqueado) {
        this.idBloqueador = idBloqueador;
        this.idBloqueado = idBloqueado;
    }

    @PrePersist
    public void prePersist() {
        if (fechaBloqueo == null) {
            fechaBloqueo = LocalDateTime.now();
        }
    }

    public Long getIdBloqueo() {
        return idBloqueo;
    }

    public void setIdBloqueo(Long idBloqueo) {
        this.idBloqueo = idBloqueo;
    }

    public Long getIdBloqueador() {
        return idBloqueador;
    }

    public void setIdBloqueador(Long idBloqueador) {
        this.idBloqueador = idBloqueador;
    }

    public Long getIdBloqueado() {
        return idBloqueado;
    }

    public void setIdBloqueado(Long idBloqueado) {
        this.idBloqueado = idBloqueado;
    }

    public LocalDateTime getFechaBloqueo() {
        return fechaBloqueo;
    }

    public void setFechaBloqueo(LocalDateTime fechaBloqueo) {
        this.fechaBloqueo = fechaBloqueo;
    }
}
