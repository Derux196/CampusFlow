
package com.devsenior.Campusflow.cursos.service;

import java.util.List;

import com.devsenior.Campusflow.common.exception.InscripcionDuplicadaException;
import com.devsenior.Campusflow.common.exception.ResourceNotFoundException;
import com.devsenior.Campusflow.cursos.dto.CrearCursoRequest;
import com.devsenior.Campusflow.cursos.dto.CursoResponse;
import com.devsenior.Campusflow.cursos.mapper.CursoMapper;
import com.devsenior.Campusflow.cursos.model.Curso;
import com.devsenior.Campusflow.cursos.repository.CursoRepository;
import com.devsenior.Campusflow.usuarios.model.RolUsuario;
import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;

    public CursoService(
            CursoRepository cursoRepository,
            UsuarioRepository usuarioRepository) {

        this.cursoRepository = cursoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<CursoResponse> listarCursos() {
        return cursoRepository.findAllWithInstructor()
                .stream()
                .map(CursoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CursoResponse obtenerCursoPorId(Long id) {
        return CursoMapper.toResponse(
                buscarCursoPorId(id)
        );
    }

    @Transactional
    public CursoResponse crearCurso(CrearCursoRequest request) {
        Curso curso = new Curso(
                request.getTitulo(),
                request.getDescripcion()
        );

        return CursoMapper.toResponse(
                cursoRepository.save(curso)
        );
    }

    @Transactional
    public CursoResponse asignarInstructor(
            Long cursoId,
            Long instructorId) {

        Curso curso = buscarCursoPorId(cursoId);
        Usuario instructor = buscarUsuarioPorId(instructorId);

        if (instructor.getRol() != RolUsuario.INSTRUCTOR) {
            throw new IllegalArgumentException(
                    "El usuario asignado debe tener rol INSTRUCTOR"
            );
        }

        curso.setInstructor(instructor);

        return CursoMapper.toResponse(curso);
    }

    @Transactional
    public CursoResponse inscribirEstudiante(
            Long cursoId,
            String emailEstudiante) {

        Curso curso = buscarCursoPorId(cursoId);
        Usuario estudiante = usuarioRepository
                .findByEmail(emailEstudiante)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario no encontrado"
                        )
                );

        if (estudiante.getRol() != RolUsuario.ESTUDIANTE) {
            throw new IllegalArgumentException(
                    "Solo los estudiantes pueden inscribirse"
            );
        }

        if (cursoRepository.existsByIdAndEstudiantesId(
                cursoId,
                estudiante.getId())) {
            throw new InscripcionDuplicadaException();
        }

        curso.getEstudiantes().add(estudiante);

        return CursoMapper.toResponse(curso);
    }

    @Transactional(readOnly = true)
    public List<CursoResponse> obtenerCursosPorInstructor(
            Long instructorId) {

        validarRolUsuario(instructorId, RolUsuario.INSTRUCTOR);

        return cursoRepository
                .findByInstructorIdWithInstructor(instructorId)
                .stream()
                .map(CursoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CursoResponse> obtenerCursosPorEstudiante(
            Long estudianteId) {

        validarRolUsuario(estudianteId, RolUsuario.ESTUDIANTE);

        return cursoRepository
                .findByEstudianteIdWithInstructor(estudianteId)
                .stream()
                .map(CursoMapper::toResponse)
                .toList();
    }

    private Curso buscarCursoPorId(Long id) {
        return cursoRepository.findByIdWithInstructor(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Curso no encontrado con ID: " + id
                        )
                );
    }

    private Usuario buscarUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario no encontrado con ID: " + id
                        )
                );
    }

    private void validarRolUsuario(
            Long usuarioId,
            RolUsuario rolEsperado) {

        Usuario usuario = buscarUsuarioPorId(usuarioId);

        if (usuario.getRol() != rolEsperado) {
            throw new IllegalArgumentException(
                    "El usuario no tiene el rol requerido"
            );
        }
    }
}
