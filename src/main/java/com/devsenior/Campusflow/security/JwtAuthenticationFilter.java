
package com.devsenior.Campusflow.security;

import java.io.IOException;
import java.util.List;

import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UsuarioRepository usuarioRepository) {

        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization != null
                && authorization.startsWith("Bearer ")) {

            String token = authorization.substring(7);

            try {
                String email = jwtService.obtenerEmail(token);

                Usuario usuario = usuarioRepository
                        .findByEmail(email)
                        .orElse(null);

                if (usuario == null) {

                } else if (usuario.getRol() == null) {

                } else if (SecurityContextHolder.getContext()
                        .getAuthentication() == null) {

                    String rol = usuario.getRol().name();

                    var authorities = List.of(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + rol
                            )
                    );

                    var authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    authorities
                            );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);

                } else {
                }

            } catch (JwtException | IllegalArgumentException ex) {

                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
