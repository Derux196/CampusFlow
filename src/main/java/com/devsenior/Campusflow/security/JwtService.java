
package com.devsenior.Campusflow.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey clave;
    private static final long EXPIRACION_MS = 3600000;

    public JwtService(@Value("${app.jwt.secret}") String secreto) {
        this.clave = Keys.hmacShaKeyFor(
                secreto.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generarToken(String email, String rol) {
        Date ahora = new Date();

        return Jwts.builder()
                .subject(email)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(new Date(
                        ahora.getTime() + EXPIRACION_MS
                ))
                .signWith(clave)
                .compact();
    }

    public Claims validarToken(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String obtenerEmail(String token) {
        return validarToken(token).getSubject();
    }
}
