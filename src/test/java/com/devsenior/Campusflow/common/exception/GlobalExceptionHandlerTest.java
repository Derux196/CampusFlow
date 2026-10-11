
package com.devsenior.Campusflow.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void debeResponder404CuandoRecursoNoExiste() {

        ResourceNotFoundException excepcion =
                new ResourceNotFoundException(
                        "Usuario no encontrado"
                );

        var respuesta =
                handler.manejarNoEncontrado(excepcion);

        assertEquals(
                HttpStatus.NOT_FOUND,
                respuesta.getStatusCode()
        );

        assertNotNull(respuesta.getBody());

        assertEquals(
                "Usuario no encontrado",
                respuesta.getBody().get("error")
        );
    }

    @Test
    void debeResponder409CuandoUsuarioTieneRelacionesAcademicas() {

        var respuesta = handler.manejarUsuarioConRelaciones(
                new UsuarioConRelacionesException()
        );

        assertEquals(
                HttpStatus.CONFLICT,
                respuesta.getStatusCode()
        );
        assertNotNull(respuesta.getBody());
        assertEquals(
                "No se puede eliminar un usuario con relaciones academicas",
                respuesta.getBody().get("error")
        );
    }

    @Test
    void debeResponder409CuandoLaInscripcionEstaDuplicada() {

        var respuesta = handler.manejarInscripcionDuplicada(
                new InscripcionDuplicadaException()
        );

        assertEquals(
                HttpStatus.CONFLICT,
                respuesta.getStatusCode()
        );
        assertNotNull(respuesta.getBody());
        assertEquals(
                "El estudiante ya esta inscrito en este curso",
                respuesta.getBody().get("error")
        );
    }
}
