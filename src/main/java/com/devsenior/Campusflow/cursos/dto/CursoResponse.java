package com.devsenior.Campusflow.cursos.dto;

public class CursoResponse {

    private Long id;
    private String titulo;
    private String descripcion;
    private Long instructorId;
    private String instructorNombre;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Long instructorId) {
        this.instructorId = instructorId;
    }

    public String getInstructorNombre() {
        return instructorNombre;
    }

    public void setInstructorNombre(String instructorNombre) {
        this.instructorNombre = instructorNombre;
    }
}
