package gui.componentes;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Function;

/**
 * Fábrica de controles con el estilo del SGA. Las vistas nunca asignan
 * colores ni tamaños: piden el componente aquí y el CSS hace el resto.
 */
public final class FabricaComponentesUI {

    private FabricaComponentesUI() {
    }

    // ---------- Textos ----------

    public static VBox crearEncabezado(String titulo, String descripcion) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("encabezado-vista");
        VBox caja = new VBox(2, lblTitulo);
        if (descripcion != null && !descripcion.isBlank()) {
            Label lblDescripcion = new Label(descripcion);
            lblDescripcion.getStyleClass().add("texto-secundario");
            lblDescripcion.setWrapText(true);
            caja.getChildren().add(lblDescripcion);
        }
        return caja;
    }

    public static Label crearTextoSecundario(String texto) {
        Label lbl = new Label(texto);
        lbl.getStyleClass().add("texto-secundario");
        lbl.setWrapText(true);
        return lbl;
    }

    // ---------- Botones ----------

    public static Button crearBotonPrimario(String texto, Runnable accion) {
        return crearBoton(texto, "boton-primario", accion);
    }

    public static Button crearBotonSecundario(String texto, Runnable accion) {
        return crearBoton(texto, "boton-secundario", accion);
    }

    public static Button crearBotonPeligro(String texto, Runnable accion) {
        return crearBoton(texto, "boton-peligro", accion);
    }

    private static Button crearBoton(String texto, String claseEstilo, Runnable accion) {
        Button boton = new Button(texto);
        boton.getStyleClass().add(claseEstilo);
        if (accion != null) {
            boton.setOnAction(e -> accion.run());
        }
        return boton;
    }

    // ---------- Campos ----------

    public static TextField crearCampoTexto(String sugerencia) {
        TextField campo = new TextField();
        campo.setPromptText(sugerencia);
        return campo;
    }

    public static TextArea crearAreaTexto(String sugerencia) {
        TextArea area = new TextArea();
        area.setPromptText(sugerencia);
        area.setPrefRowCount(3);
        area.setWrapText(true);
        return area;
    }

    /** Control con su etiqueta encima, para barras de filtros fuera de un {@link FormularioFlexible}. */
    public static VBox crearCampoEtiquetado(String etiqueta, Control control) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("formulario-etiqueta");
        lbl.setLabelFor(control);
        control.setAccessibleText(etiqueta);
        return new VBox(4, lbl, control);
    }

    // ---------- Contenedores ----------

    /** Fila horizontal de acciones; los nodos {@link #crearEspaciador()} empujan a la derecha. */
    public static HBox crearBarraAcciones(Node... nodos) {
        HBox barra = new HBox(nodos);
        barra.getStyleClass().add("barra-acciones");
        return barra;
    }

    /** Fila horizontal de filtros (campos de búsqueda, combos, fechas). */
    public static HBox crearBarraFiltros(Node... nodos) {
        HBox barra = new HBox(nodos);
        barra.getStyleClass().add("barra-filtros");
        return barra;
    }

    public static Region crearEspaciador() {
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        VBox.setVgrow(espacio, Priority.ALWAYS);
        return espacio;
    }

    public static VBox crearTarjeta(Node... contenido) {
        VBox tarjeta = new VBox(contenido);
        tarjeta.getStyleClass().add("tarjeta");
        return tarjeta;
    }

    /** Tarjeta de indicador numérico; el {@link Label} del valor se devuelve dentro para actualizarlo. */
    public static VBox crearTarjetaIndicador(String etiqueta, Label valor) {
        valor.getStyleClass().add("indicador-valor");
        Label lblEtiqueta = new Label(etiqueta);
        lblEtiqueta.getStyleClass().add("indicador-etiqueta");
        VBox tarjeta = crearTarjeta(valor, lblEtiqueta);
        tarjeta.setMinWidth(170);
        return tarjeta;
    }

    /** Mensaje centrado para listas o tablas sin elementos. */
    public static VBox crearEstadoVacio(String titulo, String descripcion) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("estado-vacio-titulo");
        Label lblDescripcion = crearTextoSecundario(descripcion);
        lblDescripcion.setMaxWidth(420);
        lblDescripcion.setAlignment(Pos.CENTER);
        VBox caja = new VBox(lblTitulo, lblDescripcion);
        caja.getStyleClass().add("estado-vacio");
        return caja;
    }

    // ---------- Estados ----------

    /**
     * Etiqueta coloreada para un estado ({@code EstadoOperativo}, {@code EstadoReserva}...).
     * La clase CSS se deriva del nombre: "FUERA_DE_SERVICIO" → {@code badge-fuera-de-servicio}.
     */
    public static Label crearBadgeEstado(String estado) {
        Label badge = new Label(formatearEstado(estado));
        badge.getStyleClass().addAll("badge", claseBadge(estado));
        return badge;
    }

    /** Convierte un nombre de enum en texto legible: "FUERA_DE_SERVICIO" → "Fuera de servicio". */
    public static String formatearEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "—";
        }
        String texto = estado.replace('_', ' ').toLowerCase(Locale.ROOT)
                .replace("preparacion", "preparación"); // los enums no llevan tildes
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static String claseBadge(String estado) {
        if (estado == null || estado.isBlank()) {
            return "badge-neutro";
        }
        String sinTildes = Normalizer.normalize(estado, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return "badge-" + sinTildes.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s_]+", "-");
    }

    // ---------- Tablas ----------

    /** Tabla con columnas que se reparten el ancho y un estado vacío propio. */
    public static <T> TableView<T> crearTabla(String tituloVacio, String descripcionVacio) {
        TableView<T> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabla.setPlaceholder(crearEstadoVacio(tituloVacio, descripcionVacio));
        VBox.setVgrow(tabla, Priority.ALWAYS);
        return tabla;
    }

    /** Agrega una columna de texto calculada a partir de cada fila. */
    public static <T> TableColumn<T, String> agregarColumna(TableView<T> tabla, String titulo,
                                                            Function<T, String> extractor) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(celda -> new ReadOnlyStringWrapper(extractor.apply(celda.getValue())));
        tabla.getColumns().add(columna);
        return columna;
    }
}
