package com.devsenior.Campusflow.pagos.service;

import java.time.LocalDateTime;
import java.util.Map;

import com.devsenior.Campusflow.pagos.model.EstadoEventoStripe;
import com.devsenior.Campusflow.pagos.model.EstadoSuscripcion;
import com.devsenior.Campusflow.pagos.model.EventoStripe;
import com.devsenior.Campusflow.pagos.model.PlanSuscripcion;
import com.devsenior.Campusflow.pagos.model.Suscripcion;
import com.devsenior.Campusflow.pagos.repository.EventoStripeRepository;
import com.devsenior.Campusflow.pagos.repository.SuscripcionRepository;
import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuscripcionService {

    private static final long MONTO_PLAN_INDIVIDUAL_CENTAVOS = 1000L;
    private static final String MONEDA_USD = "usd";
    private static final String PLAN_INDIVIDUAL = "INDIVIDUAL";

    private final SuscripcionRepository suscripcionRepository;
    private final EventoStripeRepository eventoStripeRepository;
    private final String precioPlanIndividual;

    public SuscripcionService(
            SuscripcionRepository suscripcionRepository,
            EventoStripeRepository eventoStripeRepository,
            @Value("${stripe.price-plan-individual}")
            String precioPlanIndividual) {

        this.suscripcionRepository = suscripcionRepository;
        this.eventoStripeRepository = eventoStripeRepository;
        this.precioPlanIndividual = precioPlanIndividual;
    }

    @Transactional
    public Suscripcion registrarPendiente(
            Usuario usuario,
            Session session) {

        String sessionId = session.getId();

        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalStateException(
                    "Stripe no devolvio una sesion Checkout"
            );
        }

        return suscripcionRepository
                .findByStripeCheckoutSessionId(sessionId)
                .orElseGet(() -> {
                    Suscripcion suscripcion = new Suscripcion();
                    suscripcion.setUsuario(usuario);
                    suscripcion.setPlan(PlanSuscripcion.INDIVIDUAL);
                    suscripcion.setEstado(EstadoSuscripcion.PENDIENTE);
                    suscripcion.setStripeCheckoutSessionId(sessionId);
                    suscripcion.setFechaCreacion(LocalDateTime.now());
                    return suscripcionRepository.save(suscripcion);
                });
    }

    @Transactional
    public ResultadoProcesamientoSuscripcion procesarCheckoutCompletado(
            String stripeEventId,
            String tipoEvento,
            Session session,
            Subscription stripeSubscription) {

        if (eventoStripeRepository
                .findByStripeEventId(stripeEventId)
                .isPresent()) {
            return ResultadoProcesamientoSuscripcion.EVENTO_DUPLICADO;
        }

        Suscripcion suscripcion = suscripcionRepository
                .findByStripeCheckoutSessionId(session.getId())
                .orElse(null);

        if (suscripcion == null
                || !esCheckoutSuscripcionValido(
                        session,
                        stripeSubscription,
                        suscripcion
                )) {
            registrarEvento(
                    stripeEventId,
                    tipoEvento,
                    EstadoEventoStripe.IGNORADO
            );
            return ResultadoProcesamientoSuscripcion.IGNORADA;
        }

        if (suscripcion.getEstado() == EstadoSuscripcion.ACTIVA) {
            registrarEvento(
                    stripeEventId,
                    tipoEvento,
                    EstadoEventoStripe.PROCESADO
            );
            return ResultadoProcesamientoSuscripcion.YA_ACTIVA;
        }

        if (suscripcion.getEstado() == EstadoSuscripcion.CANCELADA) {
            registrarEvento(
                    stripeEventId,
                    tipoEvento,
                    EstadoEventoStripe.IGNORADO
            );
            return ResultadoProcesamientoSuscripcion.IGNORADA;
        }

        suscripcion.setStripeSubscriptionId(
                stripeSubscription.getId()
        );
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setFechaActivacion(LocalDateTime.now());
        suscripcionRepository.save(suscripcion);
        registrarEvento(
                stripeEventId,
                tipoEvento,
                EstadoEventoStripe.PROCESADO
        );

        return ResultadoProcesamientoSuscripcion.ACTIVADA;
    }

    private boolean esCheckoutSuscripcionValido(
            Session session,
            Subscription stripeSubscription,
            Suscripcion suscripcion) {

        if (!"subscription".equals(session.getMode())
                || !"paid".equals(session.getPaymentStatus())
                || !Long.valueOf(MONTO_PLAN_INDIVIDUAL_CENTAVOS)
                        .equals(session.getAmountTotal())
                || !MONEDA_USD.equalsIgnoreCase(
                        session.getCurrency()
                )
                || !esReferenciaUsuarioValida(session, suscripcion)
                || !esMetadataValida(
                        session.getMetadata(),
                        suscripcion
                )
                || !esMetadataValida(
                        stripeSubscription.getMetadata(),
                        suscripcion
                )
                || stripeSubscription.getId() == null
                || stripeSubscription.getId().isBlank()
                || !"active".equals(stripeSubscription.getStatus())) {
            return false;
        }

        return suscripcionRepository
                .findByStripeSubscriptionId(stripeSubscription.getId())
                .map(existente -> existente.getId()
                        .equals(suscripcion.getId()))
                .orElse(true);
    }

    private boolean esReferenciaUsuarioValida(
            Session session,
            Suscripcion suscripcion) {

        return String.valueOf(suscripcion.getUsuario().getId())
                .equals(session.getClientReferenceId());
    }

    private boolean esMetadataValida(
            Map<String, String> metadata,
            Suscripcion suscripcion) {

        return metadata != null
                && String.valueOf(suscripcion.getUsuario().getId())
                        .equals(metadata.get("usuarioId"))
                && PLAN_INDIVIDUAL.equals(metadata.get("plan"))
                && precioPlanIndividual.equals(metadata.get("priceId"));
    }

    private void registrarEvento(
            String stripeEventId,
            String tipoEvento,
            EstadoEventoStripe estado) {

        eventoStripeRepository.save(new EventoStripe(
                stripeEventId,
                tipoEvento,
                estado,
                LocalDateTime.now()
        ));
    }

    public enum ResultadoProcesamientoSuscripcion {
        ACTIVADA,
        YA_ACTIVA,
        EVENTO_DUPLICADO,
        IGNORADA
    }
}
