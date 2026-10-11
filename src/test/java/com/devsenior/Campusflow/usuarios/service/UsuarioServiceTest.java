
package com.devsenior.Campusflow.usuarios.service;

import com.devsenior.Campusflow.usuarios.dto.CrearUsuarioRequest;
import com.devsenior.Campusflow.usuarios.dto.UsuarioResponse;
import com.devsenior.Campusflow.usuarios.model.RolUsuario;
import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;
import com.devsenior.Campusflow.common.exception.ResourceNotFoundException;
import com.devsenior.Campusflow.common.exception.EmailDuplicadoException;
import com.devsenior.Campusflow.common.exception.UsuarioConRelacionesException;
import com.devsenior.Campusflow.cursos.model.Curso;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.Optional;
import com.devsenior.Campusflow.usuarios.dto.ActualizarUsuarioRequest;
import com.devsenior.Campusflow.usuarios.model.TemaVisual;


import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void debeListarUsuarios() {

        Usuario usuario = new Usuario(
                "Usuario Prueba",
                "prueba@campusflow.com"
        );

        when(usuarioRepository.findAll())
                .thenReturn(List.of(usuario));

        List<UsuarioResponse> resultado =
                usuarioService.listarUsuarios();

        assertEquals(1, resultado.size());
        assertEquals(
                "Usuario Prueba",
                resultado.get(0).getNombre()
        );

        verify(usuarioRepository).findAll();
    }

    @Test
    void debeRegistrarUsuarioConPasswordCodificado() {

        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombre("Carlos Pérez");
        request.setEmail("carlos@example.com");
        request.setPassword("ClaveSegura123!");
        request.setRol(RolUsuario.ESTUDIANTE);

        when(passwordEncoder.encode("ClaveSegura123!"))
                .thenReturn("hashBCryptSimulado");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        UsuarioResponse resultado =
                usuarioService.registrarUsuario(request);

        assertEquals("Carlos Pérez", resultado.getNombre());
        assertEquals(RolUsuario.ESTUDIANTE, resultado.getRol());

        verify(passwordEncoder).encode("ClaveSegura123!");
        verify(usuarioRepository).save(
                argThat(usuario ->
                        "hashBCryptSimulado".equals(
                                usuario.getPassword()
                        )
                )
        );
    }

    @Test
    void debeRechazarRegistroComoAdministrador() {

        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombre("Administrador");
        request.setEmail("admin@example.com");
        request.setPassword("ClaveSegura123!");
        request.setRol(RolUsuario.ADMINISTRADOR);

        IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> usuarioService.registrarUsuario(request)
                );

      assertEquals(
    "Solo se permite el registro como estudiante o instructor",
    error.getMessage()
);

        verify(usuarioRepository, never())
                .save(any(Usuario.class));

        verifyNoInteractions(passwordEncoder);
    }


