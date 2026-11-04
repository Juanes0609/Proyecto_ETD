package gui;

import gui.componentes.NotificadorUI;
import gui.integracion.ConectorSGA;
import gui.integracion.dto.UsuarioDTO;
import javafx.scene.Node;

/**
 * Servicios que una ventana (cliente o personal) ofrece a sus vistas:
 * notificaciones inline, hoja lateral, conector con el núcleo y usuario en sesión.
 */
public interface ContenedorVistas {

    NotificadorUI getNotificador();

    ConectorSGA getConector();

    UsuarioDTO getUsuario();

    /** Abre el "mini menú" lateral con un formulario o detalle contextual. */
    void mostrarHoja(String titulo, Node contenido);

    void ocultarHoja();
}
