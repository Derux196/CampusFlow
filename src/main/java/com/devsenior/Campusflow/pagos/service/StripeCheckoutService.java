
package com.devsenior.Campusflow.pagos.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeCheckoutService {

    public StripeCheckoutService(
            @Value("${stripe.secret-key}") String secretKey) {
        Stripe.apiKey = secretKey;
    }

    public String crearSesionPagoUnico(
            String priceId,
            Long usuarioId) throws StripeException {

        SessionCreateParams params =
                SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)

                    .setClientReferenceId(
                        String.valueOf(usuarioId)
                    )
                    .setSuccessUrl(
                        "http://localhost:8080/pago-exitoso.html"
                    )
                    .setCancelUrl(
                        "http://localhost:8080/pago-cancelado.html"
                    )
                    .addLineItem(
                        SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build()
                    )
                    .build();

        Session session = Session.create(params);

        return session.getUrl();
    }
}
