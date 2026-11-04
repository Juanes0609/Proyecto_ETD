package gui.vistas;

import gui.VentanaPrincipalSGA;
import gui.componentes.CatalogosUI;
import gui.componentes.FabricaComponentesUI;
import gui.navegacion.VistaBase;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Vista 6 — Consulta de reservas y gestión de su ciclo de vida.
 * <p>
 * Al seleccionar una reserva, el inspector muestra el desglose de noches, el
 * saldo del folio y solo las acciones válidas para su estado actual.
 */
public class VistaConsultaReservas extends VistaBase {

    private static final String TODOS = "Todos los estados";

    private final TextField buscador = new TextField();
    private final ComboBox<String> filtroEstado = new ComboBox<>();
    private final TableView<Object> tabla = FabricaComponentesUI.crearTabla(
            "Sin reservas para mostrar",
            "Busca por código de reserva, documento del huésped o código de apartamento.");

    public VistaConsultaReservas(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        buscador.setPromptText("Código de reserva, documento o apartamento");
        buscador.setPrefColumnCount(28);
        buscador.setOnAction(e -> buscar());

        filtroEstado.getItems().add(TODOS);
        filtroEstado.getItems().addAll(CatalogosUI.ESTADOS_RESERVA.stream()
                .map(FabricaComponentesUI::formatearEstado).toList());
        filtroEstado.setValue(TODOS);

        configurarTabla();

        return new VBox(16,
                FabricaComponentesUI.crearBarraFiltros(
                        FabricaComponentesUI.crearCampoEtiquetado("Buscar", buscador),
                        FabricaComponentesUI.crearCampoEtiquetado("Estado", filtroEstado),
                        FabricaComponentesUI.crearEspaciador(),
                        FabricaComponentesUI.crearBotonPrimario("Buscar", this::buscar)),
                tabla);
    }

    private void configurarTabla() {
        FabricaComponentesUI.agregarColumna(tabla, "Código", fila -> "");
        FabricaComponentesUI.agregarColumna(tabla, "Huésped", fila -> "");
        FabricaComponentesUI.agregarColumna(tabla, "Apartamento", fila -> "");
        FabricaComponentesUI.agregarColumna(tabla, "Entrada", fila -> "");
        FabricaComponentesUI.agregarColumna(tabla, "Salida", fila -> "");
        FabricaComponentesUI.agregarColumna(tabla, "Canal", fila -> "");
        FabricaComponentesUI.agregarColumna(tabla, "Estado", fila -> "");
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionada) -> {
            if (seleccionada != null) {
                // El estado real se leerá de la reserva cuando exista modelo.Reserva.
                abrirDetalle(null);
            }
        });
    }

    private void buscar() {
        if (buscador.getText().isBlank() && TODOS.equals(filtroEstado.getValue())) {
            notificador().mostrarAdvertencia("Escribe un criterio de búsqueda o elige un estado.");
            return;
        }
        notificador().mostrarInfo("Pendiente de integración: la consulta de reservas aún no está conectada al servicio.");
    }

    private void abrirDetalle(String estado) {
        TableView<Object> desglose = FabricaComponentesUI.crearTabla(
                "Sin desglose", "El detalle noche a noche aparecerá al conectar la liquidación.");
        FabricaComponentesUI.agregarColumna(desglose, "Noche", fila -> "");
        FabricaComponentesUI.agregarColumna(desglose, "Temporada", fila -> "");
        FabricaComponentesUI.agregarColumna(desglose, "Cobrables", fila -> "");
        FabricaComponentesUI.agregarColumna(desglose, "Subtotal", fila -> "");
        desglose.setPrefHeight(220);

        Label saldo = new Label("—");
        saldo.getStyleClass().add("indicador-valor");

        VBox detalle = new VBox(14,
                new HBox(8, new Label("Estado:"), FabricaComponentesUI.crearBadgeEstado(estado)),
                FabricaComponentesUI.crearEncabezado("Desglose de noches", null),
                desglose,
                FabricaComponentesUI.crearTarjetaIndicador("Saldo pendiente del folio", saldo),
                crearAcciones(estado));

        mostrarInspector("Detalle de la reserva", detalle);
    }

    /**
     * Acciones disponibles según el {@code EstadoReserva}. Agregar una transición
     * nueva es agregar un {@code case}.
     */
    private Node crearAcciones(String estado) {
        HBox acciones = FabricaComponentesUI.crearBarraAcciones();
        switch (estado == null ? "" : estado) {
            case "PENDIENTE" -> acciones.getChildren().addAll(
                    accionPendiente(FabricaComponentesUI.crearBotonPrimario("Confirmar anticipo", null), "pagarAnticipoPortal"),
                    accionPendiente(FabricaComponentesUI.crearBotonPeligro("Cancelar", null), "cancelarReserva"));
            case "CONFIRMADA" -> acciones.getChildren().addAll(
                    accionPendiente(FabricaComponentesUI.crearBotonPrimario("Check-in", null), "registrarCheckIn"),
                    accionPendiente(FabricaComponentesUI.crearBotonPeligro("Cancelar", null), "cancelarReserva"));
            case "EN_CURSO" -> acciones.getChildren().add(
                    accionPendiente(FabricaComponentesUI.crearBotonPrimario("Check-out", null), "registrarCheckOut"));
            default -> acciones.getChildren().add(
                    FabricaComponentesUI.crearTextoSecundario("No hay acciones disponibles para este estado."));
        }
        return acciones;
    }

    /** Conecta el botón con la operación del servicio; hasta entonces avisa que está en construcción. */
    private Button accionPendiente(Button boton, String operacionServicio) {
        boton.setOnAction(e -> notificador().ejecutarConManejo(() -> {
            throw new UnsupportedOperationException(operacionServicio);
        }, null));
        return boton;
    }
}
