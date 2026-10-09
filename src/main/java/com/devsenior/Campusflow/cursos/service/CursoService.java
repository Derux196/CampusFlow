
package com.devsenior.Campusflow.cursos.service;

import java.util.List;

import com.devsenior.Campusflow.cursos.model.Curso;
import com.devsenior.Campusflow.cursos.repository.CursoRepository;

import org.springframework.stereotype.Service;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;

    public CursoService(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    public List<Curso> listarCursos() {
        return cursoRepository.findAll();
    }
}
