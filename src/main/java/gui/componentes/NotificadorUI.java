package gui.componentes;

import excepciones.CapacidadExcedidaException;
import excepciones.EstructuraVaciaException;
import excepciones.FechasInvalidasException;
import excepciones.ReservaSolapadaException;
import excepciones.TransicionEstadoInvalidaException;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Notificación flotante ("toast"). Sustituye a los {@code Alert} modales: una
 * burbuja blanca, como la barra de pestañas del huésped, que sube suavemente
 * desde la parte inferior del panel central, no bloquea la interacción y se
 * oculta sola. El tipo (éxito, aviso, error, información) se indica con el
 * color del círculo del icono, no con todo el fondo.
 * <p>
 * La ventana la coloca en el {@code StackPane} central con
 * {@link #colocarEn(StackPane)}. Al pasar el ratón por encima el temporizador
 * se pausa, para que dé tiempo de leer.
 */
public class NotificadorUI extends HBox {

    private static final Duration ENTRADA = Duration.millis(200);
    private static final Duration SALIDA = Duration.millis(160);
    private static final double DESPLAZAMIENTO = 14;

    private static final String[] CLASES_TIPO = {
            "notificacion-exito", "notificacion-advertencia", "notificacion-error", "notificacion-info"
    };

    private final Label mensaje = new Label();
    private final StackPane espacioIcono = new StackPane();
    private final PauseTransition temporizador = new PauseTransition();
    private final FadeTransition fundido = new FadeTransition();
    private final TranslateTransition desliz = new TranslateTransition();
    private final ParallelTransition animacion = new ParallelTransition(this, fundido, desliz);

    public NotificadorUI() {
        getStyleClass().add("notificacion");
        setAlignment(Pos.CENTER_LEFT);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE); // solo ocupa lo que mide la burbuja
        setPickOnBounds(false);
        mensaje.setWrapText(true);
        mensaje.setMaxWidth(440);
        mensaje.getStyleClass().add("notificacion-texto");
        espacioIcono.getStyleClass().add("notificacion-icono");

        Button cerrar = new Button();
        cerrar.setGraphic(Iconos.crear(Iconos.Icono.CERRAR, 14));
        cerrar.getStyleClass().addAll("boton-icono", "notificacion-cerrar");
        cerrar.setAccessibleText("Cerrar notificación");
        cerrar.setOnAction(e -> ocultar());

        getChildren().addAll(espacioIcono, mensaje, cerrar);
        temporizador.setOnFinished(e -> ocultar());
        setOnMouseEntered(e -> temporizador.pause());
        setOnMouseExited(e -> {
            if (isVisible() && temporizador.getStatus() == javafx.animation.Animation.Status.PAUSED) {
                temporizador.play();
            }
        });
        setVisible(false);
        setOpacity(0);
    }

    /** Pone la burbuja flotando abajo y al centro del panel indicado. */
    public void colocarEn(StackPane panel) {
        panel.getChildren().add(this);
        StackPane.setAlignment(this, Pos.BOTTOM_CENTER);
        StackPane.setMargin(this, new javafx.geometry.Insets(0, 24, 24, 24));
    }

    public void mostrarExito(String texto) {
        mostrar("notificacion-exito", Iconos.Icono.CHECK, texto, 4);
    }

    public void mostrarInfo(String texto) {
        mostrar("notificacion-info", Iconos.Icono.INFO, texto, 5);
    }

    public void mostrarAdvertencia(String texto) {
        mostrar("notificacion-advertencia", Iconos.Icono.ALERTA, texto, 6);
    }

    public void mostrarError(String texto) {
        mostrar("notificacion-error", Iconos.Icono.ALERTA, texto, 8);
    }

    /**
     * Ejecuta una acción del usuario y traduce las excepciones propias del dominio
     * a mensajes en el banner, de modo que ninguna vista necesita su propio try/catch.
     *
     * @return {@code true} si la acción terminó sin errores
     */
    public boolean ejecutarConManejo(Runnable accion, String mensajeExito) {
        try {
            accion.run();
            if (mensajeExito != null && !mensajeExito.isBlank()) {
                mostrarExito(mensajeExito);
            }
            return true;
        } catch (RuntimeException e) {
            if (esAdvertencia(e)) {
                mostrarAdvertencia(mensajeDe(e));
            } else {
                mostrarError(mensajeDe(e));
            }
            return false;
        }
    }

    /** Mensaje para el usuario a partir de una excepción del dominio o de validación. */
    public static String mensajeDe(RuntimeException e) {
        return switch (e) {
            case ReservaSolapadaException x -> "Fechas no disponibles: " + x.getMessage();
            case CapacidadExcedidaException x -> "Capacidad excedida: " + x.getMessage();
            case FechasInvalidasException x -> "Fechas inválidas: " + x.getMessage();
            case TransicionEstadoInvalidaException x -> "No se puede hacer este cambio: " + x.getMessage();
            case EstructuraVaciaException x -> x.getMessage();
            case UnsupportedOperationException x -> "Esta funcionalidad aún está en construcción por el equipo.";
            case IllegalArgumentException x -> x.getMessage();
            case IllegalStateException x -> x.getMessage();
            default -> "Error inesperado: " + e.getMessage();
        };
    }

    private static boolean esAdvertencia(RuntimeException e) {
        return e instanceof EstructuraVaciaException || e instanceof UnsupportedOperationException
                || e instanceof IllegalArgumentException || e instanceof IllegalStateException;
    }

    /** Baja y se desvanece; es interrumpible si llega otro mensaje a mitad de camino. */
    public void ocultar() {
        temporizador.stop();
        if (!isVisible()) {
            return;
        }
        animar(0, DESPLAZAMIENTO * 0.6, SALIDA, Interpolator.EASE_IN);
        animacion.setOnFinished(e -> setVisible(false));
    }

    private void mostrar(String claseTipo, Iconos.Icono icono, String texto, int segundos) {
        espacioIcono.getChildren().setAll(Iconos.crear(icono, 15));
        getStyleClass().removeAll(CLASES_TIPO);
        getStyleClass().add(claseTipo);
        mensaje.setText(texto);
        setAccessibleText(texto);
        if (!isVisible() || animacion.getOnFinished() != null) {
            // Entra desde abajo; si ya estaba visible, solo se cambia el texto.
            if (!isVisible()) {
                setTranslateY(DESPLAZAMIENTO);
                setOpacity(0);
            }
            setVisible(true);
            animacion.setOnFinished(null);
            animar(1, 0, ENTRADA, Interpolator.SPLINE(0.2, 0.8, 0.2, 1));
        }
        temporizador.stop();
        temporizador.setDuration(Duration.seconds(segundos));
        temporizador.playFromStart();
    }

    private void animar(double opacidad, double y, Duration duracion, Interpolator curva) {
        animacion.stop(); // continúa desde la posición actual, sin saltos
        fundido.setDuration(duracion);
        fundido.setToValue(opacidad);
        fundido.setInterpolator(curva);
        desliz.setDuration(duracion);
        desliz.setToY(y);
        desliz.setInterpolator(curva);
        animacion.playFromStart();
    }
}
