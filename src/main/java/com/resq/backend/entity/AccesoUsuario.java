package com.resq.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * HU-34: registro de cada inicio de sesion exitoso.
 *
 * Un usuario cuenta como "activo" en un periodo si inicio sesion al menos una vez
 * dentro de el. Se guarda una fila por login (y no solo la fecha del ultimo
 * acceso) para poder comparar periodos pasados: con un unico "ultimo acceso", el
 * login de hoy borraria la actividad del mes anterior.
 */
@Entity
@Table(name = "acceso_usuario", indexes = @Index(name = "idx_acceso_usuario_fecha", columnList = "fecha"))
public class AccesoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_acceso")
    private Long idAcceso;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public AccesoUsuario() {
    }

    public AccesoUsuario(Long idUsuario, LocalDateTime fecha) {
        this.idUsuario = idUsuario;
        this.fecha = fecha;
    }

    public Long getIdAcceso() {
        return idAcceso;
    }

    public void setIdAcceso(Long idAcceso) {
        this.idAcceso = idAcceso;
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
