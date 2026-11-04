import gui.AplicacionSGA;
import javafx.application.Application;

/**
 * Punto de entrada del SGA.
 * <p>
 * Se mantiene separado de {@link AplicacionSGA} (que extiende {@code Application})
 * para que IntelliJ IDEA pueda ejecutar el proyecto con el botón Run sin configurar
 * {@code --module-path}: al no heredar de {@code Application}, el lanzador de Java no
 * exige que JavaFX esté en el module path.
 */
public class Main {

    public static void main(String[] args) {
        Application.launch(AplicacionSGA.class, args);
    }
}
