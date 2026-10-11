package com.devsenior.Campusflow.cursos.repository;

import java.util.List;

import com.devsenior.Campusflow.cursos.model.Curso;
import com.devsenior.Campusflow.usuarios.model.RolUsuario;
import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class CursoRepositoryIntegrationTest {

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void debeRecuperarCursoConInstructorMedianteJoinFetch() {
        Usuario instructor = guardarUsuario(
                "Instructor",
                "instructor@campusflow.test",
                RolUsuario.INSTRUCTOR
        );
        Curso curso = new Curso("Spring Boot", "Curso de Spring Boot");
        curso.setInstructor(instructor);
        curso = cursoRepository.saveAndFlush(curso);
        entityManager.clear();

        Curso resultado = cursoRepository
                .findByIdWithInstructor(curso.getId())
                .orElseThrow();

        assertEquals(instructor.getId(), resultado.getInstructor().getId());
        assertTrue(Hibernate.isInitialized(resultado.getInstructor()));
    }

    @Test
    void debeConsultarCursosPorEstudianteConInstructorCargado() {
        Usuario instructor = guardarUsuario(
                "Instructor",
                "instructor@campusflow.test",
                RolUsuario.INSTRUCTOR
        );
        Usuario estudiante = guardarUsuario(
                "Estudiante",
                "estudiante@campusflow.test",
                RolUsuario.ESTUDIANTE
        );
        Curso cursoInscrito = guardarCurso(
                "Spring Boot",
                instructor,
                estudiante
        );
        guardarCurso("Java", instructor);
        entityManager.clear();

        List<Curso> resultado = cursoRepository
                .findByEstudianteIdWithInstructor(estudiante.getId());

        assertEquals(List.of(cursoInscrito.getId()),
                resultado.stream().map(Curso::getId).toList());
        assertTrue(Hibernate.isInitialized(
                resultado.get(0).getInstructor()
        ));
    }

    @Test
    void debeDetectarInscripcionExistente() {
        Usuario estudiante = guardarUsuario(
                "Estudiante",
                "estudiante@campusflow.test",
                RolUsuario.ESTUDIANTE
        );
        Curso curso = guardarCurso(
                "Spring Boot",
                null,
                estudiante
        );
        entityManager.clear();

        assertTrue(cursoRepository.existsByIdAndEstudiantesId(
                curso.getId(),
                estudiante.getId()
        ));
        assertFalse(cursoRepository.existsByIdAndEstudiantesId(
                curso.getId(),
                estudiante.getId() + 1
        ));
    }

    private Usuario guardarUsuario(
            String nombre,
            String email,
            RolUsuario rol) {

        Usuario usuario = new Usuario(nombre, email);
        usuario.setRol(rol);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Curso guardarCurso(
            String titulo,
            Usuario instructor,
            Usuario... estudiantes) {

        Curso curso = new Curso(titulo, "Descripcion");
        curso.setInstructor(instructor);
        curso.getEstudiantes().addAll(List.of(estudiantes));
        return cursoRepository.saveAndFlush(curso);
    }
}
