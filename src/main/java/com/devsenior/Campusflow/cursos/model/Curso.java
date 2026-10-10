
package com.devsenior.Campusflow.cursos.model;

import jakarta.persistence.*;

import com.devsenior.Campusflow.usuarios.model.Usuario;

import java.util.ArrayList;
import java.util.List;



@Entity
@Table(name = "cursos")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "instructor_id")
private Usuario instructor;


@ManyToMany
@JoinTable(
    name = "curso_estudiante",
    joinColumns = @JoinColumn(name = "curso_id"),
    inverseJoinColumns = @JoinColumn(name = "estudiante_id")
)
private List<Usuario> estudiantes = new ArrayList<>();


    public Curso() {
    }

    public Curso(String titulo, String descripcion) {
        this.titulo = titulo;
        this.descripcion = descripcion;
    }

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

    public Usuario getInstructor() {
        return instructor;
    }

    public void setInstructor(Usuario instructor) {
        this.instructor = instructor;
    }


public List<Usuario> getEstudiantes() {
    return estudiantes;
}

public void setEstudiantes(List<Usuario> estudiantes) {
    this.estudiantes = estudiantes;
}

}
