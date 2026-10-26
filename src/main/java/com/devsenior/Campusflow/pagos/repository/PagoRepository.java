
package com.devsenior.Campusflow.pagos.repository;

import com.devsenior.Campusflow.pagos.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByStripeSessionId(String stripeSessionId);

    boolean existsByStripeSessionId(String stripeSessionId);
}
