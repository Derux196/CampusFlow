package com.devsenior.Campusflow.usuarios.controller;

import com.devsenior.Campusflow.common.exception.EmailDuplicadoException;
import com.devsenior.Campusflow.common.exception.GlobalExceptionHandler;
import com.devsenior.Campusflow.common.exception.ResourceNotFoundException;
import com.devsenior.Campusflow.config.SecurityConfig;
import com.devsenior.Campusflow.security.JwtAuthenticationFilter;
import com.devsenior.Campusflow.security.JwtService;
import com.devsenior.Campusflow.usuarios.dto.UsuarioResponse;
import com.devsenior.Campusflow.usuarios.model.RolUsuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;
import com.devsenior.Campusflow.usuarios.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    GlobalExceptionHandler.class
})
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void debeRechazarCreacionSinAutenticacion() throws Exception {
        mockMvc.perform(
                post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "nombre": "Administrador",
                              "email": "admin@campusflow.com",
                              "password": "ClaveSegura123!",
                              "rol": "ADMINISTRADOR"
                            }
                            """)
        ).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeCrearUsuarioComoAdministradorSinExponerPassword()
            throws Exception {

        UsuarioResponse respuesta = usuarioResponse();

        when(usuarioService.crearUsuarioAdministrativo(any()))
                .thenReturn(respuesta);

        mockMvc.perform(
                post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "nombre": "Administrador",
                              "email": "admin@campusflow.com",
                              "password": "ClaveSegura123!",
                              "rol": "ADMINISTRADOR"
                            }
                            """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
        .andExpect(jsonPath("$.password").doesNotExist());

        verify(usuarioService).crearUsuarioAdministrativo(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeConsultarUsuarioPorId() throws Exception {

        when(usuarioService.obtenerUsuarioPorId(1L))
                .thenReturn(usuarioResponse());

        mockMvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email")
                        .value("admin@campusflow.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeResponder404CuandoUsuarioNoExiste() throws Exception {

        when(usuarioService.obtenerUsuarioPorId(999L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Usuario no encontrado con ID: 999"
                        )
                );

        mockMvc.perform(get("/api/usuarios/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeActualizarUsuario() throws Exception {

        UsuarioResponse respuesta = usuarioResponse();
        respuesta.setNombre("Administrador Actualizado");

        when(usuarioService.actualizarUsuario(any(), any()))
                .thenReturn(respuesta);

        mockMvc.perform(
                put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "nombre": "Administrador Actualizado",
                              "tema": "OSCURO",
                              "notificacionesActivas": false
                            }
                            """)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre")
                .value("Administrador Actualizado"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeRechazarActualizacionInvalida() throws Exception {

        mockMvc.perform(
                put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "nombre": ""
                            }
                            """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeResponder409CuandoElCorreoYaExiste() throws Exception {

        when(usuarioService.crearUsuarioAdministrativo(any()))
                .thenThrow(
                        new EmailDuplicadoException(
                                "El correo electrónico ya está registrado"
                        )
                );

        mockMvc.perform(
                post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "nombre": "Administrador",
                              "email": "admin@campusflow.com",
                              "password": "ClaveSegura123!",
                              "rol": "ADMINISTRADOR"
                            }
                            """)
        ).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void debeEliminarUsuarioSinRelacionesAcademicas()
            throws Exception {

        doNothing().when(usuarioService).eliminarUsuario(1L);

        mockMvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isNoContent());

        verify(usuarioService).eliminarUsuario(1L);
    }

    private UsuarioResponse usuarioResponse() {
        UsuarioResponse respuesta = new UsuarioResponse();
        respuesta.setId(1L);
        respuesta.setNombre("Administrador");
        respuesta.setEmail("admin@campusflow.com");
        respuesta.setRol(RolUsuario.ADMINISTRADOR);
        return respuesta;
    }
}
