
package com.devsenior.Campusflow.security;

import com.devsenior.Campusflow.usuarios.dto.CrearUsuarioRequest;
import com.devsenior.Campusflow.usuarios.dto.UsuarioResponse;
import com.devsenior.Campusflow.usuarios.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public AuthController(
            AuthService authService,
            UsuarioService usuarioService) {

        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse respuesta = authService.login(request);

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(
            @Valid @RequestBody CrearUsuarioRequest request) {

        UsuarioResponse usuario =
                usuarioService.registrarUsuario(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuario);
    }
}
