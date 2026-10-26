
package com.devsenior.Campusflow.pagos.model;

import com.devsenior.Campusflow.usuarios.model.Usuario;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos",
       uniqueConstraints = {
           @UniqueConstraint(
               name = "uk_pagos_checkout_session",
               columnNames = "stripe_session_id"
           )
       })
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "stripe_session_id",
            nullable = false, length = 255)
    private String stripeSessionId;

    @Column(name = "stripe_payment_intent_id",
            length = 255)
    private String stripePaymentIntentId;

    @Column(name = "monto_centavos", nullable = false)
    private Long montoCentavos;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public Pago() {
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String id) {
        this.stripePaymentIntentId = id;
    }

    public Long getMontoCentavos() {
        return montoCentavos;
    }

    public void setMontoCentavos(Long montoCentavos) {
        this.montoCentavos = montoCentavos;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
