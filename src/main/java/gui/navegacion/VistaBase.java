package gui.navegacion;

import gui.ContenedorVistas;
import gui.componentes.NotificadorUI;
import gui.integracion.ConectorSGA;
import gui.integracion.dto.UsuarioDTO;
import javafx.scene.Node;

/**
 * Clase base de todas las pantallas.
 * <p>
 * Cada vista construye su contenido una sola vez ({@link #construir()}) y se
 * reutiliza al volver a ella; {@link #alMostrar()} sirve para refrescar datos.
 * Desde aquí accede a los servicios compartidos de su ventana.
 */
public abstract class VistaBase {

    private final ContenedorVistas contenedor;
    private Node contenido;

    protected VistaBase(ContenedorVistas contenedor) {
        this.contenedor = contenedor;
    }

    /** Construye el nodo raíz de la vista. Se invoca una sola vez. */
    protected abstract Node construir();

    /** Se invoca cada vez que la vista pasa a ser la activa (refrescar datos, etc.). */
    public void alMostrar() {
        // Por defecto no hace nada.
    }

    /** Se invoca cuando la vista deja de ser la activa. */
    public void alOcultar() {
        // Por defecto no hace nada.
    }

    public final Node getContenido() {
        if (contenido == null) {
            contenido = construir();
            contenido.getStyleClass().add("vista");
        }
        return contenido;
    }

    protected ContenedorVistas contenedor() {
        return contenedor;
    }

    protected NotificadorUI notificador() {
        return contenedor.getNotificador();
    }

    protected ConectorSGA conector() {
        return contenedor.getConector();
    }

    protected UsuarioDTO usuario() {
        return contenedor.getUsuario();
    }

    protected void mostrarHoja(String titulo, Node contenidoHoja) {
        contenedor.mostrarHoja(titulo, contenidoHoja);
    }

    protected void ocultarHoja() {
        contenedor.ocultarHoja();
    }
}
