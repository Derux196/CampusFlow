package com.devsenior.Campusflow.cursos.service;

import java.util.List;
import java.util.Optional;

import com.devsenior.Campusflow.common.exception.InscripcionDuplicadaException;
import com.devsenior.Campusflow.common.exception.ResourceNotFoundException;
import com.devsenior.Campusflow.cursos.dto.CrearCursoRequest;
import com.devsenior.Campusflow.cursos.dto.CursoResponse;
import com.devsenior.Campusflow.cursos.model.Curso;
import com.devsenior.Campusflow.cursos.repository.CursoRepository;
import com.devsenior.Campusflow.usuarios.model.RolUsuario;
import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CursoService cursoService;

    @Test
    void debeListarCursosComoRespuestas() {
        Curso curso = curso(1L, "Introduccion a Java");

        when(cursoRepository.findAllWithInstructor())
                .thenReturn(List.of(curso));

        List<CursoResponse> resultado =
                cursoService.listarCursos();

        assertEquals(1, resultado.size());
        assertEquals(
                "Introduccion a Java",
                resultado.get(0).getTitulo()
        );
        verify(cursoRepository).findAllWithInstructor();
    }

    @Test
    void debeCrearCurso() {
        CrearCursoRequest request = new CrearCursoRequest();
        request.setTitulo("Spring Boot");
        request.setDescripcion("Curso de Spring Boot");

        when(cursoRepository.save(any(Curso.class)))
                .thenAnswer(invocacion -> {
                    Curso curso = invocacion.getArgument(0);
                    curso.setId(1L);
                    return curso;
                });

        CursoResponse resultado = cursoService.crearCurso(request);

        assertEquals(1L, resultado.getId());
        assertEquals("Spring Boot", resultado.getTitulo());
        assertEquals(
                "Curso de Spring Boot",
                resultado.getDescripcion()
        );
    }

    @Test
    void debeObtenerCursoPorId() {
        Curso curso = curso(1L, "Spring Boot");

        when(cursoRepository.findByIdWithInstructor(1L))
                .thenReturn(Optional.of(curso));

        CursoResponse resultado =
                cursoService.obtenerCursoPorId(1L);

        assertEquals("Spring Boot", resultado.getTitulo());
    }

    @Test
    void debeRechazarCursoInexistente() {
        when(cursoRepository.findByIdWithInstructor(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cursoService.obtenerCursoPorId(99L)
        );
    }

    @Test
    void debeAsignarInstructorValido() {
        Curso curso = curso(1L, "Spring Boot");
        Usuario instructor = usuario(
                2L,
                "instructor@campusflow.com",
                RolUsuario.INSTRUCTOR
        );

        when(cursoRepository.findByIdWithInstructor(1L))
                .thenReturn(Optional.of(curso));
        when(usuarioRepository.findById(2L))
                .thenReturn(Optional.of(instructor));

        CursoResponse resultado =
                cursoService.asignarInstructor(1L, 2L);

        assertEquals(2L, resultado.getInstructorId());
        assertEquals("Usuario Prueba", resultado.getInstructorNombre());
    }

    @Test
    void debeRechazarAsignacionDeUsuarioNoInstructor() {
        Curso curso = curso(1L, "Spring Boot");
        Usuario estudiante = usuario(
                2L,
                "estudiante@campusflow.com",
                RolUsuario.ESTUDIANTE
        );

        when(cursoRepository.findByIdWithInstructor(1L))
                .thenReturn(Optional.of(curso));
        when(usuarioRepository.findById(2L))
                .thenReturn(Optional.of(estudiante));

        assertThrows(
                IllegalArgumentException.class,
                () -> cursoService.asignarInstructor(1L, 2L)
        );
    }

    @Test
    void debeInscribirEstudianteAutenticado() {
        Curso curso = curso(1L, "Spring Boot");
        Usuario estudiante = usuario(
                2L,
                "estudiante@campusflow.com",
                RolUsuario.ESTUDIANTE
        );

        when(cursoRepository.findByIdWithInstructor(1L))
                .thenReturn(Optional.of(curso));
        when(usuarioRepository.findByEmail(
                "estudiante@campusflow.com"))
                .thenReturn(Optional.of(estudiante));
        when(cursoRepository.existsByIdAndEstudiantesId(1L, 2L))
                .thenReturn(false);

        cursoService.inscribirEstudiante(
                1L,
                "estudiante@campusflow.com"
        );

        assertEquals(1, curso.getEstudiantes().size());
        assertEquals(estudiante, curso.getEstudiantes().get(0));
    }

    @Test
    void debeRechazarInscripcionDuplicada() {
        Curso curso = curso(1L, "Spring Boot");
        Usuario estudiante = usuario(
                2L,
                "estudiante@campusflow.com",
                RolUsuario.ESTUDIANTE
        );

        when(cursoRepository.findByIdWithInstructor(1L))
                .thenReturn(Optional.of(curso));
        when(usuarioRepository.findByEmail(
                "estudiante@campusflow.com"))
                .thenReturn(Optional.of(estudiante));
        when(cursoRepository.existsByIdAndEstudiantesId(1L, 2L))
                .thenReturn(true);

        assertThrows(
                InscripcionDuplicadaException.class,
                () -> cursoService.inscribirEstudiante(
                        1L,
                        "estudiante@campusflow.com"
                )
        );

        assertTrue(curso.getEstudiantes().isEmpty());
    }

    @Test
    void debeRechazarInscripcionDeUsuarioSinRolEstudiante() {
        Curso curso = curso(1L, "Spring Boot");
        Usuario instructor = usuario(
                2L,
                "instructor@campusflow.com",
                RolUsuario.INSTRUCTOR
        );

        when(cursoRepository.findByIdWithInstructor(1L))
                .thenReturn(Optional.of(curso));
        when(usuarioRepository.findByEmail(
                "instructor@campusflow.com"))
                .thenReturn(Optional.of(instructor));

        assertThrows(
                IllegalArgumentException.class,
                () -> cursoService.inscribirEstudiante(
                        1L,
                        "instructor@campusflow.com"
                )
        );

        verify(cursoRepository, never())
                .existsByIdAndEstudiantesId(any(), any());
    }

    @Test
    void debeConsultarCursosPorInstructor() {
        Usuario instructor = usuario(
                2L,
                "instructor@campusflow.com",
                RolUsuario.INSTRUCTOR
        );

        when(usuarioRepository.findById(2L))
                .thenReturn(Optional.of(instructor));
        when(cursoRepository.findByInstructorIdWithInstructor(2L))
                .thenReturn(List.of(curso(1L, "Spring Boot")));

        List<CursoResponse> resultado =
                cursoService.obtenerCursosPorInstructor(2L);

        assertEquals(1, resultado.size());
    }

    @Test
    void debeConsultarCursosPorEstudiante() {
        Usuario estudiante = usuario(
                2L,
                "estudiante@campusflow.com",
                RolUsuario.ESTUDIANTE
        );

        when(usuarioRepository.findById(2L))
                .thenReturn(Optional.of(estudiante));
        when(cursoRepository.findByEstudianteIdWithInstructor(2L))
                .thenReturn(List.of(curso(1L, "Spring Boot")));

        List<CursoResponse> resultado =
                cursoService.obtenerCursosPorEstudiante(2L);

        assertEquals(1, resultado.size());
    }

    private Curso curso(Long id, String titulo) {
        Curso curso = new Curso(titulo, "Descripcion");
        curso.setId(id);
        return curso;
    }

    private Usuario usuario(
            Long id,
            String email,
            RolUsuario rol) {

        Usuario usuario = new Usuario("Usuario Prueba", email);
        usuario.setId(id);
        usuario.setRol(rol);
        return usuario;
    }
}
