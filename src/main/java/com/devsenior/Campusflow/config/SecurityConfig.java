
package com.devsenior.Campusflow.config;

import com.devsenior.Campusflow.security.JwtAuthenticationFilter;

import jakarta.servlet.DispatcherType;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter) throws Exception {

        http
            // API REST: autenticacion mediante JWT
            .csrf(csrf -> csrf.disable())

            // No guardar autenticacion en sesiones HTTP
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // Solicitudes sin autenticacion: 401
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(
                    new HttpStatusEntryPoint(
                        HttpStatus.UNAUTHORIZED
                    )
                )
            )

            .authorizeHttpRequests(auth -> auth

                // Paginas publicas de resultado de Checkout
                .requestMatchers(
                   HttpMethod.GET,
                   "/pago-exitoso.html",
                   "/pago-cancelado.html"
                ).permitAll()

                // Permitir despachos internos de errores
                .dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()

                // Endpoints publicos de consulta
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/cursos",
                    "/api/cursos/*",
                    "/api/saludo"
                ).permitAll()

                // Checkout: requiere un usuario autenticado
                .requestMatchers(
                  HttpMethod.POST,
                  "/api/stripe/checkout/pago-unico"
                ).authenticated()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/cursos/instructores/*",
                    "/api/cursos/estudiantes/*"
                ).hasRole("ADMINISTRADOR")

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/cursos"
                ).hasRole("ADMINISTRADOR")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/cursos/*/instructor/*"
                ).hasRole("ADMINISTRADOR")

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/cursos/*/inscripcion"
                ).hasRole("ESTUDIANTE")

                // Login y registro publicos
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/login",
                    "/api/auth/registro"
                ).permitAll()

              // Webhook de Stripe: acceso sin JWT.
          // La firma se validara en StripeWebhookController.
          .requestMatchers(
                    HttpMethod.POST,
                   "/api/stripe/webhook"
               ).permitAll()

                // Rutas exclusivas de administradores
                .requestMatchers(
                    "/api/usuarios",
                    "/api/usuarios/**"
                ).hasRole("ADMINISTRADOR")

                // Denegar todas las demas rutas
                .anyRequest().denyAll()
            )

            // Validar JWT antes del filtro de login
            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    // Evitar registrar el filtro JWT dos veces
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter>
            jwtFilterRegistration(JwtAuthenticationFilter jwtFilter) {

        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtFilter);

        registration.setEnabled(false);

        return registration;
    }
}
