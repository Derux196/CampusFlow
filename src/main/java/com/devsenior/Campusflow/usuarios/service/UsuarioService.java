
package com.devsenior.Campusflow.usuarios.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.model.RolUsuario;
import com.devsenior.Campusflow.usuarios.repository.UsuarioRepository;
import com.devsenior.Campusflow.usuarios.dto.CrearUsuarioRequest;
import com.devsenior.Campusflow.usuarios.dto.UsuarioResponse;
import com.devsenior.Campusflow.usuarios.mapper.UsuarioMapper;
import com.devsenior.Campusflow.usuarios.dto.ActualizarUsuarioRequest;
import com.devsenior.Campusflow.usuarios.model.PreferenciasUsuario;
import com.devsenior.Campusflow.common.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import com.devsenior.Campusflow.common.exception.EmailDuplicadoException;
import com.devsenior.Campusflow.common.exception.UsuarioConRelacionesException;

@Service
public class UsuarioService {

private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

   public UsuarioService(
        UsuarioRepository usuarioRepository,
        PasswordEncoder passwordEncoder) {

    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
}


@Transactional
public UsuarioResponse actualizarUsuario(
        Long id,
        ActualizarUsuarioRequest request) {

    Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Usuario no encontrado con ID: " + id
                    )
            );

    usuario.setNombre(request.getNombre());

    PreferenciasUsuario preferencias =
            usuario.getPreferencias();

    if (preferencias == null) {
        preferencias = new PreferenciasUsuario();
        usuario.setPreferencias(preferencias);
    }

    if (request.getTema() != null) {
        preferencias.setTema(request.getTema());
    }

    if (request.getNotificacionesActivas() != null) {
        preferencias.setNotificacionesActivas(
                request.getNotificacionesActivas()
        );
    }

    Usuario actualizado = usuarioRepository.save(usuario);

    return UsuarioMapper.toResponse(actualizado);
}

  public List<UsuarioResponse> listarUsuarios() {
    return usuarioRepository.findAll()
            .stream()
            .map(UsuarioMapper::toResponse)
            .toList();
}

  public UsuarioResponse obtenerUsuarioPorId(Long id) {
    return usuarioRepository.findById(id)
            .map(UsuarioMapper::toResponse)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Usuario no encontrado con ID: " + id
                    )
            );
}

public UsuarioResponse crearUsuarioAdministrativo(
        CrearUsuarioRequest request) {

    return crearUsuario(request);
}

public UsuarioResponse registrarUsuario(
        CrearUsuarioRequest request) {

    if (request.getRol() != RolUsuario.ESTUDIANTE &&
            request.getRol() != RolUsuario.INSTRUCTOR) {

        throw new IllegalArgumentException(
                "Solo se permite el registro como estudiante o instructor"
        );
    }

    return crearUsuario(request);
}

@Transactional
public void eliminarUsuario(Long id) {
    Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Usuario no encontrado con ID: " + id
                    )
            );

    if (!usuario.getCursosDictados().isEmpty()
            || !usuario.getCursosInscritos().isEmpty()) {
        throw new UsuarioConRelacionesException();
    }

    usuarioRepository.delete(usuario);
}

private UsuarioResponse crearUsuario(CrearUsuarioRequest request) {
    if (usuarioRepository.existsByEmail(request.getEmail())) {
        throw new EmailDuplicadoException(
                "El correo electrónico ya está registrado"
        );
    }

    Usuario usuario = new Usuario();

    usuario.setNombre(request.getNombre());
    usuario.setEmail(request.getEmail());
    usuario.setRol(request.getRol());

    // La contraseña se guarda como hash BCrypt
    usuario.setPassword(
            passwordEncoder.encode(request.getPassword())
    );

    Usuario guardado = usuarioRepository.save(usuario);

    return UsuarioMapper.toResponse(guardado);
}

}
