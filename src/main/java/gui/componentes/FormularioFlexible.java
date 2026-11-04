package gui.componentes;

import javafx.scene.Node;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formulario declarativo: cada campo es una línea {@code agregarCampo(...)}.
 * Agregar o quitar un atributo de {@code Apartamento}, {@code Huesped} o
 * {@code Reserva} en la interfaz se reduce a añadir o borrar esa línea.
 * <p>
 * Los campos se identifican por su etiqueta, que también se usa en los
 * mensajes de validación.
 */
public class FormularioFlexible extends GridPane {

    private final Map<String, Control> campos = new LinkedHashMap<>();
    private final List<String> obligatorios = new ArrayList<>();
    private int filaActual = 0;

    public FormularioFlexible() {
        getStyleClass().add("formulario");
        ColumnConstraints columna = new ColumnConstraints();
        columna.setHgrow(Priority.ALWAYS);
        columna.setFillWidth(true);
        getColumnConstraints().add(columna);
    }

    /** Agrega un campo opcional. */
    public FormularioFlexible agregarCampo(String etiqueta, Control control, String ayuda) {
        return agregarCampo(etiqueta, etiqueta, control, ayuda);
    }

    /** Agrega un campo que {@link #validarObligatorios()} exigirá diligenciado. */
    public FormularioFlexible agregarCampoObligatorio(String etiqueta, Control control, String ayuda) {
        obligatorios.add(etiqueta);
        return agregarCampo(etiqueta, etiqueta + " *", control, ayuda);
    }

    private FormularioFlexible agregarCampo(String clave, String etiqueta, Control control, String ayuda) {
        Label lblEtiqueta = new Label(etiqueta);
        lblEtiqueta.getStyleClass().add("formulario-etiqueta");
        lblEtiqueta.setLabelFor(control);
        control.setMaxWidth(Double.MAX_VALUE);
        control.setAccessibleText(etiqueta);

        VBox bloque = new VBox(4, lblEtiqueta, control);
        if (ayuda != null && !ayuda.isBlank()) {
            Label lblAyuda = new Label(ayuda);
            lblAyuda.getStyleClass().add("formulario-ayuda");
            lblAyuda.setWrapText(true);
            bloque.getChildren().add(lblAyuda);
        }
        add(bloque, 0, filaActual++);
        campos.put(clave, control);
        return this;
    }

    /** Agrega un título que agrupa los campos siguientes. */
    public FormularioFlexible agregarSeccion(String titulo) {
        Label lbl = new Label(titulo);
        lbl.getStyleClass().add("encabezado-vista");
        add(lbl, 0, filaActual++);
        return this;
    }

    /** Agrega cualquier nodo (por ejemplo, la barra de acciones) al final del formulario. */
    public FormularioFlexible agregarNodo(Node nodo) {
        add(nodo, 0, filaActual++);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <C extends Control> C obtenerControl(String etiqueta) {
        return (C) campos.get(etiqueta);
    }

    /**
     * Marca en rojo los campos obligatorios vacíos.
     *
     * @return etiquetas de los campos que faltan (vacía si el formulario es válido)
     */
    public List<String> validarObligatorios() {
        List<String> faltantes = new ArrayList<>();
        for (String etiqueta : obligatorios) {
            Control control = campos.get(etiqueta);
            boolean vacio = estaVacio(control);
            control.getStyleClass().remove("campo-invalido");
            if (vacio) {
                control.getStyleClass().add("campo-invalido");
                faltantes.add(etiqueta);
            }
        }
        return faltantes;
    }

    /** Vacía todos los campos y quita las marcas de validación. */
    public void limpiar() {
        for (Control control : campos.values()) {
            control.getStyleClass().remove("campo-invalido");
            if (control instanceof TextInputControl texto) {
                texto.clear();
            } else if (control instanceof ComboBoxBase<?> combo) {
                combo.setValue(null);
            }
        }
    }

    private static boolean estaVacio(Control control) {
        if (control instanceof TextInputControl texto) {
            return texto.getText() == null || texto.getText().isBlank();
        }
        if (control instanceof ComboBoxBase<?> combo) {
            return combo.getValue() == null;
        }
        return false;
    }
}
