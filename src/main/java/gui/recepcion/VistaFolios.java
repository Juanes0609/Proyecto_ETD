package gui.recepcion;

import gui.VentanaPrincipalSGA;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.FormularioFlexible;
import gui.componentes.Formato;
import gui.componentes.Iconos;
import gui.integracion.dto.MovimientoFolioDTO;
import gui.integracion.dto.ReservaDTO;
import gui.navegacion.VistaBase;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

/**
 * Folios: cargos y pagos de cada reserva activa según los servicios que pida el
 * huésped, con la opción de deshacer la última operación (Pila de deshacer, que
 * registra un movimiento contrario porque el folio es inalterable).
 */
public class VistaFolios extends VistaBase {

    private static final List<String> SERVICIOS = List.of("Restaurante", "Lavandería", "Minibar",
            "Tour al valle de Cocora", "Transporte", "Late check-out");

    private final TextField buscador = FabricaComponentesUI.crearCampoTexto("Buscar por reserva o huésped");
    private final TableView<ReservaDTO> reservas = FabricaComponentesUI.crearTabla("Sin folios abiertos",
            "Los folios se abren con las reservas confirmadas y en curso.");
    private final TableView<MovimientoFolioDTO> movimientos = FabricaComponentesUI.crearTabla("Sin movimientos",
            "Los cargos y pagos de esta reserva aparecerán aquí.");
    private final StackPane panelFolio = new StackPane();
    private List<ReservaDTO> abiertas = List.of();
    private ReservaDTO seleccionada;

    public VistaFolios(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        FabricaComponentesUI.agregarColumna(reservas, "Reserva", ReservaDTO::codigo);
        FabricaComponentesUI.agregarColumna(reservas, "Huésped", ReservaDTO::nombreTitular);
        FabricaComponentesUI.agregarColumna(reservas, "Saldo", r -> Formato.moneda(r.saldo()));
        reservas.getSelectionModel().selectedItemProperty().addListener((o, a, r) -> {
            seleccionada = r;
            pintarFolio();
        });
        buscador.textProperty().addListener((o, a, b) -> filtrar());

        FabricaComponentesUI.agregarColumna(movimientos, "Fecha", m -> Formato.fechaHora(m.fechaHora()));
        FabricaComponentesUI.agregarColumnaEstado(movimientos, "Tipo", MovimientoFolioDTO::tipo);
        FabricaComponentesUI.agregarColumna(movimientos, "Concepto", MovimientoFolioDTO::concepto);
        FabricaComponentesUI.agregarColumna(movimientos, "Monto", m -> Formato.moneda(m.efectoEnSaldo()));

        VBox izquierda = new VBox(12, buscador, reservas);
        izquierda.setPrefWidth(380);
        izquierda.setMinWidth(320);
        HBox.setHgrow(panelFolio, Priority.ALWAYS);
        HBox contenido = new HBox(20, izquierda, panelFolio);
        VBox.setVgrow(contenido, Priority.ALWAYS);
        return new VBox(contenido);
    }

    @Override
    public void alMostrar() {
        String codigo = seleccionada == null ? null : seleccionada.codigo();
        notificador().ejecutarConManejo(() -> abiertas = conector().reservasActivas().stream()
                .filter(r -> "CONFIRMADA".equals(r.estado()) || "EN_CURSO".equals(r.estado())).toList(), null);
        filtrar();
        abiertas.stream().filter(r -> r.codigo().equals(codigo)).findFirst()
                .ifPresentOrElse(r -> reservas.getSelectionModel().select(r), this::pintarFolio);
    }

    private void filtrar() {
        String texto = buscador.getText().trim().toLowerCase(Locale.ROOT);
        reservas.getItems().setAll(abiertas.stream().filter(r -> texto.isEmpty()
                || r.codigo().toLowerCase(Locale.ROOT).contains(texto)
                || r.nombreTitular().toLowerCase(Locale.ROOT).contains(texto)).toList());
    }

