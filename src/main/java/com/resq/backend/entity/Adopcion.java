package com.resq.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HU-30: adopción de un animal rescatado. Queda asociada al caso (reporte) del que
 * proviene y a la organización que la registró, junto con los datos del adoptante.
 */
@Entity
@Table(name = "adopcion")
public class Adopcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_adopcion")
    private Long idAdopcion;

    // Caso del que proviene el animal. unique: un caso solo puede adoptarse una vez.
    @Column(name = "id_reporte", nullable = false, unique = true)
    private Long idReporte;

    // Organización que realizó la adopción.
    @Column(name = "id_organizacion", nullable = false)
    private Long idOrganizacion;

    @Column(name = "nombre_adoptante", nullable = false, length = 150)
    private String nombreAdoptante;

    @Column(name = "telefono_adoptante", nullable = false, length = 30)
    private String telefonoAdoptante;

    @Column(name = "email_adoptante", length = 150)
    private String emailAdoptante;

    @Column(name = "fecha_adopcion", nullable = false)
    private LocalDate fechaAdopcion;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    public Adopcion() {
    }

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

    public Long getIdAdopcion() {
        return idAdopcion;
    }

    public void setIdAdopcion(Long idAdopcion) {
        this.idAdopcion = idAdopcion;
    }

    public Long getIdReporte() {
        return idReporte;
    }

    public void setIdReporte(Long idReporte) {
        this.idReporte = idReporte;
    }

    public Long getIdOrganizacion() {
        return idOrganizacion;
    }

    public void setIdOrganizacion(Long idOrganizacion) {
        this.idOrganizacion = idOrganizacion;
    }

    public String getNombreAdoptante() {
        return nombreAdoptante;
    }

    public void setNombreAdoptante(String nombreAdoptante) {
        this.nombreAdoptante = nombreAdoptante;
    }

    public String getTelefonoAdoptante() {
        return telefonoAdoptante;
    }

    public void setTelefonoAdoptante(String telefonoAdoptante) {
        this.telefonoAdoptante = telefonoAdoptante;
    }

    public String getEmailAdoptante() {
        return emailAdoptante;
    }

    public void setEmailAdoptante(String emailAdoptante) {
        this.emailAdoptante = emailAdoptante;
    }

    public LocalDate getFechaAdopcion() {
        return fechaAdopcion;
    }

    public void setFechaAdopcion(LocalDate fechaAdopcion) {
        this.fechaAdopcion = fechaAdopcion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
