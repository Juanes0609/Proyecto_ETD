package gui;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;

/**
 * Identidad de la aplicación (nombre y logo), siempre arriba a la izquierda.
 * <p>
 * Para poner la marca definitiva basta con:
 * <ul>
 *     <li>cambiar {@link #NOMBRE} y {@link #LEMA}, y</li>
 *     <li>guardar el logo cuadrado en {@code src/main/resources/gui/marca/logo.png}.</li>
 * </ul>
 * Mientras no exista el archivo se dibuja un monograma con las iniciales.
 */
public final class MarcaApp {

    public static final String NOMBRE = "Aparta-Suites UQ";
    public static final String LEMA = "Salento, Quindío";
    private static final String RUTA_LOGO = "/gui/marca/logo.png";

    private MarcaApp() {
    }

    /** Logo + nombre + lema, para barras superiores y sidebar. */
    public static HBox crear() {
        Label nombre = new Label(NOMBRE);
        nombre.getStyleClass().add("marca-nombre");
        Label lema = new Label(LEMA);
        lema.getStyleClass().add("marca-lema");

        HBox marca = new HBox(10, crearLogo(36), new VBox(0, nombre, lema));
        marca.getStyleClass().add("marca");
        marca.setAccessibleText(NOMBRE);
        return marca;
    }

    /** Solo el logo, del tamaño indicado (por ejemplo, en la tarjeta de inicio de sesión). */
    public static StackPane crearLogo(double tamano) {
        StackPane logo = new StackPane();
        logo.getStyleClass().add("marca-logo");
        logo.setMinSize(tamano, tamano);
        logo.setPrefSize(tamano, tamano);
        logo.setMaxSize(tamano, tamano);

        Image imagen = cargarLogo();
        if (imagen != null) {
            ImageView vista = new ImageView(imagen);
            vista.setFitWidth(tamano);
            vista.setFitHeight(tamano);
            vista.setPreserveRatio(true);
            vista.setSmooth(true);
            Rectangle recorte = new Rectangle(tamano, tamano);
            recorte.setArcWidth(tamano * 0.5);
            recorte.setArcHeight(tamano * 0.5);
            vista.setClip(recorte);
            logo.getChildren().add(vista);
        } else {
            Label monograma = new Label(iniciales());
            monograma.getStyleClass().add("marca-monograma");
            monograma.setStyle("-fx-font-size: " + Math.round(tamano * 0.38) + "px;");
            logo.getChildren().add(monograma);
        }
        return logo;
    }

    private static Image cargarLogo() {
        try (InputStream entrada = MarcaApp.class.getResourceAsStream(RUTA_LOGO)) {
            return entrada == null ? null : new Image(entrada);
        } catch (Exception e) {
            return null;
        }
    }

    private static String iniciales() {
        StringBuilder sb = new StringBuilder();
        for (String parte : NOMBRE.split("[\\s-]+")) {
            if (!parte.isEmpty() && Character.isLetter(parte.charAt(0)) && sb.length() < 2) {
                sb.append(Character.toUpperCase(parte.charAt(0)));
            }
        }
        return sb.toString();
    }
}
