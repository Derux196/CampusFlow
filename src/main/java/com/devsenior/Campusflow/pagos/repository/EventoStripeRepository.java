package com.devsenior.Campusflow.pagos.repository;

import java.util.Optional;

import com.devsenior.Campusflow.pagos.model.EventoStripe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoStripeRepository
        extends JpaRepository<EventoStripe, Long> {

    Optional<EventoStripe> findByStripeEventId(String stripeEventId);
}
