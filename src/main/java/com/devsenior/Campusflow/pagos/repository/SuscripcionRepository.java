package com.devsenior.Campusflow.pagos.repository;

import java.util.Optional;

import com.devsenior.Campusflow.pagos.model.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuscripcionRepository
        extends JpaRepository<Suscripcion, Long> {

    Optional<Suscripcion> findByStripeCheckoutSessionId(
            String stripeCheckoutSessionId);

    Optional<Suscripcion> findByStripeSubscriptionId(
            String stripeSubscriptionId);
}
