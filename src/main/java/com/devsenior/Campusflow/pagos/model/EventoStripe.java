package com.devsenior.Campusflow.pagos.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "eventos_stripe",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_eventos_stripe_event_id",
                columnNames = "stripe_event_id"
        )
)
public class EventoStripe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stripe_event_id", nullable = false, length = 255)
    private String stripeEventId;

    @Column(name = "tipo_evento", nullable = false, length = 100)
    private String tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoEventoStripe estado;

    @Column(name = "fecha_procesamiento", nullable = false)
    private LocalDateTime fechaProcesamiento;

    public EventoStripe() {
    }

    public EventoStripe(
            String stripeEventId,
            String tipoEvento,
            EstadoEventoStripe estado,
            LocalDateTime fechaProcesamiento) {

        this.stripeEventId = stripeEventId;
        this.tipoEvento = tipoEvento;
        this.estado = estado;
        this.fechaProcesamiento = fechaProcesamiento;
    }

    public Long getId() {
        return id;
    }

    public String getStripeEventId() {
        return stripeEventId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public EstadoEventoStripe getEstado() {
        return estado;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }
}
