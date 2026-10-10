
package com.devsenior.Campusflow.security;

import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {

        Usuario usuario = usuarioRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Credenciales incorrectas"
                        )
                );

        if (usuario.getPassword() == null ||
                !passwordEncoder.matches(
                        request.getPassword(),
                        usuario.getPassword())) {

            throw new BadCredentialsException(
                    "Credenciales incorrectas"
            );
        }

        if (usuario.getRol() == null) {
            throw new BadCredentialsException(
                    "Credenciales incorrectas"
            );
        }

        String token = jwtService.generarToken(
                usuario.getEmail(),
                usuario.getRol().name()
        );

        return new LoginResponse(token);
    }
}
