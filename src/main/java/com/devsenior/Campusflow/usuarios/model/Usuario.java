
package com.devsenior.Campusflow.usuarios.model;

import java.util.ArrayList;
import java.util.List;
import com.devsenior.Campusflow.cursos.model.Curso;


import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    
@Column(name = "password", length = 255)
private String password;


    // Rol del usuario
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", length = 30)
    private RolUsuario rol = RolUsuario.ESTUDIANTE;

    // Preferencias del usuario
    @Embedded
    private PreferenciasUsuario preferencias =
            new PreferenciasUsuario();

    
@OneToMany(mappedBy = "instructor")
private List<Curso> cursosDictados = new ArrayList<>();


@ManyToMany(mappedBy = "estudiantes")
private List<Curso> cursosInscritos = new ArrayList<>();

        

    // Constructor vacío requerido por JPA
    public Usuario() {
    }

    // Constructor existente
    public Usuario(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public PreferenciasUsuario getPreferencias() {
        return preferencias;
    }

    public void setPreferencias(
            PreferenciasUsuario preferencias) {
        this.preferencias = preferencias;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    

public List<Curso> getCursosDictados() {
    return cursosDictados;
}

public void setCursosDictados(List<Curso> cursosDictados) {
    this.cursosDictados = cursosDictados;
}


}
