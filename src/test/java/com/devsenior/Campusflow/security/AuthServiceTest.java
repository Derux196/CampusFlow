
package com.devsenior.Campusflow.security;

import com.devsenior.Campusflow.usuarios.model.RolUsuario;
import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void debeIniciarSesionConCredencialesCorrectas() {

        LoginRequest request = new LoginRequest();
        request.setEmail("estudiante@example.com");
        request.setPassword("ClaveSegura123!");

        Usuario usuario = new Usuario();
        usuario.setEmail("estudiante@example.com");
        usuario.setPassword("hashBCryptSimulado");
        usuario.setRol(RolUsuario.ESTUDIANTE);

        when(usuarioRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(usuario));

        when(passwordEncoder.matches(
                "ClaveSegura123!",
                "hashBCryptSimulado"
        )).thenReturn(true);

        when(jwtService.generarToken(
                "estudiante@example.com",
                "ESTUDIANTE"
        )).thenReturn("tokenJWTsimulado");

        LoginResponse respuesta = authService.login(request);

        assertEquals("tokenJWTsimulado", respuesta.getToken());
        assertEquals("Bearer", respuesta.getTipo());

        verify(jwtService).generarToken(
                "estudiante@example.com",
                "ESTUDIANTE"
        );
    }

    @Test
    void debeRechazarPasswordIncorrecto() {

        LoginRequest request = new LoginRequest();
        request.setEmail("estudiante@example.com");
        request.setPassword("ClaveIncorrecta");

        Usuario usuario = new Usuario();
        usuario.setEmail("estudiante@example.com");
        usuario.setPassword("hashBCryptSimulado");
        usuario.setRol(RolUsuario.ESTUDIANTE);

        when(usuarioRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(usuario));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        verifyNoInteractions(jwtService);
    }

    @Test
    void debeRechazarUsuarioInexistente() {

        LoginRequest request = new LoginRequest();
        request.setEmail("noexiste@example.com");
        request.setPassword("ClaveSegura123!");

        when(usuarioRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        verifyNoInteractions(jwtService);
    }
}
