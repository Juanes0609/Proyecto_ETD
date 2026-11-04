package gui.admin;

import gui.VentanaPrincipalSGA;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.Iconos;
import gui.integracion.dto.ConexionDTO;
import gui.integracion.dto.NodoEdificioDTO;
import gui.navegacion.VistaBase;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Análisis del grafo del edificio: dibuja nodos y pasillos, permite cerrar o
 * reabrir un pasillo con un clic, resalta los aparta-suites inalcanzables desde
 * recepción (BFS) y agrupa los aparta-suites por bloque.
 */
public class VistaEdificio extends VistaBase {

    private static final double MARGEN = 30;
    private static final double ESCALA = 0.9;
    private static final double ANCHO_NODO = 92;
    private static final double ALTO_NODO = 30;

    private final Pane mapa = new Pane();
    private final VBox panelCerrados = new VBox(8);
    private final VBox panelInalcanzables = new VBox(8);
    private final VBox panelBloque = new VBox(8);
    private final ComboBox<String> bloque = new ComboBox<>();
    private Map<String, NodoEdificioDTO> nodos = new HashMap<>();
    private List<ConexionDTO> conexiones = List.of();
    private Set<String> inalcanzables = new HashSet<>();

    public VistaEdificio(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        mapa.getStyleClass().add("mapa");
        mapa.setPrefSize(620, 410);
        mapa.setMinSize(620, 410);
        mapa.setMaxSize(620, 410);
        bloque.getItems().setAll("Todos los bloques", "Bloque A", "Bloque B", "Bloque C");
        bloque.setValue("Todos los bloques");
        bloque.valueProperty().addListener((o, a, b) -> pintar());

        VBox tarjetaMapa = FabricaComponentesUI.crearTarjeta(
                new HBox(FabricaComponentesUI.crearEncabezado("Mapa del edificio",
                        "Haz clic en un pasillo para cerrarlo o reabrirlo."), FabricaComponentesUI.crearEspaciador(),
                        FabricaComponentesUI.crearCampoEtiquetado("Agrupar", bloque)), mapa);
        tarjetaMapa.setSpacing(14);
        tarjetaMapa.setMaxWidth(Region.USE_PREF_SIZE);

        VBox lateral = new VBox(16,
                FabricaComponentesUI.crearTarjeta(FabricaComponentesUI.crearEncabezado("Inalcanzables desde recepción",
                        "Aparta-suites sin camino con los pasillos actuales."), panelInalcanzables),
                FabricaComponentesUI.crearTarjeta(FabricaComponentesUI.crearEncabezado("Pasillos cerrados", null),
                        panelCerrados),
                FabricaComponentesUI.crearTarjeta(FabricaComponentesUI.crearEncabezado("Aparta-suites por bloque",
                        "Agrupación con recorrido del grafo."), panelBloque));
        lateral.setPrefWidth(300);
        lateral.setMinWidth(260);
        HBox.setHgrow(lateral, Priority.ALWAYS);

        HBox columnas = new HBox(20, tarjetaMapa, lateral);
        ScrollPane scroll = new ScrollPane(columnas);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        return scroll;
    }

    @Override
    public void alMostrar() {
        notificador().ejecutarConManejo(() -> {
            nodos = new HashMap<>();
            conector().nodosEdificio().forEach(n -> nodos.put(n.codigo(), n));
            conexiones = conector().conexionesEdificio();
            inalcanzables = new HashSet<>(conector().apartaSuitesInalcanzables());
            pintar();
        }, null);
    }

    private void alternar(ConexionDTO c) {
        String origen = nodos.get(c.origen()).nombre();
        String destino = nodos.get(c.destino()).nombre();
        if (notificador().ejecutarConManejo(() -> conector().alternarPasillo(c.origen(), c.destino()),
                (c.habilitada() ? "Pasillo cerrado: " : "Pasillo reabierto: ") + origen + " – " + destino + ".")) {
            alMostrar();
        }
    }

