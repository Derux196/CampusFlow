
package com.devsenior.Campusflow.pagos.controller;

import com.devsenior.Campusflow.pagos.service.StripeCheckoutService;
import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;
import com.stripe.exception.StripeException;

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
    private final UsuarioRepository usuarioRepository;
    private final String precioPagoUnico;

    public StripeCheckoutController(
            StripeCheckoutService checkoutService,
            UsuarioRepository usuarioRepository,
            @Value("${stripe.price-pago-unico}") String precioPagoUnico) {

        this.checkoutService = checkoutService;
        this.usuarioRepository = usuarioRepository;
        this.precioPagoUnico = precioPagoUnico;
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
}
