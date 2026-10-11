package com.devsenior.Campusflow.cursos.mapper;

import com.devsenior.Campusflow.cursos.dto.CursoResponse;
import com.devsenior.Campusflow.cursos.model.Curso;
import com.devsenior.Campusflow.usuarios.model.Usuario;

public class CursoMapper {

    public static CursoResponse toResponse(Curso curso) {
        CursoResponse response = new CursoResponse();

        response.setId(curso.getId());
        response.setTitulo(curso.getTitulo());
        response.setDescripcion(curso.getDescripcion());

        Usuario instructor = curso.getInstructor();

        if (instructor != null) {
            response.setInstructorId(instructor.getId());
            response.setInstructorNombre(instructor.getNombre());
        }

        return response;
    }
}
