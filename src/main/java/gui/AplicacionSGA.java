package gui;

import gui.integracion.ConectorSGAProveedor;
import gui.navegacion.GestorNavegacion;
import gui.navegacion.TipoVista;
import gui.vistas.VistaMenuPrincipal;
import gui.vistas.VistaTableroApartamentos;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Aplicación JavaFX del SGA. Crea el único {@link Stage} del sistema,
 * aplica la hoja de estilos centralizada y registra las vistas.
 */
public class AplicacionSGA extends Application {

    public static final String TITULO = "SGA — Aparta-Suites UQ";
    private static final String HOJA_ESTILOS = "/gui/estilos/sga-tema.css";

    @Override
    public void start(Stage escenario) {
        VentanaPrincipalSGA ventana = new VentanaPrincipalSGA(new ConectorSGAProveedor());
        registrarVistas(ventana.getNavegacion());
        ventana.iniciar(TipoVista.MENU_PRINCIPAL);

        Scene escena = new Scene(ventana, 1280, 800);
        escena.getStylesheets().add(getClass().getResource(HOJA_ESTILOS).toExternalForm());

        escenario.setTitle(TITULO);
        escenario.setMinWidth(960);
        escenario.setMinHeight(640);
        escenario.setScene(escena);
        escenario.show();
    }

    /**
     * Registro de vistas: para agregar una sección nueva basta con una constante
     * en {@link TipoVista} y una línea aquí.
     */
    private void registrarVistas(GestorNavegacion navegacion) {
        navegacion.registrar(TipoVista.MENU_PRINCIPAL, VistaMenuPrincipal::new);
        navegacion.registrar(TipoVista.TABLERO_APARTAMENTOS, VistaTableroApartamentos::new);
    }
}