@Test
void debeActualizarNombreYPreferencias() {

    Usuario usuario = new Usuario(
            "Usuario Prueba",
            "prueba@campusflow.com"
    );
    usuario.setId(1L);

    ActualizarUsuarioRequest request =
            new ActualizarUsuarioRequest();

    request.setNombre("Usuario Actualizado");
    request.setTema(TemaVisual.OSCURO);
    request.setNotificacionesActivas(false);

    when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(usuario));

    when(usuarioRepository.save(any(Usuario.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

    UsuarioResponse resultado =
            usuarioService.actualizarUsuario(1L, request);

    assertEquals("Usuario Actualizado", resultado.getNombre());
    assertEquals(TemaVisual.OSCURO, resultado.getTema());
    assertEquals(false, resultado.getNotificacionesActivas());

    verify(usuarioRepository).findById(1L);
    verify(usuarioRepository).save(usuario);
}

@Test
void debeRechazarActualizacionDeUsuarioInexistente() {

    Long id = 999L;

    ActualizarUsuarioRequest request =
            new ActualizarUsuarioRequest();

    request.setNombre("Usuario Actualizado");

    when(usuarioRepository.findById(id))
            .thenReturn(Optional.empty());

    ResourceNotFoundException error =
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> usuarioService.actualizarUsuario(id, request)
            );

    assertEquals(
            "Usuario no encontrado con ID: 999",
            error.getMessage()
    );

    verify(usuarioRepository).findById(id);
    verify(usuarioRepository, never())
            .save(any(Usuario.class));
}



@Test
void debeRechazarCorreoDuplicado() {

    CrearUsuarioRequest request = new CrearUsuarioRequest();

    request.setNombre("Estudiante Nuevo");
    request.setEmail("prueba@campusflow.com");
    request.setPassword("ClaveSegura123!");
    request.setRol(RolUsuario.ESTUDIANTE);

    when(usuarioRepository.existsByEmail(
            "prueba@campusflow.com"
    )).thenReturn(true);

    EmailDuplicadoException error = assertThrows(
            EmailDuplicadoException.class,
            () -> usuarioService.registrarUsuario(request)
    );

    assertEquals(
            "El correo electrónico ya está registrado",
            error.getMessage()
    );

    verify(usuarioRepository)
            .existsByEmail("prueba@campusflow.com");

    verify(usuarioRepository, never())
            .save(any(Usuario.class));

    verifyNoInteractions(passwordEncoder);
}

@Test
void debeCrearAdministradorDesdeOperacionAdministrativa() {

    CrearUsuarioRequest request = new CrearUsuarioRequest();
    request.setNombre("Administrador");
    request.setEmail("admin@campusflow.com");
    request.setPassword("ClaveSegura123!");
    request.setRol(RolUsuario.ADMINISTRADOR);

    when(passwordEncoder.encode("ClaveSegura123!"))
            .thenReturn("hashBCryptSimulado");
    when(usuarioRepository.save(any(Usuario.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

    UsuarioResponse resultado =
            usuarioService.crearUsuarioAdministrativo(request);

    assertEquals(RolUsuario.ADMINISTRADOR, resultado.getRol());
    verify(passwordEncoder).encode("ClaveSegura123!");
    verify(usuarioRepository).save(
            argThat(usuario ->
                    "hashBCryptSimulado".equals(usuario.getPassword())
            )
    );
}

@Test
void debeObtenerUsuarioPorId() {

    Usuario usuario = new Usuario(
            "Usuario Prueba",
            "prueba@campusflow.com"
    );
    usuario.setId(1L);

    when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(usuario));

    UsuarioResponse resultado =
            usuarioService.obtenerUsuarioPorId(1L);

    assertEquals(1L, resultado.getId());
    assertEquals("Usuario Prueba", resultado.getNombre());
}

@Test
void debeRechazarConsultaDeUsuarioInexistente() {

    when(usuarioRepository.findById(999L))
            .thenReturn(Optional.empty());

    assertThrows(
            ResourceNotFoundException.class,
            () -> usuarioService.obtenerUsuarioPorId(999L)
    );
}

@Test
void debeEliminarUsuarioSinRelacionesAcademicas() {

    Usuario usuario = new Usuario(
            "Usuario Prueba",
            "prueba@campusflow.com"
    );

    when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(usuario));

    usuarioService.eliminarUsuario(1L);

    verify(usuarioRepository).delete(usuario);
}

@Test
void debeRechazarEliminacionDeUsuarioConCursosDictados() {

    Usuario usuario = new Usuario(
            "Instructor",
            "instructor@campusflow.com"
    );
    usuario.getCursosDictados().add(
            new Curso("Curso de prueba", "Descripcion")
    );

    when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(usuario));

    assertThrows(
            UsuarioConRelacionesException.class,
            () -> usuarioService.eliminarUsuario(1L)
    );

    verify(usuarioRepository, never()).delete(any(Usuario.class));
}

@Test
void debeRechazarEliminacionDeUsuarioConCursosInscritos() {

    Usuario usuario = new Usuario(
            "Estudiante",
            "estudiante@campusflow.com"
    );
    usuario.getCursosInscritos().add(
            new Curso("Curso de prueba", "Descripcion")
    );

    when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(usuario));

    assertThrows(
            UsuarioConRelacionesException.class,
            () -> usuarioService.eliminarUsuario(1L)
    );

    verify(usuarioRepository, never()).delete(any(Usuario.class));
}


}
