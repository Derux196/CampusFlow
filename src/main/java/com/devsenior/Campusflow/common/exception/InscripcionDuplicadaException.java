package com.devsenior.Campusflow.common.exception;

public class InscripcionDuplicadaException
        extends RuntimeException {

    public InscripcionDuplicadaException() {
        super("El estudiante ya esta inscrito en este curso");
    }
}
