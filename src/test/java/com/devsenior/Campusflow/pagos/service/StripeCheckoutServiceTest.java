package com.devsenior.Campusflow.pagos.service;

import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StripeCheckoutServiceTest {

    @Test
    void debeCrearParametrosParaSuscripcionMensualConMetadata() {
        StripeCheckoutService service = new StripeCheckoutService(
                "sk_test_placeholder",
                "http://localhost:8080/pago-exitoso.html",
                "http://localhost:8080/pago-cancelado.html"
        );

        SessionCreateParams params =
                service.construirParametrosSuscripcion(
                        "price_individual_test",
                        4L
                );

        assertEquals(
                SessionCreateParams.Mode.SUBSCRIPTION,
                params.getMode()
        );
        assertEquals("4", params.getClientReferenceId());
        assertEquals("4", params.getMetadata().get("usuarioId"));
        assertEquals("INDIVIDUAL", params.getMetadata().get("plan"));
        assertEquals(
                "price_individual_test",
                params.getMetadata().get("priceId")
        );
        assertEquals(
                "INDIVIDUAL",
                params.getSubscriptionData()
                        .getMetadata()
                        .get("plan")
        );
        assertEquals(
                "price_individual_test",
                params.getLineItems().get(0).getPrice()
        );
    }
}
