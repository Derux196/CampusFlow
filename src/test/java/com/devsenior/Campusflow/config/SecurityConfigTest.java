
package com.devsenior.Campusflow.config;

import com.devsenior.Campusflow.SaludoController;
import com.devsenior.Campusflow.security.JwtAuthenticationFilter;
import com.devsenior.Campusflow.security.JwtService;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import com.devsenior.Campusflow.usuarios.controller.UsuarioController;
import com.devsenior.Campusflow.usuarios.service.UsuarioService;
import com.devsenior.Campusflow.pagos.controller.StripeCheckoutController;
import com.devsenior.Campusflow.pagos.controller.CheckoutResultController;
import com.devsenior.Campusflow.pagos.service.StripeCheckoutService;
import com.devsenior.Campusflow.pagos.service.SuscripcionService;

import java.util.List;

import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.model.RolUsuario;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.JwtException;


@WebMvcTest({
    SaludoController.class,
    UsuarioController.class,
    StripeCheckoutController.class,
    CheckoutResultController.class
})
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
private UsuarioService usuarioService;

    @MockitoBean
    private StripeCheckoutService stripeCheckoutService;

    @MockitoBean
    private SuscripcionService suscripcionService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void debePermitirSaludoPublico() throws Exception {
        mockMvc.perform(get("/api/saludo"))
                .andExpect(status().isOk());
    }

    @Test
    void debeBloquearUsuariosSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void debeBloquearCheckoutSuscripcionSinAutenticacion()
            throws Exception {
        mockMvc.perform(post("/api/stripe/checkout/suscripcion"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void debeMostrarResultadosCheckoutSinJwt() throws Exception {
        mockMvc.perform(get("/pago-exitoso"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/pago-exitoso.html"));
        mockMvc.perform(get("/pago-exitoso.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Pago finalizado"
                        )
                ));
        mockMvc.perform(get("/pago-cancelado"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/pago-cancelado.html"));
        mockMvc.perform(get("/pago-cancelado.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Pago cancelado"
                        )
                ));
    }

    @Test
    void debeMantenerCheckoutPagoUnicoProtegidoSinJwt()
            throws Exception {
        mockMvc.perform(post("/api/stripe/checkout/pago-unico"))
                .andExpect(status().isUnauthorized());
    }


@Test
void debeBloquearEstudianteEnRutaAdministrativa()
        throws Exception {

    Usuario estudiante = new Usuario();
    estudiante.setEmail("estudiante@example.com");
    estudiante.setRol(RolUsuario.ESTUDIANTE);

    when(jwtService.obtenerEmail("tokenEstudiante"))
            .thenReturn("estudiante@example.com");

    when(usuarioRepository.findByEmail(
            "estudiante@example.com"))
            .thenReturn(Optional.of(estudiante));

    mockMvc.perform(
            get("/api/usuarios")
                .header(
                    "Authorization",
                    "Bearer tokenEstudiante"
                )
    ).andExpect(status().isForbidden());
}


@Test
void debePermitirAdministradorEnRutaUsuarios()
        throws Exception {

    Usuario administrador = new Usuario();
    administrador.setEmail("admin@example.com");
    administrador.setRol(RolUsuario.ADMINISTRADOR);

    when(jwtService.obtenerEmail("tokenAdministrador"))
            .thenReturn("admin@example.com");

    when(usuarioRepository.findByEmail("admin@example.com"))
            .thenReturn(Optional.of(administrador));

    when(usuarioService.listarUsuarios())
            .thenReturn(List.of());

    mockMvc.perform(
            get("/api/usuarios")
                .header(
                    "Authorization",
                    "Bearer tokenAdministrador"
                )
    ).andExpect(status().isOk());
}


@Test
void debeRechazarTokenJwtInvalido() throws Exception {

    String tokenInvalido = "token.manipulado.invalido";

    when(jwtService.obtenerEmail(tokenInvalido))
            .thenThrow(new JwtException("Firma JWT invalida"));

    mockMvc.perform(
            get("/api/usuarios")
                .header(
                    "Authorization",
                    "Bearer " + tokenInvalido
                )
    ).andExpect(status().isUnauthorized());
}



}
