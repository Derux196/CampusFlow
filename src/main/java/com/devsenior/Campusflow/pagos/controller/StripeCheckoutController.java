
package com.devsenior.Campusflow.pagos.controller;

import com.devsenior.Campusflow.pagos.service.StripeCheckoutService;
import com.devsenior.Campusflow.pagos.service.SuscripcionService;
import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stripe/checkout")
public class StripeCheckoutController {

    private final StripeCheckoutService checkoutService;
    private final SuscripcionService suscripcionService;
    private final UsuarioRepository usuarioRepository;
    private final String precioPagoUnico;
    private final String precioPlanIndividual;

    public StripeCheckoutController(
            StripeCheckoutService checkoutService,
            SuscripcionService suscripcionService,
            UsuarioRepository usuarioRepository,
            @Value("${stripe.price-pago-unico}") String precioPagoUnico,
            @Value("${stripe.price-plan-individual}")
            String precioPlanIndividual) {

        this.checkoutService = checkoutService;
        this.suscripcionService = suscripcionService;
        this.usuarioRepository = usuarioRepository;
        this.precioPagoUnico = precioPagoUnico;
        this.precioPlanIndividual = precioPlanIndividual;
    }

    @PostMapping("/pago-unico")
    public ResponseEntity<Map<String, String>> crearPagoUnico(
            Authentication authentication) throws StripeException {

        String email = authentication.getName();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuario autenticado no encontrado"
                ));

        String url = checkoutService.crearSesionPagoUnico(
                precioPagoUnico,
                usuario.getId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("url", url));
    }

    @PostMapping("/suscripcion")
    public ResponseEntity<Map<String, String>> crearSuscripcion(
            Authentication authentication) throws StripeException {

        Usuario usuario = usuarioRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Usuario autenticado no encontrado"
                ));

        Session session = checkoutService.crearSesionSuscripcion(
                precioPlanIndividual,
                usuario.getId()
        );

        suscripcionService.registrarPendiente(usuario, session);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("url", session.getUrl()));
    }
}
