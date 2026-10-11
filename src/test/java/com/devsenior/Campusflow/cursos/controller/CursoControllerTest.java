package com.devsenior.Campusflow.cursos.controller;

import java.util.List;

import com.devsenior.Campusflow.common.exception.GlobalExceptionHandler;
import com.devsenior.Campusflow.common.exception.InscripcionDuplicadaException;
import com.devsenior.Campusflow.common.exception.ResourceNotFoundException;
import com.devsenior.Campusflow.config.SecurityConfig;
import com.devsenior.Campusflow.cursos.dto.CursoResponse;
import com.devsenior.Campusflow.cursos.service.CursoService;
import com.devsenior.Campusflow.security.JwtAuthenticationFilter;
import com.devsenior.Campusflow.security.JwtService;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CursoController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    GlobalExceptionHandler.class
})
class CursoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CursoService cursoService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void debeListarCursosPublicamente() throws Exception {
        when(cursoService.listarCursos())
                .thenReturn(List.of(cursoResponse()));

        mockMvc.perform(get("/api/cursos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo")
                        .value("Spring Boot"));
    }

    @Test
    void debeConsultarCursoPublicamente() throws Exception {
        when(cursoService.obtenerCursoPorId(1L))
                .thenReturn(cursoResponse());

        mockMvc.perform(get("/api/cursos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void debeResponder404CuandoCursoNoExiste() throws Exception {
        when(cursoService.obtenerCursoPorId(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Curso no encontrado con ID: 99"
                ));

        mockMvc.perform(get("/api/cursos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void debeRechazarCreacionSinAutenticacion() throws Exception {
        mockMvc.perform(
                post("/api/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "titulo": "Spring Boot"
                            }
                            """)
        ).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void debeRechazarCreacionComoEstudiante() throws Exception {
        mockMvc.perform(
                post("/api/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "titulo": "Spring Boot"
                            }
                            """)
        ).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeCrearCursoComoAdministrador() throws Exception {
        when(cursoService.crearCurso(any()))
                .thenReturn(cursoResponse());

        mockMvc.perform(
                post("/api/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "titulo": "Spring Boot",
                              "descripcion": "Curso de Spring Boot"
                            }
                            """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.titulo").value("Spring Boot"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeRechazarCursoSinTitulo() throws Exception {
        mockMvc.perform(
                post("/api/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "titulo": ""
                            }
                            """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeAsignarInstructorComoAdministrador() throws Exception {
        when(cursoService.asignarInstructor(1L, 2L))
                .thenReturn(cursoResponse());

        mockMvc.perform(put("/api/cursos/1/instructor/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructorId").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeRechazarInstructorConRolInvalido() throws Exception {
        when(cursoService.asignarInstructor(1L, 2L))
                .thenThrow(new IllegalArgumentException(
                        "El usuario asignado debe tener rol INSTRUCTOR"
                ));

        mockMvc.perform(put("/api/cursos/1/instructor/2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeResponder404AlAsignarInstructorACursoInexistente()
            throws Exception {

        when(cursoService.asignarInstructor(99L, 2L))
                .thenThrow(new ResourceNotFoundException(
                        "Curso no encontrado con ID: 99"
                ));

        mockMvc.perform(put("/api/cursos/99/instructor/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void debeRechazarInscripcionSinAutenticacion() throws Exception {
        mockMvc.perform(post("/api/cursos/1/inscripcion"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(
            username = "estudiante@campusflow.com",
            roles = "ESTUDIANTE"
    )
    void debePermitirAutoInscripcionDeEstudiante() throws Exception {
        when(cursoService.inscribirEstudiante(
                1L,
                "estudiante@campusflow.com"))
                .thenReturn(cursoResponse());

        mockMvc.perform(post("/api/cursos/1/inscripcion"))
                .andExpect(status().isOk());

        verify(cursoService).inscribirEstudiante(
                1L,
                "estudiante@campusflow.com"
        );
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeRechazarInscripcionComoAdministrador() throws Exception {
        mockMvc.perform(post("/api/cursos/1/inscripcion"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
            username = "estudiante@campusflow.com",
            roles = "ESTUDIANTE"
    )
    void debeResponder409EnInscripcionDuplicada() throws Exception {
        when(cursoService.inscribirEstudiante(
                1L,
                "estudiante@campusflow.com"))
                .thenThrow(new InscripcionDuplicadaException());

        mockMvc.perform(post("/api/cursos/1/inscripcion"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(
            username = "estudiante@campusflow.com",
            roles = "ESTUDIANTE"
    )
    void debeResponder404AlInscribirEnCursoInexistente()
            throws Exception {

        when(cursoService.inscribirEstudiante(
                99L,
                "estudiante@campusflow.com"))
                .thenThrow(new ResourceNotFoundException(
                        "Curso no encontrado con ID: 99"
                ));

        mockMvc.perform(post("/api/cursos/99/inscripcion"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void debeRechazarConsultaPorInstructorComoEstudiante()
            throws Exception {

        mockMvc.perform(get("/api/cursos/instructores/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeConsultarCursosPorInstructorComoAdministrador()
            throws Exception {

        when(cursoService.obtenerCursosPorInstructor(2L))
                .thenReturn(List.of(cursoResponse()));

        mockMvc.perform(get("/api/cursos/instructores/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeConsultarCursosPorEstudianteComoAdministrador()
            throws Exception {

        when(cursoService.obtenerCursosPorEstudiante(3L))
                .thenReturn(List.of(cursoResponse()));

        mockMvc.perform(get("/api/cursos/estudiantes/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    private CursoResponse cursoResponse() {
        CursoResponse response = new CursoResponse();
        response.setId(1L);
        response.setTitulo("Spring Boot");
        response.setDescripcion("Curso de Spring Boot");
        response.setInstructorId(2L);
        response.setInstructorNombre("Instructor");
        return response;
    }
}
