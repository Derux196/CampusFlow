package com.devsenior.Campusflow.common.exception;

public class UsuarioConRelacionesException
        extends RuntimeException {

    public UsuarioConRelacionesException() {
        super("No se puede eliminar un usuario con relaciones academicas");
    }
}
