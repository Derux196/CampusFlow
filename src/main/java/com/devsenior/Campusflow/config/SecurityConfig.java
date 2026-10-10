
package com.devsenior.Campusflow.config;

import com.devsenior.Campusflow.security.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/cursos",
                    "/api/saludo"
                ).permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/login"
                ).permitAll()
                .requestMatchers("/api/usuarios/**")
                    .hasRole("ADMINISTRADOR")
                .anyRequest().denyAll()
            )
            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

@Bean
public FilterRegistrationBean<JwtAuthenticationFilter>
        jwtFilterRegistration(JwtAuthenticationFilter jwtFilter) {

    FilterRegistrationBean<JwtAuthenticationFilter> registration =
            new FilterRegistrationBean<>(jwtFilter);

    registration.setEnabled(false);

    return registration;
}

}
