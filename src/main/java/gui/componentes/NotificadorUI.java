package gui.componentes;

import excepciones.CapacidadExcedidaException;
import excepciones.EstructuraVaciaException;
import excepciones.FechasInvalidasException;
import excepciones.ReservaSolapadaException;
import excepciones.TransicionEstadoInvalidaException;
import javafx.animation.PauseTransition;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

/**
 * Banner de notificaciones inline. Sustituye a los {@code Alert} modales:
 * aparece encima del contenido, no bloquea la interacción y se oculta solo.
 */
public class NotificadorUI extends HBox {

    private static final String[] CLASES_TIPO = {
            "notificacion-exito", "notificacion-advertencia", "notificacion-error", "notificacion-info"
    };

    private final Label mensaje = new Label();
    private final javafx.scene.layout.StackPane espacioIcono = new javafx.scene.layout.StackPane();
    private final PauseTransition temporizador = new PauseTransition();

    public NotificadorUI() {
        getStyleClass().add("notificacion");
        mensaje.setWrapText(true);
        mensaje.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(mensaje, Priority.ALWAYS);

        Button cerrar = new Button();
        cerrar.setGraphic(Iconos.crear(Iconos.Icono.CERRAR, 16));
        cerrar.getStyleClass().add("boton-icono");
        cerrar.setAccessibleText("Cerrar notificación");
        cerrar.setOnAction(e -> ocultar());

        getChildren().addAll(espacioIcono, mensaje, cerrar);
        temporizador.setOnFinished(e -> ocultar());
        ocultar();
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
        } catch (ReservaSolapadaException e) {
            mostrarError("Reserva solapada: " + e.getMessage());
        } catch (CapacidadExcedidaException e) {
            mostrarError("Capacidad excedida: " + e.getMessage());
        } catch (FechasInvalidasException e) {
            mostrarError("Fechas inválidas: " + e.getMessage());
        } catch (TransicionEstadoInvalidaException e) {
            mostrarError("Cambio de estado no permitido: " + e.getMessage());
        } catch (EstructuraVaciaException e) {
            mostrarAdvertencia("Sin elementos: " + e.getMessage());
        } catch (UnsupportedOperationException e) {
            mostrarAdvertencia("Funcionalidad aún en construcción por el equipo.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarAdvertencia(e.getMessage());
        } catch (RuntimeException e) {
            mostrarError("Error inesperado: " + e.getMessage());
        }
        return false;
    }

    public void ocultar() {
        temporizador.stop();
        setVisible(false);
        setManaged(false);
    }

    private void mostrar(String claseTipo, Iconos.Icono icono, String texto, int segundos) {
        espacioIcono.getChildren().setAll(Iconos.crear(icono, 18));
        getStyleClass().removeAll(CLASES_TIPO);
        getStyleClass().add(claseTipo);
        mensaje.setText(texto);
        setVisible(true);
        setManaged(true);
        temporizador.stop();
        temporizador.setDuration(Duration.seconds(segundos));
        temporizador.playFromStart();
    }
}
