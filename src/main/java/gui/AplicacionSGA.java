package gui;

import gui.cliente.VentanaCliente;
import gui.integracion.ConectorSGA;
import gui.integracion.ConectorSGADemo;
import gui.integracion.dto.Rol;
import gui.integracion.dto.UsuarioDTO;
import gui.navegacion.GestorNavegacion;
import gui.navegacion.TipoVista;
import gui.recepcion.VistaFolios;
import gui.recepcion.VistaLlegadasSalidas;
import gui.recepcion.VistaPreparacion;
import gui.recepcion.VistaReservarRecepcion;
import gui.sesion.VistaLogin;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Aplicación JavaFX del SGA. Crea el único {@link Stage} del sistema y cambia
 * su contenido según la sesión: inicio de sesión → experiencia del rol → cerrar
 * sesión vuelve al inicio. Nunca se abre una segunda ventana.
 */
public class AplicacionSGA extends Application {

    public static final String TITULO = MarcaApp.NOMBRE;
    private static final String HOJA_ESTILOS = "/gui/estilos/sga-tema.css";

    /** Cambiar por {@code new ConectorSGAProveedor()} cuando el núcleo esté listo. */
    private final ConectorSGA conector = new ConectorSGADemo();
    private Stage escenario;
    private Scene escena;

    @Override
    public void start(Stage escenario) {
        this.escenario = escenario;
        escena = new Scene(new Region(), 1280, 800);
        escena.getStylesheets().add(getClass().getResource(HOJA_ESTILOS).toExternalForm());

        escenario.setMinWidth(1024);
        escenario.setMinHeight(680);
        escenario.setScene(escena);
        mostrarLogin();
        escenario.show();
    }

    private void mostrarLogin() {
        escenario.setTitle(TITULO);
        cambiarRaiz(new VistaLogin(conector, this::entrar));
    }

    private void entrar(UsuarioDTO usuario) {
        ContextoApp contexto = new ContextoApp(conector, usuario, this::mostrarLogin);
        escenario.setTitle(TITULO + " — " + usuario.rol().getEtiqueta());
        cambiarRaiz(crearVentana(contexto, usuario.rol()));
    }

    private Parent crearVentana(ContextoApp contexto, Rol rol) {
        if (rol == Rol.HUESPED) {
            return new VentanaCliente(contexto);
        }
        VentanaPrincipalSGA ventana = new VentanaPrincipalSGA(contexto, rol);
        registrarVistas(ventana.getNavegacion());
        ventana.iniciar();
        return ventana;
    }

    /**
     * Registro de destinos del personal: agregar una sección nueva es una
     * constante en {@code TipoVista} y una línea aquí.
     */
    private void registrarVistas(GestorNavegacion navegacion) {
        navegacion.registrar(TipoVista.RECEPCION_RESERVAR, VistaReservarRecepcion::new);
        navegacion.registrar(TipoVista.RECEPCION_LLEGADAS, VistaLlegadasSalidas::new);
        navegacion.registrar(TipoVista.RECEPCION_FOLIOS, VistaFolios::new);
        navegacion.registrar(TipoVista.RECEPCION_PREPARACION, VistaPreparacion::new);
    }

    private void cambiarRaiz(Parent raiz) {
        escena.setRoot(raiz);
        raiz.setOpacity(0);
        FadeTransition aparecer = new FadeTransition(Duration.millis(180), raiz);
        aparecer.setToValue(1);
        aparecer.play();
    }
}