    private void pintar() {
        mapa.getChildren().clear();
        String filtroBloque = bloque.getValue().startsWith("Bloque") ? bloque.getValue().substring(7) : null;

        for (ConexionDTO c : conexiones) {
            NodoEdificioDTO a = nodos.get(c.origen());
            NodoEdificioDTO b = nodos.get(c.destino());
            Line linea = new Line(x(a), y(a), x(b), y(b));
            linea.getStyleClass().add("mapa-pasillo");
            if (!c.habilitada()) {
                linea.getStyleClass().add("mapa-pasillo-cerrado");
            }
            Line zona = new Line(x(a), y(a), x(b), y(b)); // área de clic más ancha que el trazo
            zona.getStyleClass().add("mapa-zona-clic");
            String texto = (c.habilitada() ? "Cerrar" : "Reabrir") + " pasillo " + a.nombre() + " – " + b.nombre()
                    + " (" + Math.round(c.metros()) + " m)";
            Tooltip.install(zona, new Tooltip(texto));
            zona.setOnMouseClicked(e -> alternar(c));
            Text peso = new Text((x(a) + x(b)) / 2 + 3, (y(a) + y(b)) / 2 - 5, Math.round(c.metros()) + " m");
            peso.getStyleClass().add("mapa-peso");
            peso.setMouseTransparent(true);
            mapa.getChildren().addAll(linea, zona);
            if (Math.hypot(x(a) - x(b), y(a) - y(b)) > 70) {
                mapa.getChildren().add(peso); // en tramos cortos el peso queda en el tooltip
            }
        }

        for (NodoEdificioDTO n : nodos.values()) {
            boolean apartamento = "APARTAMENTO".equals(n.tipo());
            Label etiqueta = new Label(apartamento ? n.codigo() : n.nombre());
            HBox caja = new HBox(4);
            caja.getStyleClass().add("mapa-nodo");
            if (!apartamento) {
                caja.getStyleClass().add("RECEPCION".equals(n.tipo()) || "CUARTO_ASEO".equals(n.tipo())
                        ? "mapa-nodo-servicio" : "mapa-nodo-paso");
            }
            if (inalcanzables.contains(n.codigo())) {
                caja.getStyleClass().add("mapa-nodo-inalcanzable");
                caja.getChildren().add(Iconos.crear(Iconos.Icono.ALERTA, 12));
            }
            if (filtroBloque != null && apartamento && !filtroBloque.equals(n.bloque())) {
                caja.getStyleClass().add("mapa-nodo-atenuado");
            }
            caja.getChildren().add(etiqueta);
            caja.setAlignment(Pos.CENTER);
            caja.setPrefSize(ANCHO_NODO, ALTO_NODO);
            caja.setLayoutX(x(n) - ANCHO_NODO / 2);
            caja.setLayoutY(y(n) - ALTO_NODO / 2);
            Tooltip.install(caja, new Tooltip(n.nombre()));
            mapa.getChildren().add(caja);
        }
        pintarPaneles(filtroBloque);
    }

    private void pintarPaneles(String filtroBloque) {
        panelInalcanzables.getChildren().clear();
        if (inalcanzables.isEmpty()) {
            panelInalcanzables.getChildren().add(FabricaComponentesUI.crearTextoConIcono(Iconos.Icono.CHECK,
                    "Todos los aparta-suites son alcanzables.", "texto-exito"));
        }
        inalcanzables.stream().sorted().forEach(c -> panelInalcanzables.getChildren().add(
                FabricaComponentesUI.crearTextoConIcono(Iconos.Icono.ALERTA, nodos.get(c).nombre(), "texto-error")));

        panelCerrados.getChildren().clear();
        List<ConexionDTO> cerrados = conexiones.stream().filter(c -> !c.habilitada()).toList();
        if (cerrados.isEmpty()) {
            panelCerrados.getChildren().add(FabricaComponentesUI.crearTextoSecundario("Ninguno. Todos los pasillos están abiertos."));
        }
        for (ConexionDTO c : cerrados) {
            HBox fila = new HBox(8, new Label(nodos.get(c.origen()).nombre() + " – " + nodos.get(c.destino()).nombre()),
                    FabricaComponentesUI.crearEspaciador(), FabricaComponentesUI.crearEnlace("Reabrir", () -> alternar(c)));
            fila.setAlignment(Pos.CENTER_LEFT);
            panelCerrados.getChildren().add(fila);
        }

        panelBloque.getChildren().clear();
        for (String b : List.of("A", "B", "C")) {
            if (filtroBloque != null && !filtroBloque.equals(b)) {
                continue;
            }
            List<String> codigos = nodos.values().stream()
                    .filter(n -> "APARTAMENTO".equals(n.tipo()) && b.equals(n.bloque()))
                    .map(NodoEdificioDTO::codigo).sorted().toList();
            panelBloque.getChildren().add(new VBox(2, FabricaComponentesUI.crearTexto("Bloque " + b, "campo-label"),
                    FabricaComponentesUI.crearTextoSecundario(String.join(" · ", codigos))));
        }
    }

    private static double x(NodoEdificioDTO n) {
        return n.x() * ESCALA + MARGEN;
    }

    private static double y(NodoEdificioDTO n) {
        return n.y() * ESCALA + MARGEN - 8;
    }
}
