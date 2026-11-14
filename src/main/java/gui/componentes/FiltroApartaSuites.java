package gui.componentes;

import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/**
 * Búsqueda de disponibilidad que va sobre las cards: un botón "Filtrar" que
 * despliega los criterios (fechas de la estancia, grupo de personas y precio
 * máximo), los filtros activos como chips que se quitan uno a uno, y a la
 * derecha el selector de orden (precio, capacidad o dormitorios).
 * El resultado se actualiza en vivo, sin botón "Aplicar".
 */
public class FiltroApartaSuites extends VBox {

    private static final double PASO_PRECIO = 5_000;
    /** Criterios de orden: texto visible → clave que entiende el conector. */
    private static final List<String> ORDENES = List.of("Precio", "Capacidad", "Dormitorios");

    private final DatePicker entrada = FabricaComponentesUI.crearSelectorFecha(null);
    private final DatePicker salida = FabricaComponentesUI.crearSelectorFecha(null);
    private final ComboBox<String> orden = FabricaComponentesUI.crearDesplegable(null);
    /** Evita avisar dos veces cuando una fecha corrige a la otra. */
    private boolean ajustandoFechas;

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
        filtrar.setAccessibleText("Mostrar filtros por fechas, personas y precio");

        resultado.getStyleClass().add("texto-secundario");
        chips.setAlignment(Pos.CENTER_LEFT);
        orden.getItems().setAll(ORDENES);
        orden.setValue(ORDENES.getFirst());
        orden.setAccessibleText("Ordenar aparta-suites por");
        orden.valueProperty().addListener((obs, antes, ahora) -> alCambiar.run());
        Label lblOrden = FabricaComponentesUI.crearTexto("Ordenar por", "texto-secundario");
        lblOrden.setLabelFor(orden);
        lblOrden.setMinWidth(Region.USE_PREF_SIZE);
        resultado.setMinWidth(Region.USE_PREF_SIZE);
        HBox fila = new HBox(10, filtrar, chips, FabricaComponentesUI.crearEspaciador(), resultado, lblOrden, orden);
        fila.setAlignment(Pos.CENTER_LEFT);

        entrada.setPrefWidth(150);
        salida.setPrefWidth(150);
        entrada.setAccessibleText("Fecha de entrada");
        salida.setAccessibleText("Fecha de salida");
        // La salida siempre queda después de la entrada: si no, se corrige sola a la noche siguiente.
        entrada.valueProperty().addListener((obs, antes, ahora) -> {
            if (ahora != null && (salida.getValue() == null || !salida.getValue().isAfter(ahora))) {
                fijarFechas(ahora, ahora.plusDays(1));
            }
            cambio();
        });
        salida.valueProperty().addListener((obs, antes, ahora) -> {
            if (ahora != null && entrada.getValue() == null) {
                fijarFechas(ahora.minusDays(1), ahora);
            } else if (ahora != null && !ahora.isAfter(entrada.getValue())) {
                fijarFechas(entrada.getValue(), entrada.getValue().plusDays(1));
            }
            cambio();
        });
        HBox controlFechas = new HBox(8, entrada, FabricaComponentesUI.crearTexto("a", "texto-secundario"), salida);
        controlFechas.setAlignment(Pos.CENTER_LEFT);
        VBox campoFechas = campo("Fechas de la estancia", controlFechas,
                "Solo se muestran los que están libres todas esas noches.");

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

        HBox criterios = new HBox(32, campoFechas, campoPersonas, campoPrecio, FabricaComponentesUI.crearEspaciador(),
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

    /** Entrada de la estancia buscada, o {@code null} si no se filtra por fechas. */
    public LocalDate entrada() {
        return salida.getValue() == null ? null : entrada.getValue();
    }

    public LocalDate salida() {
        return entrada.getValue() == null ? null : salida.getValue();
    }

    /** PRECIO, CAPACIDAD o DORMITORIOS. */
    public String orden() {
        return orden.getValue() == null ? "PRECIO" : orden.getValue().toUpperCase(java.util.Locale.ROOT);
    }

    /** {@code true} si la tarifa vigente entra en el precio máximo elegido. */
    public boolean coincidePrecio(double tarifaVigente) {
        return tarifaVigente <= precioMaximo() + 0.5;
    }

    private void fijarFechas(LocalDate nuevaEntrada, LocalDate nuevaSalida) {
        if (ajustandoFechas) {
            return;
        }
        ajustandoFechas = true;
        entrada.setValue(nuevaEntrada);
        salida.setValue(nuevaSalida);
        ajustandoFechas = false;
    }

    public void setResultado(int cantidad) {
        resultado.setText(Formato.plural(cantidad, "aparta-suite", "aparta-suites"));
    }

    public void limpiar() {
        personas.getValueFactory().setValue(1);
        precio.setValue(precioTope);
        fijarFechas(null, null);
        cambio();
    }

    private void cambio() {
        if (ajustandoFechas) {
            return; // una fecha está corrigiendo a la otra: se avisa una sola vez al terminar
        }
        actualizarChips();
        alCambiar.run();
    }

    private void actualizarChips() {
        chips.getChildren().clear();
        if (entrada() != null) {
            chips.getChildren().add(FabricaComponentesUI.crearChip(Formato.rango(entrada(), salida()), () -> {
                fijarFechas(null, null);
                cambio();
            }));
        }
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
