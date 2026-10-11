
package com.devsenior.Campusflow.common.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler
        extends ResponseEntityExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> manejarNoEncontrado(
            ResourceNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }


@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<Map<String, String>> manejarCredencialesIncorrectas(
        BadCredentialsException ex) {

    return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                    "error", "Credenciales incorrectas"
            ));
}

@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(
        IllegalArgumentException ex) {

    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                    "error", "Solicitud inválida"
            ));
}


@ExceptionHandler(EmailDuplicadoException.class)
public ResponseEntity<Map<String, String>> manejarEmailDuplicado(
        EmailDuplicadoException ex) {

    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of(
                    "error", "El correo electrónico ya está registrado"
            ));
}

@ExceptionHandler(UsuarioConRelacionesException.class)
public ResponseEntity<Map<String, String>> manejarUsuarioConRelaciones(
        UsuarioConRelacionesException ex) {

    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of(
                    "error",
                    "No se puede eliminar un usuario con relaciones academicas"
            ));
}


@ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> manejarGeneral(
            Exception ex) {

        log.error("Error no controlado al procesar la petición", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error",
                    "Ocurrió un error interno. Intenta de nuevo."
                ));
    }
}
