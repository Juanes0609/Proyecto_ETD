package gui.componentes;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Panel que se desliza desde la derecha dentro de la misma ventana: el "mini menú"
 * para formularios contextuales (reservar, pagar, editar) y detalles.
 * <p>
 * Siguiendo a {@code apple-design}: entra y sale por el mismo lado, es
 * interrumpible (cada animación arranca desde la posición actual) y no oscurece
 * el resto de la pantalla porque es una tarea paralela, no bloqueante.
 * Se cierra con la X o con Escape.
 */
public class HojaLateral extends StackPane {

    private static final Duration DURACION = Duration.millis(220);
    private static final Interpolator SALIDA_SUAVE = Interpolator.SPLINE(0.2, 0.8, 0.2, 1.0);
    private static final double ANCHO = 420;

    private final VBox panel = new VBox();
    private final Label titulo = new Label();
    private final VBox cuerpo = new VBox();
    private ParallelTransition animacion;

    public HojaLateral() {
        getStyleClass().add("capa-hoja");
        setPickOnBounds(false);
        setAlignment(Pos.CENTER_RIGHT);

        titulo.getStyleClass().add("hoja-titulo");
        titulo.setWrapText(true);
        Button cerrar = new Button();
        cerrar.setGraphic(Iconos.crear(Iconos.Icono.CERRAR, 18));
        cerrar.getStyleClass().add("boton-icono");
        cerrar.setAccessibleText("Cerrar panel");
        cerrar.setOnAction(e -> ocultar());
        HBox encabezado = new HBox(12, titulo, FabricaComponentesUI.crearEspaciador(), cerrar);
        encabezado.setAlignment(Pos.TOP_LEFT);

        ScrollPane scroll = new ScrollPane(cuerpo);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getStyleClass().add("hoja-lateral");
        panel.setPrefWidth(ANCHO);
        panel.setMaxWidth(ANCHO);
        panel.getChildren().addAll(encabezado, scroll);
        panel.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                ocultar();
            }
        });
        StackPane.setMargin(panel, new javafx.geometry.Insets(12));
        panel.setVisible(false);
        getChildren().add(panel);
    }

    public boolean estaAbierta() {
        return panel.isVisible() && panel.getTranslateX() < ANCHO;
    }

    /** Abre la hoja con un título y su contenido (formulario, detalle...). */
    public void mostrar(String textoTitulo, Node contenido) {
        titulo.setText(textoTitulo);
        cuerpo.getChildren().setAll(contenido);
        if (!panel.isVisible()) {
            panel.setTranslateX(ANCHO);
            panel.setOpacity(0);
            panel.setVisible(true);
        }
        animarHacia(0, 1, null);
        panel.requestFocus();
    }

    public void ocultar() {
        if (!panel.isVisible()) {
            return;
        }
        animarHacia(ANCHO, 0, () -> {
            panel.setVisible(false);
            cuerpo.getChildren().clear();
        });
    }

    private void animarHacia(double x, double opacidad, Runnable alTerminar) {
        if (animacion != null) {
            animacion.stop(); // interrumpible: la siguiente parte desde la posición actual
        }
        TranslateTransition mover = new TranslateTransition(DURACION, panel);
        mover.setToX(x);
        mover.setInterpolator(SALIDA_SUAVE);
        FadeTransition fundido = new FadeTransition(DURACION, panel);
        fundido.setToValue(opacidad);
        fundido.setInterpolator(SALIDA_SUAVE);
        animacion = new ParallelTransition(mover, fundido);
        if (alTerminar != null) {
            animacion.setOnFinished(e -> alTerminar.run());
        }
        animacion.play();
    }
}
