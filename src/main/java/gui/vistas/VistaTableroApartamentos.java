package gui.vistas;

import gui.VentanaPrincipalSGA;
import gui.componentes.CatalogosUI;
import gui.componentes.FabricaComponentesUI;
import gui.navegacion.VistaBase;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Vista 2 — Tablero de apartamentos como tarjetas coloreadas por {@code EstadoOperativo}.
 * <p>
 * Al conectar el modelo, cada tarjeta abrirá en el inspector el detalle del
 * apartamento y la opción de marcarlo fuera de servicio.
 */
public class VistaTableroApartamentos extends VistaBase {

    private static final String TODOS = "Todos los estados";

    private final ComboBox<String> filtroEstado = new ComboBox<>();
    private final TextField filtroBloque = new TextField();
    private final FlowPane tarjetas = new FlowPane(12, 12);

    public VistaTableroApartamentos(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        filtroEstado.getItems().add(TODOS);
        filtroEstado.getItems().addAll(CatalogosUI.ESTADOS_OPERATIVOS.stream()
                .map(FabricaComponentesUI::formatearEstado).toList());
        filtroEstado.setValue(TODOS);
        filtroEstado.setAccessibleText("Filtrar por estado");
        filtroEstado.valueProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());

        filtroBloque.setPromptText("Filtrar por bloque");
        filtroBloque.setAccessibleText("Filtrar por bloque");
        filtroBloque.textProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());

        HBox filtros = FabricaComponentesUI.crearBarraFiltros(filtroEstado, filtroBloque,
                FabricaComponentesUI.crearBotonSecundario("Limpiar filtros", this::limpiarFiltros));

        HBox leyenda = new HBox(8);
        CatalogosUI.ESTADOS_OPERATIVOS.forEach(e -> leyenda.getChildren().add(FabricaComponentesUI.crearBadgeEstado(e)));

        VBox estadoVacio = FabricaComponentesUI.crearEstadoVacio(
                "Sin apartamentos para mostrar",
                "Las tarjetas aparecerán cuando el catálogo de apartamentos esté conectado.");
        estadoVacio.visibleProperty().bind(Bindings.isEmpty(tarjetas.getChildren()));

        StackPane area = new StackPane(tarjetas, estadoVacio);
        ScrollPane scroll = new ScrollPane(area);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return new VBox(16, filtros, leyenda, scroll);
    }

    private void aplicarFiltros() {
        // Se conectará al filtro por estado/bloque de ConectorSGA.
    }

    private void limpiarFiltros() {
        filtroEstado.setValue(TODOS);
        filtroBloque.clear();
    }
}
