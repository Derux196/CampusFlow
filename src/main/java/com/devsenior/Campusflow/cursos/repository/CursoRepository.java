
package com.devsenior.Campusflow.cursos.repository;

import com.devsenior.Campusflow.cursos.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CursoRepository
        extends JpaRepository<Curso, Long> {
}
