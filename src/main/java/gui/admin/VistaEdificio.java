package gui.admin;

import gui.VentanaPrincipalSGA;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.Iconos;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.ConexionDTO;
import gui.integracion.dto.NodoEdificioDTO;
import gui.integracion.dto.RutaDTO;
import gui.navegacion.VistaBase;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Análisis del grafo del edificio: dibuja nodos y pasillos, permite cerrar o
 * reabrir un pasillo con un clic, resalta los aparta-suites inalcanzables desde
 * recepción (BFS) y agrupa los aparta-suites por bloque.
 * <p>
 * Cada aparta-suite se colorea según su estado operativo (se refresca solo cada
 * pocos segundos) y se puede dibujar sobre el mapa la ruta más corta (Dijkstra)
 * desde el cuarto de aseo hasta un aparta-suite, o el recorrido completo de la
 * cola de preparación.
 */
public class VistaEdificio extends VistaBase {

    private static final double MARGEN = 30;
    private static final double ESCALA = 0.9;
    private static final double ANCHO_NODO = 98;
    private static final double ALTO_NODO = 30;

    private final Pane mapa = new Pane();
    private final VBox panelCerrados = new VBox(8);
    private final VBox panelInalcanzables = new VBox(8);
    private final VBox panelBloque = new VBox(8);
    private final ComboBox<String> bloque = FabricaComponentesUI.crearDesplegable(null);
    private Map<String, NodoEdificioDTO> nodos = new HashMap<>();
    private List<ConexionDTO> conexiones = List.of();
    private Set<String> inalcanzables = new HashSet<>();
    private Map<String, String> estados = new HashMap<>();
    private final ComboBox<String> destinoRuta = FabricaComponentesUI.crearDesplegable("Elige un aparta-suite");
    private final Button recorridoCompleto = FabricaComponentesUI.crearBotonSecundario(
            "Recorrido de la cola", this::mostrarRecorridoCompleto);
    private final VBox panelRuta = new VBox(8);
    /** Opción del desplegable que vuelve al mapa sin ninguna ruta dibujada (vista predeterminada). */
    private static final String SIN_RUTA = "Sin ruta (mapa completo)";
    private List<String> rutaCodigos = List.of();
    private boolean actualizandoDestinos;
    private Timeline refrescoEstados;

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
        destinoRuta.valueProperty().addListener((o, a, b) -> {
            if (!actualizandoDestinos) {
                mostrarRutaA(SIN_RUTA.equals(b) ? null : b);
            }
        });
        // El botón se centra con el campo del desplegable (no con su etiqueta) y mide lo mismo de alto.
        recorridoCompleto.minHeightProperty().bind(destinoRuta.heightProperty());
        recorridoCompleto.prefHeightProperty().bind(destinoRuta.heightProperty());
        HBox filaRuta = new HBox(10, FabricaComponentesUI.crearCampoEtiquetado("Ruta", destinoRuta), recorridoCompleto);
        filaRuta.setAlignment(Pos.BOTTOM_LEFT);
        mostrarSinRuta();

        VBox tarjetaMapa = FabricaComponentesUI.crearTarjeta(
                new HBox(FabricaComponentesUI.crearEncabezado("Mapa del edificio",
                        "Haz clic en un pasillo para cerrarlo o reabrirlo."), FabricaComponentesUI.crearEspaciador(),
                        FabricaComponentesUI.crearCampoEtiquetado("Agrupar", bloque)),
                filaRuta,
                crearLeyenda(), mapa);
        tarjetaMapa.setSpacing(14);
        tarjetaMapa.setMaxWidth(Region.USE_PREF_SIZE);

        VBox lateral = new VBox(16,
                FabricaComponentesUI.crearTarjeta(FabricaComponentesUI.crearEncabezado("Ruta calculada", null), panelRuta),
                FabricaComponentesUI.crearTarjeta(FabricaComponentesUI.crearEncabezado("Inalcanzables desde recepción", null),
                        panelInalcanzables),
                FabricaComponentesUI.crearTarjeta(FabricaComponentesUI.crearEncabezado("Pasillos cerrados", null),
                        panelCerrados),
                FabricaComponentesUI.crearTarjeta(FabricaComponentesUI.crearEncabezado("Aparta-suites por bloque", null), panelBloque));
        lateral.setPrefWidth(300);
        lateral.setMinWidth(260);
        HBox.setHgrow(lateral, Priority.ALWAYS);

