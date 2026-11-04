package gui.componentes;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Filtro discreto que va arriba a la izquierda de las cards: un botón "Filtrar"
 * que despliega dos criterios (grupo de personas y precio máximo) y muestra
 * los filtros activos como chips que se pueden quitar uno a uno.
 * El resultado se actualiza en vivo, sin botón "Aplicar".
 */
public class FiltroApartaSuites extends VBox {

    private static final double PASO_PRECIO = 5_000;

    private final Spinner<Integer> personas;
    private final Slider precio;
    private final double precioTope;
    private final HBox chips = new HBox(8);
    private final Label resultado = new Label();
    private final Runnable alCambiar;

    public FiltroApartaSuites(int capacidadMaxima, double precioMinimo, double precioMaximo, Runnable alCambiar) {
        this.alCambiar = alCambiar;
        this.precioTope = Math.ceil(precioMaximo / PASO_PRECIO) * PASO_PRECIO;
        double piso = Math.floor(precioMinimo / PASO_PRECIO) * PASO_PRECIO;
        setSpacing(12);

        ToggleButton filtrar = new ToggleButton("Filtrar");
        filtrar.setGraphic(Iconos.crear(Iconos.Icono.FILTRO, 16));
        filtrar.getStyleClass().add("boton-sutil");
        filtrar.setAccessibleText("Mostrar filtros por personas y precio");

        resultado.getStyleClass().add("texto-secundario");
        chips.setAlignment(Pos.CENTER_LEFT);
        HBox fila = new HBox(10, filtrar, chips, FabricaComponentesUI.crearEspaciador(), resultado);
        fila.setAlignment(Pos.CENTER_LEFT);

        personas = new Spinner<>(1, Math.max(1, capacidadMaxima), 1);
        personas.setEditable(true);
        personas.setPrefWidth(120);
        personas.valueProperty().addListener((obs, antes, ahora) -> cambio());

        precio = new Slider(piso, precioTope, precioTope);
        precio.setBlockIncrement(PASO_PRECIO);
        precio.setMajorTickUnit(PASO_PRECIO);
        precio.setMinorTickCount(0);
        precio.setSnapToTicks(true);
        precio.setPrefWidth(260);
        Label valorPrecio = new Label();
        valorPrecio.getStyleClass().add("encabezado-vista");
        valorPrecio.setMinWidth(Region.USE_PREF_SIZE);
        precio.valueProperty().addListener((obs, antes, ahora) -> {
            valorPrecio.setText("Hasta " + Formato.moneda(ahora.doubleValue()));
            cambio();
        });
        valorPrecio.setText("Hasta " + Formato.moneda(precioTope));

        VBox campoPersonas = campo("Grupo de personas", new HBox(personas),
                "Incluye niños: ocupan cupo, pero los menores de 6 años no pagan.");
        HBox controlPrecio = new HBox(12, precio, valorPrecio);
        controlPrecio.setAlignment(Pos.CENTER_LEFT);
        VBox campoPrecio = campo("Precio por persona / noche", controlPrecio, "Según la temporada vigente.");

        HBox criterios = new HBox(32, campoPersonas, campoPrecio, FabricaComponentesUI.crearEspaciador(),
                FabricaComponentesUI.crearEnlace("Limpiar filtros", this::limpiar));
        criterios.setAlignment(Pos.TOP_LEFT);
        VBox panel = FabricaComponentesUI.crearTarjeta(criterios);
        panel.setVisible(false);
        panel.setManaged(false);
        filtrar.selectedProperty().addListener((obs, antes, abierto) -> {
            panel.setVisible(abierto);
            panel.setManaged(abierto);
        });

        getChildren().addAll(fila, panel);
        actualizarChips();
    }

    public int personas() {
        return personas.getValue();
    }

    public double precioMaximo() {
        return precio.getValue();
    }

    /** {@code true} si el aparta-suite cumple los filtros activos. */
    public boolean coincide(int capacidad, double tarifaVigente) {
        return capacidad >= personas() && tarifaVigente <= precioMaximo() + 0.5;
    }

    public void setResultado(int cantidad) {
        resultado.setText(Formato.plural(cantidad, "aparta-suite", "aparta-suites"));
    }

    public void limpiar() {
        personas.getValueFactory().setValue(1);
        precio.setValue(precioTope);
    }

    private void cambio() {
        actualizarChips();
        alCambiar.run();
    }

    private void actualizarChips() {
        chips.getChildren().clear();
        if (personas() > 1) {
            chips.getChildren().add(FabricaComponentesUI.crearChip(Formato.plural(personas(), "persona", "personas"),
                    () -> personas.getValueFactory().setValue(1)));
        }
        if (precioMaximo() < precioTope) {
            chips.getChildren().add(FabricaComponentesUI.crearChip("Hasta " + Formato.moneda(precioMaximo()),
                    () -> precio.setValue(precioTope)));
        }
    }

    private static VBox campo(String etiqueta, javafx.scene.Node control, String ayuda) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("campo-label");
        HBox mensaje = new HBox(6, Iconos.crear(Iconos.Icono.INFO, 14), new Label(ayuda));
        mensaje.getStyleClass().addAll("campo-mensaje", "campo-ayuda");
        mensaje.setAlignment(Pos.CENTER_LEFT);
        VBox caja = new VBox(6, lbl, control, mensaje);
        HBox.setHgrow(caja, Priority.NEVER);
        return caja;
    }
}
