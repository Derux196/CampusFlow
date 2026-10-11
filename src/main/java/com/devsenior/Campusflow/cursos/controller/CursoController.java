
package com.devsenior.Campusflow.cursos.controller;

import java.util.List;

import com.devsenior.Campusflow.cursos.dto.CrearCursoRequest;
import com.devsenior.Campusflow.cursos.dto.CursoResponse;
import com.devsenior.Campusflow.cursos.service.CursoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cursos")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    public List<CursoResponse> listar() {
        return cursoService.listarCursos();
    }

    @PostMapping
    public ResponseEntity<CursoResponse> crear(
            @Valid @RequestBody CrearCursoRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cursoService.crearCurso(request));
    }

    @GetMapping("/{id}")
    public CursoResponse obtenerPorId(@PathVariable Long id) {
        return cursoService.obtenerCursoPorId(id);
    }

    @PutMapping("/{cursoId}/instructor/{instructorId}")
    public CursoResponse asignarInstructor(
            @PathVariable Long cursoId,
            @PathVariable Long instructorId) {

        return cursoService.asignarInstructor(
                cursoId,
                instructorId
        );
    }

    @PostMapping("/{cursoId}/inscripcion")
    public CursoResponse inscribir(
            @PathVariable Long cursoId,
            Authentication authentication) {

        return cursoService.inscribirEstudiante(
                cursoId,
                authentication.getName()
        );
    }

    @GetMapping("/instructores/{instructorId}")
    public List<CursoResponse> obtenerPorInstructor(
            @PathVariable Long instructorId) {

        return cursoService
                .obtenerCursosPorInstructor(instructorId);
    }

    @GetMapping("/estudiantes/{estudianteId}")
    public List<CursoResponse> obtenerPorEstudiante(
            @PathVariable Long estudianteId) {

        return cursoService
                .obtenerCursosPorEstudiante(estudianteId);
    }
}
