package com.resq.backend.entity;

import jakarta.persistence.*;

/**
 * HU-36 / HU-36a: preferencias de notificacion del usuario.
 *
 * Un usuario decide que eventos quiere recibir. Existe una fila por usuario
 * (relacion 1 a 1 por id_usuario). Mientras el usuario no haya tocado nada, se
 * trabaja con los valores por defecto (todos los eventos activados), que es lo
 * que devuelve el servicio la primera vez que se consulta.
 */
@Entity
@Table(name = "preferencia_notificacion")
public class PreferenciaNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_preferencia")
    private Long idPreferencia;

    /** Un usuario tiene una sola fila de preferencias. */
    @Column(name = "id_usuario", nullable = false, unique = true)
    private Long idUsuario;

    @Column(name = "notificar_solicitud_aprobada", nullable = false)
    private boolean notificarSolicitudAprobada = true;

    @Column(name = "notificar_solicitud_rechazada", nullable = false)
    private boolean notificarSolicitudRechazada = true;

    @Column(name = "notificar_reporte_cerca", nullable = false)
    private boolean notificarReporteCerca = true;

    @Column(name = "notificar_chat_mensaje", nullable = false)
    private boolean notificarChatMensaje = true;

    public PreferenciaNotificacion() {
    }

    public PreferenciaNotificacion(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdPreferencia() {
        return idPreferencia;
    }

    public void setIdPreferencia(Long idPreferencia) {
        this.idPreferencia = idPreferencia;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public boolean isNotificarSolicitudAprobada() {
        return notificarSolicitudAprobada;
    }

    public void setNotificarSolicitudAprobada(boolean notificarSolicitudAprobada) {
        this.notificarSolicitudAprobada = notificarSolicitudAprobada;
    }

    public boolean isNotificarSolicitudRechazada() {
        return notificarSolicitudRechazada;
    }

    public void setNotificarSolicitudRechazada(boolean notificarSolicitudRechazada) {
        this.notificarSolicitudRechazada = notificarSolicitudRechazada;
    }

    public boolean isNotificarReporteCerca() {
        return notificarReporteCerca;
    }

    public void setNotificarReporteCerca(boolean notificarReporteCerca) {
        this.notificarReporteCerca = notificarReporteCerca;
    }

    public boolean isNotificarChatMensaje() {
        return notificarChatMensaje;
    }

    public void setNotificarChatMensaje(boolean notificarChatMensaje) {
        this.notificarChatMensaje = notificarChatMensaje;
    }
}
