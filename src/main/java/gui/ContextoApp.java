package gui;

import gui.integracion.ConectorSGA;
import gui.integracion.dto.UsuarioDTO;

/** Datos de la sesión activa que comparten las ventanas de cada rol. */
public final class ContextoApp {

    private final ConectorSGA conector;
    private final Runnable alCerrarSesion;
    private UsuarioDTO usuario;

    public ContextoApp(ConectorSGA conector, UsuarioDTO usuario, Runnable alCerrarSesion) {
        this.conector = conector;
        this.usuario = usuario;
        this.alCerrarSesion = alCerrarSesion;
    }

    public ConectorSGA getConector() {
        return conector;
    }

    public UsuarioDTO getUsuario() {
        return usuario;
    }

    /** Actualiza el usuario en sesión (por ejemplo, tras editar el perfil). */
    public void setUsuario(UsuarioDTO usuario) {
        this.usuario = usuario;
    }

    public void cerrarSesion() {
        alCerrarSesion.run();
    }
}
