package com.devsenior.Campusflow.pagos.service;

import java.util.Optional;

import com.devsenior.Campusflow.pagos.model.Pago;
import com.devsenior.Campusflow.pagos.repository.PagoRepository;
import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Session session;

    @InjectMocks
    private PagoService pagoService;

    @Test
    void debeIgnorarCheckoutSinPagoConfirmado() {
        when(session.getPaymentStatus()).thenReturn("unpaid");

        var resultado = pagoService.registrarPagoCheckout(session);

        assertEquals(
                PagoService.ResultadoRegistroPago.IGNORADO,
                resultado
        );
        verifyNoInteractions(pagoRepository, usuarioRepository);
    }

    @Test
    void debeIgnorarCheckoutConImporteIncorrecto() {
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getAmountTotal()).thenReturn(1000L);

        var resultado = pagoService.registrarPagoCheckout(session);

        assertEquals(
                PagoService.ResultadoRegistroPago.IGNORADO,
                resultado
        );
        verifyNoInteractions(pagoRepository, usuarioRepository);
    }

    @Test
    void debeIgnorarCheckoutConMonedaIncorrecta() {
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getAmountTotal()).thenReturn(2000L);
        when(session.getCurrency()).thenReturn("eur");

        var resultado = pagoService.registrarPagoCheckout(session);

        assertEquals(
                PagoService.ResultadoRegistroPago.IGNORADO,
                resultado
        );
        verifyNoInteractions(pagoRepository, usuarioRepository);
    }

    @Test
    void debeIgnorarCheckoutConReferenciaNoNumerica() {
        configurarCheckoutParaReferencia("usuario-cuatro");
        when(pagoRepository.existsByStripeSessionId("cs_test_123"))
                .thenReturn(false);

        var resultado = pagoService.registrarPagoCheckout(session);

        assertEquals(
                PagoService.ResultadoRegistroPago.IGNORADO,
                resultado
        );
        verify(usuarioRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
        verify(pagoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void debeInformarPagoYaRegistrado() {
        configurarCheckoutSinReferencia();
        when(pagoRepository.existsByStripeSessionId("cs_test_123"))
                .thenReturn(true);

        var resultado = pagoService.registrarPagoCheckout(session);

        assertEquals(
                PagoService.ResultadoRegistroPago.YA_REGISTRADO,
                resultado
        );
        verifyNoInteractions(usuarioRepository);
        verify(pagoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void debeRegistrarCheckoutValido() {
        configurarCheckoutParaReferencia("4");
        Usuario usuario = new Usuario("Estudiante", "estudiante@test.com");
        usuario.setId(4L);

        when(pagoRepository.existsByStripeSessionId("cs_test_123"))
                .thenReturn(false);
        when(usuarioRepository.findById(4L))
                .thenReturn(Optional.of(usuario));

        var resultado = pagoService.registrarPagoCheckout(session);
        ArgumentCaptor<Pago> pagoCaptor =
                ArgumentCaptor.forClass(Pago.class);

        assertEquals(
                PagoService.ResultadoRegistroPago.REGISTRADO,
                resultado
        );
        verify(pagoRepository).save(pagoCaptor.capture());
        assertSame(usuario, pagoCaptor.getValue().getUsuario());
        assertEquals("cs_test_123",
                pagoCaptor.getValue().getStripeSessionId());
        assertEquals(2000L, pagoCaptor.getValue().getMontoCentavos());
        assertEquals("USD", pagoCaptor.getValue().getMoneda());
    }

    private void configurarCheckoutSinReferencia() {
        when(session.getId()).thenReturn("cs_test_123");
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getAmountTotal()).thenReturn(2000L);
        when(session.getCurrency()).thenReturn("usd");
    }

    private void configurarCheckoutParaReferencia(
            String clientReferenceId) {

        configurarCheckoutSinReferencia();
        when(session.getClientReferenceId()).thenReturn(clientReferenceId);
    }
}