        // Refresco "en tiempo real" de los estados mientras el mapa está en pantalla.
        refrescoEstados = new Timeline(new KeyFrame(Duration.seconds(3), e -> refrescarEstados()));
        refrescoEstados.setCycleCount(Timeline.INDEFINITE);
        refrescoEstados.play();

        HBox columnas = new HBox(20, tarjetaMapa, lateral);
        ScrollPane scroll = new ScrollPane(columnas);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        return scroll;
    }

    @Override
    public void alOcultar() {
        if (refrescoEstados != null) {
            refrescoEstados.pause(); // no consume recursos si el mapa no está a la vista
        }
    }

    @Override
    public void alMostrar() {
        if (refrescoEstados != null) {
            refrescoEstados.play();
        }
        notificador().ejecutarConManejo(() -> {
            nodos = new HashMap<>();
            conector().nodosEdificio().forEach(n -> nodos.put(n.codigo(), n));
            conexiones = conector().conexionesEdificio();
            inalcanzables = new HashSet<>(conector().apartaSuitesInalcanzables());
            cargarEstados();
            cargarDestinos();
            pintar();
        }, null);
    }

    private void cargarEstados() {
        Map<String, String> nuevos = new HashMap<>();
        try {
            for (ApartaSuiteDTO a : conector().listarApartaSuites()) {
                nuevos.put(a.codigo(), a.estadoOperativo());
            }
        } catch (UnsupportedOperationException e) {
            // el conector aún no entrega estados: el mapa se dibuja sin colores de estado
        }
        estados = nuevos;
    }

    private void cargarDestinos() {
        String seleccionado = destinoRuta.getValue();
        actualizandoDestinos = true;
        destinoRuta.getItems().setAll(SIN_RUTA);
        nodos.values().stream().filter(n -> "APARTAMENTO".equals(n.tipo()))
                .map(NodoEdificioDTO::codigo).sorted().forEach(destinoRuta.getItems()::add);
        // Sin elección previa (o si mostraba el recorrido de la cola) se conserva lo que había.
        if (seleccionado == null && !recorridoCompleto.isDisabled()) {
            seleccionado = SIN_RUTA;
        }
        destinoRuta.setValue(seleccionado != null && destinoRuta.getItems().contains(seleccionado) ? seleccionado : null);
        actualizandoDestinos = false;
    }

    /**
     * Llamado por el temporizador. Si el mapa ya no está en pantalla (por ejemplo,
     * tras cerrar sesión) se detiene; si nada cambió no redibuja, para no
     * interrumpir el hover ni los tooltips cada 3 s.
     */
    private void refrescarEstados() {
        if (mapa.getScene() == null) {
            refrescoEstados.pause();
            return;
        }
        try {
            List<ConexionDTO> nuevasConexiones = conector().conexionesEdificio();
            Set<String> nuevasInalcanzables = new HashSet<>(conector().apartaSuitesInalcanzables());
            Map<String, String> estadosAntes = estados;
            cargarEstados();
            boolean cambio = !nuevasConexiones.equals(conexiones) || !nuevasInalcanzables.equals(inalcanzables)
                    || !estados.equals(estadosAntes);
            conexiones = nuevasConexiones;
            inalcanzables = nuevasInalcanzables;
            if (cambio) {
                pintar();
            }
        } catch (RuntimeException e) {
            // un fallo de refresco no debe interrumpir la vista; se reintenta en 3 s
        }
    }

    /** Vista predeterminada: el grafo sin rutas y una indicación de cómo ver una. */
    private void mostrarSinRuta() {
        rutaCodigos = List.of();
        marcarRecorrido(false);
        panelRuta.getChildren().setAll(FabricaComponentesUI.crearTextoSecundario(
                "Elige un aparta-suite o el recorrido de la cola para verlo en el mapa."));
    }

    private void mostrarRutaA(String codigo) {
        panelRuta.getChildren().clear();
        marcarRecorrido(false);
        if (codigo == null) {
            mostrarSinRuta();
            pintar();
            return;
        }
        try {
            mostrarRuta(conector().rutaPreparacion(codigo), "Aseo → " + codigo);
        } catch (RuntimeException e) {
            rutaCodigos = List.of();
            panelRuta.getChildren().add(FabricaComponentesUI.crearTexto(
                    "No hay ruta hasta " + codigo + " con los pasillos abiertos.", "texto-error"));
            pintar();
        }
    }

    /**
     * Dibuja el recorrido de toda la cola. Mientras se muestra, el botón queda
     * deshabilitado (no haría nada nuevo) y el desplegable vuelve a su texto guía;
     * elegir otra opción del desplegable lo habilita de nuevo.
     */
    private void mostrarRecorridoCompleto() {
        actualizandoDestinos = true;
        destinoRuta.setValue(null);
        actualizandoDestinos = false;
        if (notificador().ejecutarConManejo(
                () -> mostrarRuta(conector().rutaRecorridoPreparacion(), "Recorrido de la cola de preparación"), null)) {
            marcarRecorrido(true);
        }
    }

    /** Mientras el recorrido está en el mapa el botón lo indica y no se puede volver a pulsar. */
    private void marcarRecorrido(boolean mostrado) {
        recorridoCompleto.setDisable(mostrado);
        recorridoCompleto.setText(mostrado ? "Recorrido en el mapa" : "Recorrido de la cola");
        recorridoCompleto.setGraphic(mostrado ? Iconos.crear(Iconos.Icono.CHECK, 16) : null);
        recorridoCompleto.getStyleClass().remove("boton-activo");
        if (mostrado) {
            recorridoCompleto.getStyleClass().add("boton-activo");
        }
    }

    /** Convierte los nombres de la ruta en códigos de nodo, la dibuja y la lista en el panel. */
    private void mostrarRuta(RutaDTO ruta, String titulo) {
        Map<String, String> nombreACodigo = new HashMap<>();
        nodos.values().forEach(n -> nombreACodigo.put(n.nombre(), n.codigo()));
        List<String> codigos = new ArrayList<>();
        panelRuta.getChildren().setAll(FabricaComponentesUI.crearTexto(titulo, "campo-label"));
        for (int i = 0; i < ruta.pasos().size(); i++) {
            String paso = ruta.pasos().get(i);
            String codigo = nombreACodigo.get(paso);
            if (codigo != null) {
                codigos.add(codigo);
            }
            panelRuta.getChildren().add(new Label((i + 1) + ".  " + paso));
        }
        panelRuta.getChildren().add(FabricaComponentesUI.crearTexto(Math.round(ruta.metros()) + " m en total", "suite-precio"));
        rutaCodigos = codigos;
        pintar();
    }

    private HBox crearLeyenda() {
        HBox leyenda = new HBox(14,
                crearItemLeyenda("Preparado", "mapa-nodo-preparado"),
                crearItemLeyenda("Ocupado", "mapa-nodo-ocupado"),
                crearItemLeyenda("Pendiente / en preparación", "mapa-nodo-pendiente"),
                crearItemLeyenda("Fuera de servicio", "mapa-nodo-fuera"));
        leyenda.setAlignment(Pos.CENTER_LEFT);
        return leyenda;
    }

    private HBox crearItemLeyenda(String texto, String claseEstado) {
        Region muestra = new Region();
        muestra.getStyleClass().addAll("mapa-nodo", claseEstado);
        muestra.setMinSize(18, 18);
        muestra.setPrefSize(18, 18);
        muestra.setMaxSize(18, 18);
        HBox item = new HBox(6, muestra, FabricaComponentesUI.crearTextoSecundario(texto));
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private static String claseDeEstado(String estado) {
        if (estado == null) {
            return null;
        }
        return switch (estado) {
            case "PREPARADO" -> "mapa-nodo-preparado";
            case "OCUPADO" -> "mapa-nodo-ocupado";
            case "PENDIENTE_PREPARACION", "EN_PREPARACION" -> "mapa-nodo-pendiente";
            case "FUERA_DE_SERVICIO" -> "mapa-nodo-fuera";
            default -> null;
        };
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

        for (int i = 0; i + 1 < rutaCodigos.size(); i++) {
            NodoEdificioDTO a = nodos.get(rutaCodigos.get(i));
            NodoEdificioDTO b = nodos.get(rutaCodigos.get(i + 1));
            if (a == null || b == null) {
                continue;
            }
            Line trazo = new Line(x(a), y(a), x(b), y(b));
            trazo.getStyleClass().add("mapa-ruta");
            trazo.setMouseTransparent(true);
            mapa.getChildren().add(trazo);
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
            String claseEstado = apartamento ? claseDeEstado(estados.get(n.codigo())) : null;
            if (claseEstado != null) {
                caja.getStyleClass().add(claseEstado);
            }
            if (rutaCodigos.contains(n.codigo())) {
                caja.getStyleClass().add("mapa-nodo-en-ruta");
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
            String estado = apartamento ? estados.get(n.codigo()) : null;
            Tooltip.install(caja, new Tooltip(estado == null ? n.nombre() : n.nombre() + " · " + estado));
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
