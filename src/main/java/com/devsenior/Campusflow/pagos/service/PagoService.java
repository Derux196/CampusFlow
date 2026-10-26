
package com.devsenior.Campusflow.pagos.service;

import com.devsenior.Campusflow.pagos.model.Pago;
import com.devsenior.Campusflow.pagos.repository.PagoRepository;
import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;
import com.stripe.model.checkout.Session;

import java.time.LocalDateTime;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;

    public PagoService(
            PagoRepository pagoRepository,
            UsuarioRepository usuarioRepository) {

        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ResultadoRegistroPago registrarPagoCheckout(Session session) {

        String stripeSessionId = session.getId();

        if (!"paid".equals(session.getPaymentStatus())) {
            return ResultadoRegistroPago.IGNORADO;
        }

        if (!Long.valueOf(2000L).equals(session.getAmountTotal())
                || !"usd".equalsIgnoreCase(session.getCurrency())) {
            return ResultadoRegistroPago.IGNORADO;
        }

        if (pagoRepository.existsByStripeSessionId(stripeSessionId)) {
            return ResultadoRegistroPago.YA_REGISTRADO;
        }

        String clientReferenceId = session.getClientReferenceId();

        if (clientReferenceId == null || clientReferenceId.isBlank()) {
            return ResultadoRegistroPago.IGNORADO;
        }

        Long usuarioId;

        try {
            usuarioId = Long.valueOf(clientReferenceId);
        } catch (NumberFormatException ex) {
            return ResultadoRegistroPago.IGNORADO;
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElse(null);

        if (usuario == null) {
            return ResultadoRegistroPago.IGNORADO;
        }

        Pago pago = new Pago();

        pago.setUsuario(usuario);
        pago.setStripeSessionId(stripeSessionId);
        pago.setStripePaymentIntentId(session.getPaymentIntent());
        pago.setEstado("PAGADO");
        pago.setMoneda("USD");
        pago.setMontoCentavos(session.getAmountTotal());
        pago.setFechaCreacion(LocalDateTime.now());

        pagoRepository.save(pago);

        return ResultadoRegistroPago.REGISTRADO;
    }

    public enum ResultadoRegistroPago {
        REGISTRADO,
        YA_REGISTRADO,
        IGNORADO
    }
}
