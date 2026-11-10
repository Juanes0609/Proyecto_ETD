package gui;

import gui.admin.VistaCatalogos;
import gui.admin.VistaEdificio;
import gui.admin.VistaReportes;
import gui.admin.VistaTemporadas;
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

    /** Tamaño de la ventana de trabajo (personal y huésped). */
    private static final double ANCHO_APP = 1280;
    private static final double ALTO_APP = 800;
    private static final double ANCHO_MIN_APP = 1024;
    private static final double ALTO_MIN_APP = 680;

    @Override
    public void start(Stage escenario) {
        this.escenario = escenario;
        // Sin tamaño fijo: la escena toma el tamaño preferido de su raíz, así el
        // inicio de sesión mide lo mismo que su tarjeta.
        escena = new Scene(new Region());
        escena.getStylesheets().add(getClass().getResource(HOJA_ESTILOS).toExternalForm());

        escenario.setScene(escena);
        mostrarLogin();
        escenario.show();
    }

    /** La ventana de inicio de sesión se ajusta a la tarjeta y no se redimensiona. */
    private void mostrarLogin() {
        escenario.setTitle(TITULO);
        escenario.setMinWidth(0);
        escenario.setMinHeight(0);
        escenario.setResizable(false);
        cambiarRaiz(new VistaLogin(conector, this::entrar));
        escenario.sizeToScene();
        escenario.centerOnScreen();
    }

    /** Al entrar, la misma ventana vuelve al tamaño de trabajo. */
    private void entrar(UsuarioDTO usuario) {
        ContextoApp contexto = new ContextoApp(conector, usuario, this::mostrarLogin);
        escenario.setTitle(TITULO + " — " + usuario.rol().getEtiqueta());
        cambiarRaiz(crearVentana(contexto, usuario.rol()));
        escenario.setResizable(true);
        // El tamaño de trabajo es el del contenido; se suma lo que ocupan los bordes del sistema.
        double bordeX = Math.max(0, escenario.getWidth() - escena.getWidth());
        double bordeY = Math.max(0, escenario.getHeight() - escena.getHeight());
        escenario.setWidth(ANCHO_APP + bordeX);
        escenario.setHeight(ALTO_APP + bordeY);
        escenario.setMinWidth(ANCHO_MIN_APP + bordeX);
        escenario.setMinHeight(ALTO_MIN_APP + bordeY);
        escenario.centerOnScreen();
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

        navegacion.registrar(TipoVista.ADMIN_REPORTES, VistaReportes::new);
        navegacion.registrar(TipoVista.ADMIN_EDIFICIO, VistaEdificio::new);
        navegacion.registrar(TipoVista.ADMIN_CATALOGOS, VistaCatalogos::new);
        navegacion.registrar(TipoVista.ADMIN_TEMPORADAS, VistaTemporadas::new);
    }

    private void cambiarRaiz(Parent raiz) {
        escena.setRoot(raiz);
        raiz.setOpacity(0);
        FadeTransition aparecer = new FadeTransition(Duration.millis(180), raiz);
        aparecer.setToValue(1);
        aparecer.play();
    }
}
