
package com.devsenior.Campusflow.cursos.service;

import com.devsenior.Campusflow.cursos.model.Curso;
import com.devsenior.Campusflow.cursos.repository.CursoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock
    private CursoRepository cursoRepository;

    @InjectMocks
    private CursoService cursoService;

    @Test
    void debeListarCursos() {

        // Preparar un curso de prueba
        Curso curso = new Curso(
            "Introducción a Java",
            "Curso básico de programación en Java"
        );

        // Simular la respuesta del repositorio
        when(cursoRepository.findAll())
            .thenReturn(List.of(curso));

        // Ejecutar el servicio
        List<Curso> resultado =
            cursoService.listarCursos();

        // Comprobar el resultado
        assertEquals(1, resultado.size());

        assertEquals(
            "Introducción a Java",
            resultado.get(0).getTitulo()
        );

        // Verificar que se consultó el repositorio
        verify(cursoRepository).findAll();
    }
}
