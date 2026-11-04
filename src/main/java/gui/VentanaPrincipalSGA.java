package gui;

import gui.componentes.FabricaComponentesUI;
import gui.componentes.NotificadorUI;
import gui.integracion.ConectorSGA;
import gui.navegacion.GestorNavegacion;
import gui.navegacion.TipoVista;
import gui.navegacion.VistaBase;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.Map;

/**
 * Ventana única del SGA (patrón Single-Window):
 * <pre>
 * ┌──────────┬──────────────────────────────────────┬────────────┐
 * │          │ ← Atrás   Título        Cargar datos │            │
 * │ Sidebar  ├──────────────────────────────────────┤ Inspector  │
 * │ (vistas) │ [Banner de notificación inline]      │ (detalle / │
 * │          │ Contenido de la vista activa         │  edición)  │
 * └──────────┴──────────────────────────────────────┴────────────┘
 * </pre>
 * Ninguna operación abre otro {@code Stage} ni {@code Alert}: los detalles y
 * formularios contextuales van al inspector y los mensajes al banner.
 */
public class VentanaPrincipalSGA extends BorderPane {

    private final ConectorSGA conector;
    private final NotificadorUI notificador = new NotificadorUI();
    private final GestorNavegacion navegacion = new GestorNavegacion(this);

    private final Label lblTitulo = new Label();
    private final StackPane contenedorVista = new StackPane();
    private final Map<TipoVista, ToggleButton> itemsMenu = new EnumMap<>(TipoVista.class);
    private final ToggleGroup grupoMenu = new ToggleGroup();

    private final VBox inspector = new VBox();
    private final Label lblTituloInspector = new Label();

    public VentanaPrincipalSGA(ConectorSGA conector) {
        this.conector = conector;
        setLeft(crearSidebar());
        setTop(crearBarraSuperior());
        setCenter(crearAreaCentral());
        construirInspector();
    }

    /** Muestra la sección inicial. Se llama después de registrar las vistas. */
    public void iniciar(TipoVista vistaInicial) {
        navegacion.navegarA(vistaInicial);
    }

    // ---------- Construcción ----------

    private VBox crearSidebar() {
        Label marca = new Label("Aparta-Suites UQ");
        marca.getStyleClass().add("sidebar-marca");
        Label submarca = new Label("Sistema de Gestión de Alojamiento");
        submarca.getStyleClass().add("sidebar-submarca");

        VBox sidebar = new VBox(marca, submarca);
        sidebar.getStyleClass().add("sidebar");

        for (TipoVista tipo : TipoVista.values()) {
            ToggleButton item = new ToggleButton(tipo.getEtiquetaMenu());
            item.getStyleClass().add("sidebar-item");
            item.setToggleGroup(grupoMenu);
            item.setMaxWidth(Double.MAX_VALUE);
            item.setOnAction(e -> {
                item.setSelected(true); // un clic sobre la vista activa no la deselecciona
                navegacion.navegarA(tipo);
            });
            itemsMenu.put(tipo, item);
            sidebar.getChildren().add(item);
        }
        return sidebar;
    }

    private HBox crearBarraSuperior() {
        Button btnAtras = FabricaComponentesUI.crearBotonSecundario("← Atrás", navegacion::retroceder);
        btnAtras.setAccessibleText("Volver a la vista anterior");
        btnAtras.disableProperty().bind(navegacion.puedeRetrocederProperty().not());

        lblTitulo.getStyleClass().add("titulo-seccion");

        Button btnCargar = FabricaComponentesUI.crearBotonSecundario("Cargar escenario inicial",
                () -> notificador.mostrarAdvertencia(
                        "Pendiente de integración: la carga del escenario inicial depende de persistencia/."));

        HBox barra = new HBox(btnAtras, lblTitulo, FabricaComponentesUI.crearEspaciador(), btnCargar);
        barra.getStyleClass().add("barra-superior");
        return barra;
    }

    private VBox crearAreaCentral() {
        VBox.setVgrow(contenedorVista, Priority.ALWAYS);
        VBox area = new VBox(12, notificador, contenedorVista);
        area.getStyleClass().add("contenido-central");
        return area;
    }

    private void construirInspector() {
        lblTituloInspector.getStyleClass().add("inspector-titulo");
        lblTituloInspector.setWrapText(true);
        Button cerrar = new Button("✕");
        cerrar.getStyleClass().add("boton-icono");
        cerrar.setAccessibleText("Cerrar panel");
        cerrar.setOnAction(e -> ocultarInspector());

        HBox encabezado = new HBox(lblTituloInspector, FabricaComponentesUI.crearEspaciador(), cerrar);
        inspector.getStyleClass().add("inspector");
        inspector.getChildren().add(encabezado);
    }

    // ---------- API para vistas y navegación ----------

    /** Coloca la vista en el área central. Lo invoca {@link GestorNavegacion}. */
    public void mostrarContenido(TipoVista tipo, VistaBase vista) {
        ocultarInspector();
        notificador.ocultar();
        lblTitulo.setText(tipo.getTitulo());
        itemsMenu.get(tipo).setSelected(true);

        Node contenido = vista != null
                ? vista.getContenido()
                : FabricaComponentesUI.crearEstadoVacio("Vista en construcción",
                "Esta sección aún no está registrada en AplicacionSGA.");
        contenedorVista.getChildren().setAll(contenido);
    }

    /** Abre el panel lateral derecho con un detalle o formulario contextual. */
    public void mostrarInspector(String titulo, Node contenido) {
        // Se crea un ScrollPane nuevo en cada apertura: reutilizarlo puede mostrar
        // el contenido anterior cacheado por su skin.
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        scroll.setPadding(Insets.EMPTY);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        lblTituloInspector.setText(titulo);
        inspector.getChildren().setAll(inspector.getChildren().getFirst(), scroll);
        setRight(inspector);
    }

    public void ocultarInspector() {
        setRight(null);
        if (inspector.getChildren().size() > 1) {
            inspector.getChildren().remove(1);
        }
    }

    public NotificadorUI getNotificador() {
        return notificador;
    }

    public GestorNavegacion getNavegacion() {
        return navegacion;
    }

    public ConectorSGA getConector() {
        return conector;
    }
}
