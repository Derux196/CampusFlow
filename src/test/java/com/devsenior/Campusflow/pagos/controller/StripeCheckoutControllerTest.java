package com.devsenior.Campusflow.pagos.controller;

import java.util.Optional;

import com.devsenior.Campusflow.pagos.service.StripeCheckoutService;
import com.devsenior.Campusflow.pagos.service.SuscripcionService;
import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeCheckoutControllerTest {

    @Mock
    private StripeCheckoutService checkoutService;

    @Mock
    private SuscripcionService suscripcionService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private Session session;

    private StripeCheckoutController controller;

    @BeforeEach
    void setUp() {
        controller = new StripeCheckoutController(
                checkoutService,
                suscripcionService,
                usuarioRepository,
                "price_one_time",
                "price_individual"
        );
    }

    @Test
    void debeCrearSuscripcionParaUsuarioAutenticado() throws Exception {
        Usuario usuario = new Usuario("Student", "student@test.com");
        usuario.setId(4L);
        when(authentication.getName()).thenReturn("student@test.com");
        when(usuarioRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(usuario));
        when(checkoutService.crearSesionSuscripcion(
                "price_individual",
                4L
        )).thenReturn(session);
        when(session.getUrl()).thenReturn(
                "https://checkout.stripe.test/session"
        );

        ResponseEntity<java.util.Map<String, String>> response =
                controller.crearSuscripcion(authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(
                "https://checkout.stripe.test/session",
                response.getBody().get("url")
        );
        verify(checkoutService).crearSesionSuscripcion(
                "price_individual",
                4L
        );
        verify(suscripcionService).registrarPendiente(usuario, session);
    }
}
