package gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Aplicación JavaFX del SGA. Crea el único {@link Stage} del sistema
 * y aplica la hoja de estilos centralizada.
 */
public class AplicacionSGA extends Application {

    public static final String TITULO = "SGA — Aparta-Suites UQ";
    private static final String HOJA_ESTILOS = "/gui/estilos/sga-tema.css";

    @Override
    public void start(Stage escenario) {
        StackPane raiz = new StackPane(new Label(TITULO));
        Scene escena = new Scene(raiz, 1280, 800);
        escena.getStylesheets().add(getClass().getResource(HOJA_ESTILOS).toExternalForm());

        escenario.setTitle(TITULO);
        escenario.setMinWidth(960);
        escenario.setMinHeight(640);
        escenario.setScene(escena);
        escenario.show();
    }
}
