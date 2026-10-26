package com.devsenior.Campusflow.pagos.service;

import java.util.Map;
import java.util.Optional;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuscripcionServiceTest {

    private static final String PRICE_ID = "price_individual_test";

    @Mock
    private SuscripcionRepository suscripcionRepository;

    @Mock
    private EventoStripeRepository eventoStripeRepository;

    @Mock
    private Session session;

    @Mock
    private Subscription stripeSubscription;

    private SuscripcionService suscripcionService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        suscripcionService = new SuscripcionService(
                suscripcionRepository,
                eventoStripeRepository,
                PRICE_ID
        );
    }

    @Test
    void debeRegistrarSuscripcionPendienteParaSesionPropia() {
        Usuario usuario = usuario();
        when(session.getId()).thenReturn("cs_test_subscription");
        when(suscripcionRepository.findByStripeCheckoutSessionId(
                "cs_test_subscription"))
                .thenReturn(Optional.empty());
        when(suscripcionRepository.save(
                org.mockito.ArgumentMatchers.any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion resultado = suscripcionService.registrarPendiente(
                usuario,
                session
        );

        assertSame(usuario, resultado.getUsuario());
        assertEquals(PlanSuscripcion.INDIVIDUAL, resultado.getPlan());
        assertEquals(EstadoSuscripcion.PENDIENTE, resultado.getEstado());
        assertEquals(
                "cs_test_subscription",
                resultado.getStripeCheckoutSessionId()
        );
        verify(suscripcionRepository).save(resultado);
    }

    @Test
    void debeActivarSuscripcionPendienteConEventoValido() {
        Suscripcion pendiente = suscripcionPendiente();
        configurarCheckoutValido();
        when(eventoStripeRepository.findByStripeEventId("evt_test"))
                .thenReturn(Optional.empty());
        when(suscripcionRepository.findByStripeCheckoutSessionId(
                "cs_test_subscription"))
                .thenReturn(Optional.of(pendiente));
        when(suscripcionRepository.findByStripeSubscriptionId("sub_test"))
                .thenReturn(Optional.empty());

        var resultado = suscripcionService.procesarCheckoutCompletado(
                "evt_test",
                "checkout.session.completed",
                session,
                stripeSubscription
        );

        assertEquals(
                SuscripcionService.ResultadoProcesamientoSuscripcion.ACTIVADA,
                resultado
        );
        assertEquals(EstadoSuscripcion.ACTIVA, pendiente.getEstado());
        assertEquals("sub_test", pendiente.getStripeSubscriptionId());
        verify(suscripcionRepository).save(pendiente);
        verificarEventoGuardado(EstadoEventoStripe.PROCESADO);
    }

    @Test
    void debeIgnorarEventoSinMetadataValida() {
        Suscripcion pendiente = suscripcionPendiente();
        configurarSesionValida();
        when(session.getMetadata()).thenReturn(Map.of(
                "usuarioId", "4",
                "plan", "OTRO",
                "priceId", PRICE_ID
        ));
        when(eventoStripeRepository.findByStripeEventId("evt_test"))
                .thenReturn(Optional.empty());
        when(suscripcionRepository.findByStripeCheckoutSessionId(
                "cs_test_subscription"))
                .thenReturn(Optional.of(pendiente));

        var resultado = suscripcionService.procesarCheckoutCompletado(
                "evt_test",
                "checkout.session.completed",
                session,
                stripeSubscription
        );

        assertEquals(
                SuscripcionService.ResultadoProcesamientoSuscripcion.IGNORADA,
                resultado
        );
        assertEquals(EstadoSuscripcion.PENDIENTE, pendiente.getEstado());
        verify(suscripcionRepository, never()).save(pendiente);
        verificarEventoGuardado(EstadoEventoStripe.IGNORADO);
    }

    @Test
    void debeIgnorarEventoParaSesionNoCreadaLocalmente() {
        when(session.getId()).thenReturn("cs_test_subscription");
        when(eventoStripeRepository.findByStripeEventId("evt_test"))
                .thenReturn(Optional.empty());
        when(suscripcionRepository.findByStripeCheckoutSessionId(
                "cs_test_subscription"))
                .thenReturn(Optional.empty());

        var resultado = suscripcionService.procesarCheckoutCompletado(
                "evt_test",
                "checkout.session.completed",
                session,
                stripeSubscription
        );

        assertEquals(
                SuscripcionService.ResultadoProcesamientoSuscripcion.IGNORADA,
                resultado
        );
        verifyNoInteractions(stripeSubscription);
        verificarEventoGuardado(EstadoEventoStripe.IGNORADO);
    }

    @Test
    void debeReconocerEventoDuplicadoSinProcesarlo() {
        when(eventoStripeRepository.findByStripeEventId("evt_test"))
                .thenReturn(Optional.of(new EventoStripe()));

        var resultado = suscripcionService.procesarCheckoutCompletado(
                "evt_test",
                "checkout.session.completed",
                session,
                stripeSubscription
        );

        assertEquals(
                SuscripcionService.ResultadoProcesamientoSuscripcion
                        .EVENTO_DUPLICADO,
                resultado
        );
        verifyNoInteractions(suscripcionRepository);
    }

    @Test
    void debeInformarSuscripcionPreviamenteActiva() {
        Suscripcion activa = suscripcionPendiente();
        activa.setEstado(EstadoSuscripcion.ACTIVA);
        activa.setStripeSubscriptionId("sub_test");
        configurarCheckoutValido();
        when(eventoStripeRepository.findByStripeEventId("evt_test"))
                .thenReturn(Optional.empty());
        when(suscripcionRepository.findByStripeCheckoutSessionId(
                "cs_test_subscription"))
                .thenReturn(Optional.of(activa));
        when(suscripcionRepository.findByStripeSubscriptionId("sub_test"))
                .thenReturn(Optional.empty());

        var resultado = suscripcionService.procesarCheckoutCompletado(
                "evt_test",
                "checkout.session.completed",
                session,
                stripeSubscription
        );

        assertEquals(
                SuscripcionService.ResultadoProcesamientoSuscripcion
                        .YA_ACTIVA,
                resultado
        );
        verify(suscripcionRepository, never()).save(activa);
        verificarEventoGuardado(EstadoEventoStripe.PROCESADO);
    }

    @Test
    void debePropagarErrorTecnicoAlGuardarEvento() {
        Suscripcion pendiente = suscripcionPendiente();
        configurarCheckoutValido();
        when(eventoStripeRepository.findByStripeEventId("evt_test"))
                .thenReturn(Optional.empty());
        when(suscripcionRepository.findByStripeCheckoutSessionId(
                "cs_test_subscription"))
                .thenReturn(Optional.of(pendiente));
        when(suscripcionRepository.findByStripeSubscriptionId("sub_test"))
                .thenReturn(Optional.empty());
        when(eventoStripeRepository.save(
                org.mockito.ArgumentMatchers.any(EventoStripe.class)))
                .thenThrow(new IllegalStateException("database failure"));

        assertThrows(
                IllegalStateException.class,
                () -> suscripcionService.procesarCheckoutCompletado(
                        "evt_test",
                        "checkout.session.completed",
                        session,
                        stripeSubscription
                )
        );
    }

    private void configurarCheckoutValido() {
        configurarSesionValida();
        Map<String, String> metadata = metadataValida();
        when(stripeSubscription.getId()).thenReturn("sub_test");
        when(stripeSubscription.getStatus()).thenReturn("active");
        when(stripeSubscription.getMetadata()).thenReturn(metadata);
    }

    private void configurarSesionValida() {
        Map<String, String> metadata = metadataValida();
        when(session.getId()).thenReturn("cs_test_subscription");
        when(session.getMode()).thenReturn("subscription");
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getAmountTotal()).thenReturn(1000L);
        when(session.getCurrency()).thenReturn("usd");
        when(session.getClientReferenceId()).thenReturn("4");
        when(session.getMetadata()).thenReturn(metadata);
    }

    private Map<String, String> metadataValida() {
        return Map.of(
                "usuarioId", "4",
                "plan", "INDIVIDUAL",
                "priceId", PRICE_ID
        );
    }

    private Suscripcion suscripcionPendiente() {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setUsuario(usuario());
        suscripcion.setPlan(PlanSuscripcion.INDIVIDUAL);
        suscripcion.setEstado(EstadoSuscripcion.PENDIENTE);
        suscripcion.setStripeCheckoutSessionId("cs_test_subscription");
        return suscripcion;
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario("Estudiante", "student@test.com");
        usuario.setId(4L);
        return usuario;
    }

    private void verificarEventoGuardado(EstadoEventoStripe estado) {
        ArgumentCaptor<EventoStripe> captor =
                ArgumentCaptor.forClass(EventoStripe.class);
        verify(eventoStripeRepository).save(captor.capture());
        assertEquals(estado, captor.getValue().getEstado());
    }
}
