
package com.devsenior.Campusflow.usuarios.service;

import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;

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
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

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

        List<Usuario> resultado =
            usuarioService.listarUsuarios();

        assertEquals(1, resultado.size());

        assertEquals(
            "Usuario Prueba",
            resultado.get(0).getNombre()
        );

        verify(usuarioRepository).findAll();
    }
}
