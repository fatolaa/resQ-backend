package com.resq.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * HU-20: organización (refugio, veterinaria o rescatista independiente) registrada
 * por un usuario que actúa como su representante.
 */
@Entity
@Table(name = "organizacion")
public class Organizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_organizacion")
    private Long idOrganizacion;

    // Un usuario representa a una sola organización.
    @Column(name = "id_representante", nullable = false, unique = true)
    private Long idRepresentante;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Column(name = "direccion", nullable = false, length = 255)
    private String direccion;

    @Column(name = "telefono", nullable = false, length = 30)
    private String telefono;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    // Ruta relativa del logo (GET /api/organizaciones/logos/{nombre}); null si no subió uno.
    @Column(name = "logo_url", length = 300)
    private String logoUrl;

    @Column(name = "estado_verificacion", nullable = false, length = 30)
    private String estadoVerificacion;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    public Organizacion() {
    }

    @PrePersist
    public void prePersist() {
        if (estadoVerificacion == null) {
            estadoVerificacion = EstadoVerificacion.PENDIENTE;
        }
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

    /**
     * Regla de negocio de la HU-20: solo una organización verificada puede gestionar
     * casos. Se expone en el JSON para que el frontend lo muestre sin repetir la regla.
     */
    @JsonProperty("puedeGestionarCasos")
    public boolean puedeGestionarCasos() {
        return EstadoVerificacion.VERIFICADA.equals(estadoVerificacion);
    }

    public Long getIdOrganizacion() {
        return idOrganizacion;
    }

    public void setIdOrganizacion(Long idOrganizacion) {
        this.idOrganizacion = idOrganizacion;
    }

    public Long getIdRepresentante() {
        return idRepresentante;
    }

    public void setIdRepresentante(Long idRepresentante) {
        this.idRepresentante = idRepresentante;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getEstadoVerificacion() {
        return estadoVerificacion;
    }

    public void setEstadoVerificacion(String estadoVerificacion) {
        this.estadoVerificacion = estadoVerificacion;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
