
package com.devsenior.Campusflow.security;

import com.devsenior.Campusflow.config.SecurityConfig;
import com.devsenior.Campusflow.usuarios.dto.CrearUsuarioRequest;
import com.devsenior.Campusflow.usuarios.dto.UsuarioResponse;
import com.devsenior.Campusflow.usuarios.model.RolUsuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;

import com.devsenior.Campusflow.common.exception.EmailDuplicadoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.BadCredentialsException;
import com.devsenior.Campusflow.common.exception.GlobalExceptionHandler;
import com.devsenior.Campusflow.usuarios.service.UsuarioService;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)

@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    GlobalExceptionHandler.class
})

class AuthControllerTest {

    @MockitoBean
private UsuarioService usuarioService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void debePermitirLoginConCredencialesValidas()
            throws Exception {

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("tokenSimulado"));

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "estudiante@example.com",
                              "password": "ClaveSegura123!"
                            }
                            """)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("tokenSimulado"))
        .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    @Test
    void debeRechazarLoginConDatosInvalidos()
            throws Exception {

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "correo-invalido",
                              "password": ""
                            }
                            """)
        )
        .andExpect(status().isBadRequest());
    }
    @Test
void debeRechazarLoginConCredencialesIncorrectas()
        throws Exception {

    when(authService.login(any(LoginRequest.class)))
            .thenThrow(
                new BadCredentialsException("Credenciales incorrectas")
            );

    mockMvc.perform(
            post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "email": "estudiante@example.com",
                          "password": "ClaveIncorrecta123"
                        }
                        """)
    )
    .andExpect(status().isUnauthorized());

}


@Test
void debeRegistrarEstudianteCorrectamente()
        throws Exception {

    UsuarioResponse respuesta = new UsuarioResponse();

    respuesta.setId(2L);
    respuesta.setNombre("Estudiante Nuevo");
    respuesta.setEmail("nuevo@campusflow.com");
    respuesta.setRol(RolUsuario.ESTUDIANTE);

    when(usuarioService.registrarUsuario(
            any(CrearUsuarioRequest.class)))
            .thenReturn(respuesta);

    mockMvc.perform(
            post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nombre": "Estudiante Nuevo",
                      "email": "nuevo@campusflow.com",
                      "password": "ClaveSegura123!",
                      "rol": "ESTUDIANTE"
                    }
                    """)
    )
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.nombre").value("Estudiante Nuevo"))
    .andExpect(jsonPath("$.rol").value("ESTUDIANTE"));
}

@Test
void debeRechazarRegistroComoAdministrador()
        throws Exception {

    when(usuarioService.registrarUsuario(
            any(CrearUsuarioRequest.class)))
            .thenThrow(new IllegalArgumentException(
                    "Solo se permite el registro como estudiante o instructor"
            ));

    mockMvc.perform(
            post("/api/auth/registro")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "nombre": "Administrador Prueba",
                          "email": "adminprueba@campusflow.com",
                          "password": "ClaveSegura123!",
                          "rol": "ADMINISTRADOR"
                        }
                        """)
    )
    .andExpect(status().isBadRequest());
}
@Test
void debeRechazarRegistroConEmailDuplicado()
        throws Exception {

    when(usuarioService.registrarUsuario(
            any(CrearUsuarioRequest.class)))
            .thenThrow(new EmailDuplicadoException(
                    "El correo electrónico ya está registrado"
            ));

    mockMvc.perform(
            post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nombre": "Estudiante Repetido",
                      "email": "prueba@campusflow.com",
                      "password": "ClaveSegura123!",
                      "rol": "ESTUDIANTE"
                    }
                    """)
    )
    .andExpect(status().isConflict())
    .andExpect(jsonPath("$.error")
            .value("El correo electrónico ya está registrado"));
}


}
