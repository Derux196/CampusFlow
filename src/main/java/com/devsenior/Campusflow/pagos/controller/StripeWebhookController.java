
package com.devsenior.Campusflow.pagos.controller;

import com.devsenior.Campusflow.pagos.service.PagoService;
import com.devsenior.Campusflow.pagos.service.SuscripcionService;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class StripeWebhookController {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(StripeWebhookController.class);

    private final String webhookSecret;
    private final PagoService pagoService;
    private final SuscripcionService suscripcionService;
    private final JsonMapper jsonMapper;

    public StripeWebhookController(
            @Value("${stripe.webhook-secret}") String webhookSecret,
            PagoService pagoService,
            SuscripcionService suscripcionService) {

        this.webhookSecret = webhookSecret;
        this.pagoService = pagoService;
        this.suscripcionService = suscripcionService;
        this.jsonMapper = new JsonMapper();
    }

    @PostMapping("/api/stripe/webhook")
    public ResponseEntity<String> recibirWebhook(
            @RequestBody String payload,
            @RequestHeader(
                    value = "Stripe-Signature",
                    required = false
            ) String signature) {

        // 1. Comprobar que existe la firma
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Falta Stripe-Signature");
        }

        try {

            // 2. Verificar la autenticidad del evento
            Event evento = Webhook.constructEvent(
                    payload,
                    signature,
                    webhookSecret
            );

            // 3. Procesar solamente Checkout completado
            if ("checkout.session.completed".equals(
                    evento.getType())) {

                // Obtener el JSON del objeto del evento
                String rawJson = evento
                        .getDataObjectDeserializer()
                        .getRawJson();

                // Interpretarlo con Jackson
                JsonNode objeto = jsonMapper.readTree(rawJson);

                if (objeto == null) {
                    throw new IllegalStateException(
                            "El evento no contiene datos"
                    );
                }

                JsonNode idNodo = objeto.get("id");

                if (idNodo == null || idNodo.isNull()) {
                    throw new IllegalStateException(
                            "El evento no contiene ID de sesion"
                    );
                }

                String sessionId = idNodo.asText();

                // 4. Verificar formato del ID
                if (!sessionId.startsWith("cs_")) {
                    throw new IllegalStateException(
                            "ID de sesion Checkout invalido"
                    );
                }

                // 5. Consultar la sesion directamente a Stripe
                Session session = Session.retrieve(sessionId);

                if (!sessionId.equals(session.getId())) {
                    throw new IllegalStateException(
                            "La sesion recuperada no coincide"
                    );
                }

                if ("subscription".equals(session.getMode())) {
                    return procesarSuscripcion(
                            evento,
                            session
                    );
                }

                PagoService.ResultadoRegistroPago resultado =
                        pagoService.registrarPagoCheckout(session);

                if (resultado == PagoService.ResultadoRegistroPago.IGNORADO) {
                    return ResponseEntity.ok("Evento descartado");
                }
            }

            return ResponseEntity.ok("Evento recibido");

        } catch (SignatureVerificationException ex) {

            return ResponseEntity.badRequest()
                    .body("Firma Stripe invalida");

        } catch (Exception ex) {

            LOGGER.error("Error tecnico al procesar un webhook de Stripe");

            return ResponseEntity.internalServerError()
                    .body("Error al procesar webhook");
        }
    }

    private ResponseEntity<String> procesarSuscripcion(
            Event evento,
            Session session) throws Exception {

        String subscriptionId = session.getSubscription();

        if (subscriptionId == null || subscriptionId.isBlank()) {
            return ResponseEntity.ok("Evento descartado");
        }

        Subscription stripeSubscription =
                Subscription.retrieve(subscriptionId);

        SuscripcionService.ResultadoProcesamientoSuscripcion resultado =
                suscripcionService.procesarCheckoutCompletado(
                        evento.getId(),
                        evento.getType(),
                        session,
                        stripeSubscription
                );

        if (resultado
                == SuscripcionService.ResultadoProcesamientoSuscripcion
                        .IGNORADA) {
            return ResponseEntity.ok("Evento descartado");
        }

        return ResponseEntity.ok("Evento recibido");
    }
}
