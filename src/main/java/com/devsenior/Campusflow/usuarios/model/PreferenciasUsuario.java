
package com.devsenior.Campusflow.usuarios.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;

@Embeddable
public class PreferenciasUsuario {

    @Enumerated(EnumType.STRING)
    @Column(name = "tema")
    private TemaVisual tema = TemaVisual.CLARO;

    @Column(name = "notificaciones_activas")
    private Boolean notificacionesActivas = true;

    public PreferenciasUsuario() {
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

    public void setNotificacionesActivas(Boolean notificacionesActivas) {
        this.notificacionesActivas = notificacionesActivas;
    }
}
