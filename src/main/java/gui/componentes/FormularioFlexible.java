package gui.componentes;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Formulario declarativo con el patrón de campo del SGA:
 * <ul>
 *     <li><b>Label arriba</b>, siempre visible. Los campos opcionales lo indican con
 *         "(opcional)"; los demás son obligatorios.</li>
 *     <li><b>Placeholder</b> con un ejemplo o el formato esperado ("dd/mm/aaaa").</li>
 *     <li><b>Ayuda abajo</b> cuando hace falta; se reemplaza por el mensaje de error
 *         si la validación falla.</li>
 *     <li><b>Campo activo</b>: cambia el borde (CSS {@code :focused}) y su label se
 *         pinta del color primario (pseudo-clase {@code :activo}).</li>
 *     <li><b>Validación inline</b> al salir de un campo que ya se tocó, no solo al enviar.</li>
 * </ul>
 * Agregar o quitar un dato del formulario es agregar o borrar una línea.
 */
public class FormularioFlexible extends VBox {

    private static final PseudoClass ACTIVO = PseudoClass.getPseudoClass("activo");
    private static final PseudoClass INVALIDO = PseudoClass.getPseudoClass("invalido");

    private final class Campo {
        final Control control;
        final Label etiqueta;
        final HBox mensaje = new HBox(6);
        final String ayuda;
        final boolean obligatorio;
        Function<Control, String> validador;
        boolean tocado;

        Campo(Control control, Label etiqueta, String ayuda, boolean obligatorio) {
            this.control = control;
            this.etiqueta = etiqueta;
            this.ayuda = ayuda;
            this.obligatorio = obligatorio;
            mensaje.getStyleClass().add("campo-mensaje");
            mensaje.setAlignment(Pos.CENTER_LEFT);
            mostrarAyuda();
        }

        /** @return mensaje de error, o {@code null} si el campo es válido */
        String error() {
            if (obligatorio && estaVacio(control)) {
                return "Este dato es obligatorio.";
            }
            return validador == null || estaVacio(control) ? null : validador.apply(control);
        }

        boolean validar() {
            String error = error();
            control.pseudoClassStateChanged(INVALIDO, error != null);
            etiqueta.pseudoClassStateChanged(INVALIDO, error != null);
            if (error == null) {
                mostrarAyuda();
            } else {
                pintarMensaje(Iconos.Icono.ALERTA, error, "campo-error");
            }
            return error == null;
        }

        void mostrarAyuda() {
            if (ayuda == null || ayuda.isBlank()) {
                mensaje.getChildren().clear();
                mensaje.setVisible(false);
                mensaje.setManaged(false);
            } else {
                pintarMensaje(Iconos.Icono.INFO, ayuda, "campo-ayuda");
            }
        }

        void pintarMensaje(Iconos.Icono icono, String texto, String clase) {
            Label lbl = new Label(texto);
            lbl.setWrapText(true);
            mensaje.getChildren().setAll(Iconos.crear(icono, 14), lbl);
            mensaje.getStyleClass().setAll("campo-mensaje", clase);
            mensaje.setVisible(true);
            mensaje.setManaged(true);
        }
    }

    private final Map<String, Campo> campos = new LinkedHashMap<>();

    public FormularioFlexible() {
        getStyleClass().add("formulario");
    }

    /** Campo obligatorio. */
    public FormularioFlexible agregarCampo(String etiqueta, Control control, String ayuda) {
        return agregar(etiqueta, control, ayuda, true);
    }

    /** Campo opcional: su label muestra "(opcional)". */
    public FormularioFlexible agregarCampoOpcional(String etiqueta, Control control, String ayuda) {
        return agregar(etiqueta, control, ayuda, false);
    }

    /**
     * Campo cuya presentación es un nodo compuesto (por ejemplo, contraseña con botón
     * para mostrarla). {@code control} es el que se valida y lee.
     */
    public FormularioFlexible agregarCampoCompuesto(String etiqueta, Control control, Node visual, String ayuda) {
        return agregar(etiqueta, control, visual, ayuda, true);
    }

    /** Hace que otro control (alternativo al principal) también active el label del campo. */
    public FormularioFlexible vincularFoco(String etiqueta, Control otro) {
        Campo campo = campos.get(etiqueta);
        otro.focusedProperty().addListener((obs, antes, enfocado) ->
                campo.etiqueta.pseudoClassStateChanged(ACTIVO, enfocado));
        return this;
    }

