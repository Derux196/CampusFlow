package com.devsenior.Campusflow.pagos.controller;

import com.devsenior.Campusflow.pagos.service.PagoService;
import com.devsenior.Campusflow.pagos.service.SuscripcionService;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeWebhookControllerTest {

    private static final String WEBHOOK_SECRET = "whsec_test_placeholder";
    private static final String PAYLOAD = """
            {
              "id": "evt_test_subscription",
              "type": "checkout.session.completed",
              "data": {
                "object": {
                  "id": "cs_test_subscription"
                }
              }
            }
            """;

    @Mock
    private PagoService pagoService;

    @Mock
    private SuscripcionService suscripcionService;

    @Mock
    private Session session;

    @Mock
    private Subscription stripeSubscription;

    private StripeWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new StripeWebhookController(
                WEBHOOK_SECRET,
                pagoService,
                suscripcionService
        );
    }

    @Test
    void debeRechazarFirmaAusente() {
        ResponseEntity<String> response =
                controller.recibirWebhook(PAYLOAD, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void debeRechazarFirmaInvalida() {
        ResponseEntity<String> response =
                controller.recibirWebhook(PAYLOAD, "t=1,v1=invalid");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void debeProcesarSuscripcionFirmadaValida() throws Exception {
        String signature = Webhook.Signature.generateSignatureHeader(
                PAYLOAD,
                WEBHOOK_SECRET
        );
        when(session.getId()).thenReturn("cs_test_subscription");
        when(session.getMode()).thenReturn("subscription");
        when(session.getSubscription()).thenReturn("sub_test_subscription");
        when(suscripcionService.procesarCheckoutCompletado(
                eq("evt_test_subscription"),
                eq("checkout.session.completed"),
                eq(session),
                eq(stripeSubscription)
        )).thenReturn(
                SuscripcionService.ResultadoProcesamientoSuscripcion.ACTIVADA
        );

        try (MockedStatic<Session> sessionMock =
                     org.mockito.Mockito.mockStatic(Session.class);
             MockedStatic<Subscription> subscriptionMock =
                     org.mockito.Mockito.mockStatic(Subscription.class)) {
            sessionMock.when(
                    () -> Session.retrieve("cs_test_subscription")
            ).thenReturn(session);
            subscriptionMock.when(
                    () -> Subscription.retrieve("sub_test_subscription")
            ).thenReturn(stripeSubscription);

            ResponseEntity<String> response =
                    controller.recibirWebhook(PAYLOAD, signature);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Evento recibido", response.getBody());
        }

        verify(suscripcionService).procesarCheckoutCompletado(
                "evt_test_subscription",
                "checkout.session.completed",
                session,
                stripeSubscription
        );
    }

    @Test
    void debeDescartarSuscripcionFirmadaIgnoradaSinErrorServidor()
            throws Exception {
        String signature = Webhook.Signature.generateSignatureHeader(
                PAYLOAD,
                WEBHOOK_SECRET
        );
        when(session.getId()).thenReturn("cs_test_subscription");
        when(session.getMode()).thenReturn("subscription");
        when(session.getSubscription()).thenReturn("sub_test_subscription");
        when(suscripcionService.procesarCheckoutCompletado(
                any(),
                any(),
                eq(session),
                eq(stripeSubscription)
        )).thenReturn(
                SuscripcionService.ResultadoProcesamientoSuscripcion.IGNORADA
        );

        try (MockedStatic<Session> sessionMock =
                     org.mockito.Mockito.mockStatic(Session.class);
             MockedStatic<Subscription> subscriptionMock =
                     org.mockito.Mockito.mockStatic(Subscription.class)) {
            sessionMock.when(
                    () -> Session.retrieve("cs_test_subscription")
            ).thenReturn(session);
            subscriptionMock.when(
                    () -> Subscription.retrieve("sub_test_subscription")
            ).thenReturn(stripeSubscription);

            ResponseEntity<String> response =
                    controller.recibirWebhook(PAYLOAD, signature);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Evento descartado", response.getBody());
        }
    }

    @Test
    void debeResponderErrorServidorAnteFalloTecnicoDeStripe()
            throws Exception {
        String signature = Webhook.Signature.generateSignatureHeader(
                PAYLOAD,
                WEBHOOK_SECRET
        );

        try (MockedStatic<Session> sessionMock =
                     org.mockito.Mockito.mockStatic(Session.class)) {
            sessionMock.when(
                    () -> Session.retrieve("cs_test_subscription")
            ).thenThrow(new IllegalStateException("stripe unavailable"));

            ResponseEntity<String> response =
                    controller.recibirWebhook(PAYLOAD, signature);

            assertEquals(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    response.getStatusCode()
            );
        }
    }
}
