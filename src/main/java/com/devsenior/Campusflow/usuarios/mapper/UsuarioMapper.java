
package com.devsenior.Campusflow.usuarios.mapper;

import com.devsenior.Campusflow.usuarios.dto.UsuarioResponse;
import com.devsenior.Campusflow.usuarios.model.Usuario;
import com.devsenior.Campusflow.usuarios.model.PreferenciasUsuario;

public class UsuarioMapper {

    public static UsuarioResponse toResponse(Usuario usuario) {

        PreferenciasUsuario preferencias =
                usuario.getPreferencias();

        if (preferencias == null) {
            preferencias = new PreferenciasUsuario();
        }

        UsuarioResponse response = new UsuarioResponse();

        response.setId(usuario.getId());
        response.setNombre(usuario.getNombre());
        response.setEmail(usuario.getEmail());
        response.setRol(usuario.getRol());
        response.setTema(preferencias.getTema());
        response.setNotificacionesActivas(
                preferencias.getNotificacionesActivas()
        );

        return response;
    }
}
