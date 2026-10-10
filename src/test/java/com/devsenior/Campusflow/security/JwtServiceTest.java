
package com.devsenior.Campusflow.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService(
                "claveDeLaboratorio1234567890ABCDEFGHIJKLMN"
            );

    @Test
    void debeGenerarYValidarToken() {

        String token = jwtService.generarToken(
                "estudiante@example.com",
                "ESTUDIANTE"
        );

        assertEquals(
                "estudiante@example.com",
                jwtService.obtenerEmail(token)
        );

        assertEquals(
                "ESTUDIANTE",
                jwtService.validarToken(token)
                        .get("rol", String.class)
        );
    }

    @Test
    void debeRechazarTokenManipulado() {

        String token = jwtService.generarToken(
                "estudiante@example.com",
                "ESTUDIANTE"
        );

        String[] partes = token.split("\\.");

        String tokenManipulado =
                partes[0] + "." +
                partes[1] + "." +
                (partes[2].startsWith("A") ? "B" : "A")
                + partes[2].substring(1);

        assertThrows(
                JwtException.class,
                () -> jwtService.validarToken(tokenManipulado)
        );
    }
}