    /** Dato informativo que el usuario no puede editar (por ejemplo, datos del aparta-suite). */
    public FormularioFlexible agregarDatoSoloLectura(String etiqueta, String valor) {
        Label lblEtiqueta = new Label(etiqueta);
        lblEtiqueta.getStyleClass().add("campo-label");
        Label lblValor = new Label(valor);
        lblValor.getStyleClass().add("dato-solo-lectura");
        lblValor.setWrapText(true);
        lblValor.setMaxWidth(Double.MAX_VALUE);
        getChildren().add(new VBox(6, lblEtiqueta, lblValor));
        return this;
    }

    /** Regla adicional para un campo: devuelve el mensaje de error o {@code null}. */
    public FormularioFlexible conValidacion(String etiqueta, Function<Control, String> validador) {
        campos.get(etiqueta).validador = validador;
        return this;
    }

    /** Título que agrupa los campos siguientes. */
    public FormularioFlexible agregarSeccion(String titulo) {
        Label lbl = new Label(titulo);
        lbl.getStyleClass().add("formulario-seccion");
        getChildren().add(lbl);
        return this;
    }

    /** Cualquier nodo adicional (barra de acciones, nota...). */
    public FormularioFlexible agregarNodo(Node nodo) {
        getChildren().add(nodo);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <C extends Control> C obtenerControl(String etiqueta) {
        return (C) campos.get(etiqueta).control;
    }

    /** Texto de un campo de texto, sin espacios sobrantes. */
    public String texto(String etiqueta) {
        Control control = campos.get(etiqueta).control;
        return control instanceof TextInputControl t ? t.getText().trim() : "";
    }

    /** Valida todos los campos, marca los errores y enfoca el primero. */
    public boolean validar() {
        Campo primeroConError = null;
        for (Campo campo : campos.values()) {
            campo.tocado = true;
            if (!campo.validar() && primeroConError == null) {
                primeroConError = campo;
            }
        }
        if (primeroConError != null) {
            primeroConError.control.requestFocus();
        }
        return primeroConError == null;
    }

    /** Vacía los campos y quita las marcas de validación. */
    public void limpiar() {
        for (Campo campo : campos.values()) {
            if (campo.control instanceof TextInputControl texto) {
                texto.clear();
            } else if (campo.control instanceof ComboBoxBase<?> combo) {
                combo.setValue(null);
            }
            campo.tocado = false;
            campo.control.pseudoClassStateChanged(INVALIDO, false);
            campo.etiqueta.pseudoClassStateChanged(INVALIDO, false);
            campo.mostrarAyuda();
        }
    }

    private FormularioFlexible agregar(String etiqueta, Control control, String ayuda, boolean obligatorio) {
        return agregar(etiqueta, control, control, ayuda, obligatorio);
    }

    private FormularioFlexible agregar(String etiqueta, Control control, Node visual, String ayuda,
                                       boolean obligatorio) {
        Label lblEtiqueta = new Label(etiqueta);
        lblEtiqueta.getStyleClass().add("campo-label");
        lblEtiqueta.setLabelFor(control);
        HBox filaEtiqueta = new HBox(4, lblEtiqueta);
        filaEtiqueta.setAlignment(Pos.BASELINE_LEFT);
        if (!obligatorio) {
            Label opcional = new Label("(opcional)");
            opcional.getStyleClass().add("campo-opcional");
            filaEtiqueta.getChildren().add(opcional);
        }
        control.setMaxWidth(Double.MAX_VALUE);
        control.setAccessibleText(etiqueta);

        Campo campo = new Campo(control, lblEtiqueta, ayuda, obligatorio);
        control.focusedProperty().addListener((obs, antes, enfocado) -> {
            lblEtiqueta.pseudoClassStateChanged(ACTIVO, enfocado);
            if (enfocado) {
                campo.tocado = true;
            } else if (campo.tocado) {
                campo.validar();
            }
        });

        VBox bloque = new VBox(6, filaEtiqueta, visual, campo.mensaje);
        bloque.getStyleClass().add("campo");
        getChildren().add(bloque);
        campos.put(etiqueta, campo);
        return this;
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