    private void pintarFolio() {
        if (seleccionada == null) {
            panelFolio.getChildren().setAll(FabricaComponentesUI.crearEstadoVacio(Iconos.Icono.RECIBO,
                    "Elige una reserva", "Selecciona una reserva de la lista para ver y registrar sus movimientos.", null));
            return;
        }
        ReservaDTO r = seleccionada;
        double saldo;
        try {
            List<MovimientoFolioDTO> lista = conector().movimientosFolio(r.codigo());
            movimientos.getItems().setAll(lista);
            saldo = lista.stream().mapToDouble(MovimientoFolioDTO::efectoEnSaldo).sum();
        } catch (RuntimeException e) {
            notificador().ejecutarConManejo(() -> { throw e; }, null);
            return;
        }

        Label lblSaldo = new Label(Formato.moneda(saldo) + " COP");
        lblSaldo.getStyleClass().add("suite-precio");
        VBox encabezado = new VBox(2, FabricaComponentesUI.crearEncabezado(r.nombreTitular(), "Reserva " + r.codigo()
                + " · " + r.nombreApartaSuite() + " · " + Formato.rango(r.entrada(), r.salida())),
                FabricaComponentesUI.crearTexto("Saldo pendiente", "campo-label"), lblSaldo);

        ComboBox<String> concepto = new ComboBox<>();
        concepto.getItems().setAll(SERVICIOS);
        concepto.setEditable(true);
        concepto.setPromptText("Ej. Restaurante");
        TextField monto = FabricaComponentesUI.crearCampoTexto("Ej. 45000");
        FormularioFlexible formulario = new FormularioFlexible()
                .agregarCampo("Concepto", concepto, "Elige un servicio o escribe otro.")
                .agregarCampo("Monto", monto, "En pesos, sin puntos.")
                .conValidacion("Monto", c -> monto.getText().trim().matches("\\d{1,9}")
                        && Long.parseLong(monto.getText().trim()) > 0 ? null : "Escribe un valor mayor que cero.");
        HBox campos = new HBox(16, formulario);
        HBox.setHgrow(formulario, Priority.ALWAYS);

        Button cargo = FabricaComponentesUI.crearBotonPrimario("Registrar cargo", () ->
                registrar(formulario, concepto, monto, true));
        Button pago = FabricaComponentesUI.crearBotonSecundario("Registrar pago", () ->
                registrar(formulario, concepto, monto, false));
        Button deshacer = FabricaComponentesUI.conIcono(FabricaComponentesUI.crearEnlace("Deshacer última operación",
                () -> {
                    if (notificador().ejecutarConManejo(() -> conector().deshacerUltimaOperacion(r.codigo()),
                            "Se anuló la última operación del folio.")) {
                        alMostrar();
                    }
                }), Iconos.Icono.DESHACER);

        HBox acciones = FabricaComponentesUI.crearBarraAcciones(cargo, pago, FabricaComponentesUI.crearEspaciador(), deshacer);
        VBox tarjeta = FabricaComponentesUI.crearTarjeta(encabezado, FabricaComponentesUI.crearSeparador(), campos,
                acciones);
        tarjeta.setSpacing(14);
        VBox panel = new VBox(16, tarjeta, movimientos);
        panel.setAlignment(Pos.TOP_LEFT);
        panelFolio.getChildren().setAll(panel);
    }

    private void registrar(FormularioFlexible formulario, ComboBox<String> concepto, TextField monto, boolean esCargo) {
        String texto = concepto.getEditor().getText();
        if (texto != null && !texto.isBlank()) {
            concepto.setValue(texto.trim());
        }
        if (!formulario.validar()) {
            return;
        }
        String codigo = seleccionada.codigo();
        double valor = Double.parseDouble(monto.getText().trim());
        boolean ok = notificador().ejecutarConManejo(() -> {
            if (esCargo) {
                conector().registrarCargo(codigo, concepto.getValue(), valor);
            } else {
                conector().registrarPago(codigo, concepto.getValue(), valor);
            }
        }, (esCargo ? "Cargo" : "Pago") + " de " + Formato.moneda(valor) + " registrado en " + codigo + ".");
        if (ok) {
            alMostrar();
        }
    }
}
