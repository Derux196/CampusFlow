package com.devsenior.Campusflow.pagos.repository;

import java.time.LocalDateTime;

import com.devsenior.Campusflow.pagos.model.EstadoEventoStripe;
import com.devsenior.Campusflow.pagos.model.EstadoSuscripcion;
import com.devsenior.Campusflow.pagos.model.EventoStripe;
import com.devsenior.Campusflow.pagos.model.PlanSuscripcion;
import com.devsenior.Campusflow.pagos.model.Suscripcion;
import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class StripePersistenceIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SuscripcionRepository suscripcionRepository;

    @Autowired
    private EventoStripeRepository eventoStripeRepository;

    @Test
    void debePersistirSuscripcionYEventoPorIdentificadoresStripe() {
        Usuario usuario = usuarioRepository.save(
                new Usuario("Student", "student-subscription@test.com")
        );
        Suscripcion suscripcion = nuevaSuscripcion(
                usuario,
                "cs_test_subscription"
        );
        suscripcionRepository.saveAndFlush(suscripcion);
        eventoStripeRepository.saveAndFlush(new EventoStripe(
                "evt_test_subscription",
                "checkout.session.completed",
                EstadoEventoStripe.PROCESADO,
                LocalDateTime.now()
        ));

        assertEquals(
                suscripcion,
                suscripcionRepository
                        .findByStripeCheckoutSessionId(
                                "cs_test_subscription"
                        )
                        .orElseThrow()
        );
        assertEquals(
                EstadoEventoStripe.PROCESADO,
                eventoStripeRepository
                        .findByStripeEventId("evt_test_subscription")
                        .orElseThrow()
                        .getEstado()
        );
    }

    @Test
    void debeImpedirEventosStripeDuplicados() {
        eventoStripeRepository.saveAndFlush(new EventoStripe(
                "evt_duplicate",
                "checkout.session.completed",
                EstadoEventoStripe.PROCESADO,
                LocalDateTime.now()
        ));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> eventoStripeRepository.saveAndFlush(new EventoStripe(
                        "evt_duplicate",
                        "checkout.session.completed",
                        EstadoEventoStripe.PROCESADO,
                        LocalDateTime.now()
                ))
        );
    }

    private Suscripcion nuevaSuscripcion(
            Usuario usuario,
            String checkoutSessionId) {

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setUsuario(usuario);
        suscripcion.setPlan(PlanSuscripcion.INDIVIDUAL);
        suscripcion.setEstado(EstadoSuscripcion.PENDIENTE);
        suscripcion.setStripeCheckoutSessionId(checkoutSessionId);
        suscripcion.setFechaCreacion(LocalDateTime.now());
        return suscripcion;
    }
}
