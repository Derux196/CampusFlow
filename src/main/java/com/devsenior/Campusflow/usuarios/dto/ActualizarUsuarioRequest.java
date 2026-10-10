
package com.devsenior.Campusflow.usuarios.dto;

import com.devsenior.Campusflow.usuarios.model.TemaVisual;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ActualizarUsuarioRequest {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    private TemaVisual tema;

    private Boolean notificacionesActivas;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TemaVisual getTema() {
        return tema;
    }

    public void setTema(TemaVisual tema) {
        this.tema = tema;
    }

    public Boolean getNotificacionesActivas() {
        return notificacionesActivas;
    }

    public void setNotificacionesActivas(
            Boolean notificacionesActivas) {
        this.notificacionesActivas = notificacionesActivas;
    }
}
