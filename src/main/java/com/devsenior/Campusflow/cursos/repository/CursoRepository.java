
package com.devsenior.Campusflow.cursos.repository;

import java.util.List;
import java.util.Optional;

import com.devsenior.Campusflow.cursos.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CursoRepository
        extends JpaRepository<Curso, Long> {

    @Query("""
            select c from Curso c
            left join fetch c.instructor
            """)
    List<Curso> findAllWithInstructor();

    @Query("""
            select c from Curso c
            left join fetch c.instructor
            where c.id = :id
            """)
    Optional<Curso> findByIdWithInstructor(@Param("id") Long id);

    @Query("""
            select c from Curso c
            left join fetch c.instructor
            where c.instructor.id = :instructorId
            """)
    List<Curso> findByInstructorIdWithInstructor(
            @Param("instructorId") Long instructorId);

    @Query("""
            select distinct c from Curso c
            join c.estudiantes e
            left join fetch c.instructor
            where e.id = :estudianteId
            """)
    List<Curso> findByEstudianteIdWithInstructor(
            @Param("estudianteId") Long estudianteId);

    boolean existsByIdAndEstudiantesId(
            Long cursoId,
            Long estudianteId);
}
