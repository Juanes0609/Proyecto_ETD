package gui.navegacion;

import gui.VentanaPrincipalSGA;
import gui.componentes.NotificadorUI;
import gui.integracion.ConectorSGA;
import javafx.scene.Node;

/**
 * Clase base de todas las pantallas del área central.
 * <p>
 * Cada vista construye su contenido una sola vez ({@link #construir()}) y se
 * reutiliza al volver a ella, conservando lo que el usuario había escrito.
 * Desde aquí accede a los servicios compartidos de la ventana: inspector
 * lateral, notificaciones inline, navegación y conector con el núcleo.
 */
public abstract class VistaBase {

    private final VentanaPrincipalSGA ventana;
    private Node contenido;

    protected VistaBase(VentanaPrincipalSGA ventana) {
        this.ventana = ventana;
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

    protected VentanaPrincipalSGA ventana() {
        return ventana;
    }

    protected NotificadorUI notificador() {
        return ventana.getNotificador();
    }

    protected ConectorSGA conector() {
        return ventana.getConector();
    }

    protected void navegarA(TipoVista destino) {
        ventana.getNavegacion().navegarA(destino);
    }

    protected void mostrarInspector(String titulo, Node contenidoInspector) {
        ventana.mostrarInspector(titulo, contenidoInspector);
    }

    protected void ocultarInspector() {
        ventana.ocultarInspector();
    }
}
