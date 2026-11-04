package gui.vistas;

import gui.VentanaPrincipalSGA;
import gui.componentes.FabricaComponentesUI;
import gui.navegacion.TipoVista;
import gui.navegacion.VistaBase;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Vista 1 — Resumen operativo con indicadores y accesos directos.
 */
public class VistaMenuPrincipal extends VistaBase {

    private static final String SIN_DATO = "—";

    private final Label totalApartamentos = new Label(SIN_DATO);
    private final Label disponiblesHoy = new Label(SIN_DATO);
    private final Label ocupados = new Label(SIN_DATO);
    private final Label enPreparacion = new Label(SIN_DATO);
    private final Label reservasActivas = new Label(SIN_DATO);

    public VistaMenuPrincipal(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        FlowPane indicadores = new FlowPane(12, 12,
                FabricaComponentesUI.crearTarjetaIndicador("Apartamentos", totalApartamentos),
                FabricaComponentesUI.crearTarjetaIndicador("Disponibles hoy", disponiblesHoy),
                FabricaComponentesUI.crearTarjetaIndicador("Ocupados", ocupados),
                FabricaComponentesUI.crearTarjetaIndicador("En preparación", enPreparacion),
                FabricaComponentesUI.crearTarjetaIndicador("Reservas activas", reservasActivas));

        FlowPane accesos = new FlowPane(12, 12);
        for (TipoVista tipo : TipoVista.values()) {
            if (tipo != TipoVista.MENU_PRINCIPAL) {
                accesos.getChildren().add(crearAcceso(tipo));
            }
        }

        VBox contenido = new VBox(16,
                FabricaComponentesUI.crearEncabezado("Indicadores",
                        "Los valores aparecerán al conectar la interfaz con el servicio del sistema."),
                indicadores,
                FabricaComponentesUI.crearEncabezado("Accesos directos", null),
                accesos);

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        return scroll;
    }

    private Node crearAcceso(TipoVista destino) {
        Label titulo = new Label(destino.getTitulo());
        titulo.getStyleClass().add("encabezado-vista");
        VBox tarjeta = FabricaComponentesUI.crearTarjeta(titulo,
                FabricaComponentesUI.crearTextoSecundario(destino.getDescripcion()));
        tarjeta.getStyleClass().add("tarjeta-interactiva");
        tarjeta.setPrefWidth(260);
        tarjeta.setFocusTraversable(true);
        tarjeta.setAccessibleText("Ir a " + destino.getTitulo());
        tarjeta.setOnMouseClicked(e -> navegarA(destino));
        tarjeta.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER, SPACE -> navegarA(destino);
                default -> { }
            }
        });
        return tarjeta;
    }
}
