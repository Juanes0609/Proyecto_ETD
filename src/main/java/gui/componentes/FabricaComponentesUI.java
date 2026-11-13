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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
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
        lblTitulo.setWrapText(true);
        VBox caja = new VBox(2, lblTitulo);
        if (descripcion != null && !descripcion.isBlank()) {
            caja.getChildren().add(crearTextoSecundario(descripcion));
        }
        return caja;
    }

    /** Título grande de página con subtítulo opcional. */
    public static VBox crearTituloPagina(String titulo, String subtitulo) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("titulo-seccion");
        lblTitulo.setWrapText(true);
        VBox caja = new VBox(2, lblTitulo);
        if (subtitulo != null && !subtitulo.isBlank()) {
            caja.getChildren().add(crearTextoSecundario(subtitulo));
        }
        return caja;
    }

    public static Label crearTexto(String texto, String... clases) {
        Label lbl = new Label(texto);
        lbl.getStyleClass().addAll(clases);
        lbl.setWrapText(true);
        // Un texto que se ajusta pide el alto de todas sus líneas; sin esto algunos
        // contenedores le dan una sola línea y lo cortan con "…".
        lbl.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        return lbl;
    }

    public static Label crearTextoSecundario(String texto) {
        return crearTexto(texto, "texto-secundario");
    }

    /** Texto con icono delante (por ejemplo, "✓ Cancelación gratuita"). */
    public static HBox crearTextoConIcono(Iconos.Icono icono, String texto, String clase) {
        Label lbl = new Label(texto);
        lbl.setWrapText(true);
        lbl.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        HBox fila = new HBox(6, Iconos.crear(icono, 15), lbl);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add(clase);
        lbl.getStyleClass().add(clase);
        return fila;
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

    public static Button crearEnlace(String texto, Runnable accion) {
        return crearBoton(texto, "enlace", accion);
    }

    /** Botón de solo icono; {@code descripcion} es el texto accesible y el tooltip. */
    public static Button crearBotonIcono(Iconos.Icono icono, String descripcion, Runnable accion) {
        Button boton = crearBoton(null, "boton-icono", accion);
        boton.setGraphic(Iconos.crear(icono, 18));
        boton.setAccessibleText(descripcion);
        boton.setTooltip(new javafx.scene.control.Tooltip(descripcion));
        return boton;
    }

    public static Button conIcono(Button boton, Iconos.Icono icono) {
        boton.setGraphic(Iconos.crear(icono, 16));
        boton.setGraphicTextGap(8);
        return boton;
    }

    private static Button crearBoton(String texto, String claseEstilo, Runnable accion) {
        Button boton = new Button(texto);
        boton.getStyleClass().add(claseEstilo);
        // Un botón nunca se recorta con "…": siempre mide lo que su texto necesita.
        boton.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
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

    /** Selector de fecha con formato dd/mm/aaaa (también acepta escritura manual). */
    public static javafx.scene.control.DatePicker crearSelectorFecha(String sugerencia) {
        javafx.scene.control.DatePicker selector = new javafx.scene.control.DatePicker();
        selector.setPromptText(sugerencia == null ? "dd/mm/aaaa" : sugerencia);
        java.time.format.DateTimeFormatter formato = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        selector.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(java.time.LocalDate fecha) {
                return fecha == null ? "" : formato.format(fecha);
            }

            @Override
            public java.time.LocalDate fromString(String texto) {
                try {
                    return texto == null || texto.isBlank() ? null : java.time.LocalDate.parse(texto.trim(), formato);
                } catch (java.time.format.DateTimeParseException e) {
                    return null;
                }
            }
        });
        // Al salir del campo, confirma lo escrito a mano.
        selector.focusedProperty().addListener((obs, antes, enfocado) -> {
            if (!enfocado) {
                selector.setValue(selector.getConverter().fromString(selector.getEditor().getText()));
            }
        });
        return selector;
    }

    /** Control con su etiqueta encima, para barras de filtros fuera de un {@link FormularioFlexible}. */
    public static VBox crearCampoEtiquetado(String etiqueta, Control control) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("campo-label");
        lbl.setLabelFor(control);
        control.setAccessibleText(etiqueta);
        return new VBox(6, lbl, control);
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
        barra.setAlignment(Pos.BOTTOM_LEFT);
        return barra;
    }

    public static Region crearEspaciador() {
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        VBox.setVgrow(espacio, Priority.ALWAYS);
        return espacio;
    }

    public static Region crearSeparador() {
        Region linea = new Region();
        linea.getStyleClass().add("separador");
        return linea;
    }

    public static VBox crearTarjeta(Node... contenido) {
        VBox tarjeta = new VBox(contenido);
        tarjeta.getStyleClass().add("tarjeta");
        return tarjeta;
    }

    /** Tarjeta de indicador numérico; el {@link Label} del valor se pasa para actualizarlo. */
    public static VBox crearTarjetaIndicador(String etiqueta, Label valor) {
        valor.getStyleClass().add("indicador-valor");
        Label lblEtiqueta = new Label(etiqueta);
        lblEtiqueta.getStyleClass().add("indicador-etiqueta");
        lblEtiqueta.setWrapText(true);
        VBox tarjeta = crearTarjeta(valor, lblEtiqueta);
        tarjeta.setMinWidth(170);
        return tarjeta;
    }

    /** Mensaje centrado para listas vacías, con una acción de destino opcional. */
    public static VBox crearEstadoVacio(String titulo, String descripcion) {
        return crearEstadoVacio(null, titulo, descripcion, null);
    }

    public static VBox crearEstadoVacio(Iconos.Icono icono, String titulo, String descripcion, Button accion) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("estado-vacio-titulo");
        lblTitulo.setWrapText(true);
        Label lblDescripcion = crearTextoSecundario(descripcion);
        lblDescripcion.setMaxWidth(420);
        lblDescripcion.setAlignment(Pos.CENTER);
        lblDescripcion.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        VBox caja = new VBox();
        if (icono != null) {
            caja.getChildren().add(Iconos.crear(icono, 36));
        }
        caja.getChildren().addAll(lblTitulo, lblDescripcion);
        if (accion != null) {
            VBox.setMargin(accion, new javafx.geometry.Insets(8, 0, 0, 0));
            caja.getChildren().add(accion);
        }
        caja.getStyleClass().add("estado-vacio");
        return caja;
    }

    /** Mensaje de error dentro de una tarjeta o formulario (no un banner global). */
    public static HBox crearErrorInline(String mensaje) {
        Label lbl = new Label(mensaje);
        lbl.setWrapText(true);
        HBox caja = new HBox(8, Iconos.crear(Iconos.Icono.ALERTA, 16), lbl);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.getStyleClass().add("error-inline");
        return caja;
    }

    /** Píldora con texto y botón para quitarla (filtros activos). */
    public static HBox crearChip(String texto, Runnable alQuitar) {
        Label lbl = new Label(texto);
        HBox chip = new HBox(lbl);
        chip.getStyleClass().add("chip");
        if (alQuitar != null) {
            Button quitar = crearBotonIcono(Iconos.Icono.CERRAR, "Quitar filtro " + texto, alQuitar);
            quitar.setGraphic(Iconos.crear(Iconos.Icono.CERRAR, 12));
            chip.getChildren().add(quitar);
        }
        return chip;
    }

    public static Label crearEtiquetaCaracteristica(String texto) {
        Label lbl = new Label(texto);
        lbl.getStyleClass().add("etiqueta-caracteristica");
        return lbl;
    }

    /**
     * Control segmentado (dos a cuatro opciones excluyentes dentro de una misma vista).
     * La opción activa se distingue por fondo, peso del texto y sombra.
     */
    public static HBox crearSegmentado(List<String> opciones, int seleccionInicial, Consumer<Integer> alCambiar) {
        HBox contenedor = new HBox();
        contenedor.getStyleClass().add("segmentado");
        contenedor.setMaxWidth(Region.USE_PREF_SIZE);
        ToggleGroup grupo = new ToggleGroup();
        for (int i = 0; i < opciones.size(); i++) {
            int indice = i;
            ToggleButton segmento = new ToggleButton(opciones.get(i));
            segmento.getStyleClass().add("segmento");
            segmento.setToggleGroup(grupo);
            segmento.setSelected(i == seleccionInicial);
            segmento.setOnAction(e -> {
                segmento.setSelected(true);
                alCambiar.accept(indice);
            });
            contenedor.getChildren().add(segmento);
        }
        return contenedor;
    }

    // ---------- Estados ----------

    /**
     * Etiqueta coloreada para un estado ({@code EstadoOperativo}, {@code EstadoReserva}...).
     * Siempre lleva el texto del estado, de modo que no depende solo del color.
     */
    public static Label crearBadgeEstado(String estado) {
        Label badge = new Label(Formato.estado(estado));
        badge.getStyleClass().addAll("badge", claseBadge(estado));
        badge.setMinWidth(Region.USE_PREF_SIZE);
        return badge;
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

    /** Columna que muestra un badge de estado en lugar de texto plano. */
    public static <T> TableColumn<T, String> agregarColumnaEstado(TableView<T> tabla, String titulo,
                                                                  Function<T, String> extractor) {
        TableColumn<T, String> columna = agregarColumna(tabla, titulo, extractor);
        columna.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean vacio) {
                super.updateItem(estado, vacio);
                setText(null);
                setGraphic(vacio || estado == null ? null : crearBadgeEstado(estado));
            }
        });
        return columna;
    }
}
