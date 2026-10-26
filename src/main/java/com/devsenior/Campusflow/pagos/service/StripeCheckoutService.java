
package com.devsenior.Campusflow.pagos.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeCheckoutService {

    private final String urlExito;
    private final String urlCancelado;

    public StripeCheckoutService(
            @Value("${stripe.secret-key}") String secretKey,
            @Value("${stripe.url-exito}") String urlExito,
            @Value("${stripe.url-cancelado}") String urlCancelado) {

        Stripe.apiKey = secretKey;
        this.urlExito = urlExito;
        this.urlCancelado = urlCancelado;
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
                        urlExito
                    )
                    .setCancelUrl(
                        urlCancelado
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

    public Session crearSesionSuscripcion(
            String priceId,
            Long usuarioId) throws StripeException {

        validarPrecioMensual(priceId);

        return Session.create(
                construirParametrosSuscripcion(priceId, usuarioId)
        );
    }

    SessionCreateParams construirParametrosSuscripcion(
            String priceId,
            Long usuarioId) {

        String usuarioIdTexto = String.valueOf(usuarioId);

        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setClientReferenceId(usuarioIdTexto)
                .putMetadata("usuarioId", usuarioIdTexto)
                .putMetadata("plan", "INDIVIDUAL")
                .putMetadata("priceId", priceId)
                .setSubscriptionData(
                        SessionCreateParams.SubscriptionData.builder()
                                .putMetadata("usuarioId", usuarioIdTexto)
                                .putMetadata("plan", "INDIVIDUAL")
                                .putMetadata("priceId", priceId)
                                .build()
                )
                .setSuccessUrl(urlExito)
                .setCancelUrl(urlCancelado)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build()
                )
                .build();
    }

    private void validarPrecioMensual(String priceId)
            throws StripeException {

        Price price = Price.retrieve(priceId);

        if (price.getRecurring() == null
                || !"month".equals(price.getRecurring().getInterval())) {
            throw new IllegalStateException(
                    "El precio configurado no es mensual recurrente"
            );
        }
    }
}
