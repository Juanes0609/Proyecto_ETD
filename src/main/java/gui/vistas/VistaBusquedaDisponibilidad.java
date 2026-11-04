package gui.vistas;

import gui.VentanaPrincipalSGA;
import gui.componentes.CatalogosUI;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.FormularioFlexible;
import gui.navegacion.VistaBase;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Vista 5 — Búsqueda de apartamentos disponibles y creación de la reserva.
 * <p>
 * Al seleccionar un resultado, el formulario de reserva se abre en el inspector
 * lateral, sin salir de la vista ni abrir ventanas.
 */
public class VistaBusquedaDisponibilidad extends VistaBase {

    private final DatePicker fechaEntrada = new DatePicker();
    private final DatePicker fechaSalida = new DatePicker();
    private final Spinner<Integer> personas = new Spinner<>(1, 30, 2);
    private final ComboBox<String> criterioOrden = new ComboBox<>();
    private final TableView<Object> resultados = FabricaComponentesUI.crearTabla(
            "Sin resultados",
            "Indica las fechas y el número de personas y pulsa Buscar.");

    public VistaBusquedaDisponibilidad(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        configurarFiltros();
        configurarResultados();

        return new VBox(16,
                FabricaComponentesUI.crearBarraFiltros(
                        FabricaComponentesUI.crearCampoEtiquetado("Entrada", fechaEntrada),
                        FabricaComponentesUI.crearCampoEtiquetado("Salida", fechaSalida),
                        FabricaComponentesUI.crearCampoEtiquetado("Personas", personas),
                        FabricaComponentesUI.crearCampoEtiquetado("Ordenar por", criterioOrden),
                        FabricaComponentesUI.crearEspaciador(),
                        FabricaComponentesUI.crearBotonPrimario("Buscar", this::buscar)),
                resultados);
    }

    private void configurarFiltros() {
        fechaEntrada.setPromptText("dd/mm/aaaa");
        fechaSalida.setPromptText("dd/mm/aaaa");
        fechaEntrada.setDayCellFactory(selector -> celdaDesde(LocalDate.now()));
        fechaSalida.setDayCellFactory(selector -> celdaDesde(
                fechaEntrada.getValue() == null ? LocalDate.now().plusDays(1) : fechaEntrada.getValue().plusDays(1)));
        fechaEntrada.valueProperty().addListener((obs, anterior, nueva) -> {
            if (nueva != null && fechaSalida.getValue() != null && !fechaSalida.getValue().isAfter(nueva)) {
                fechaSalida.setValue(nueva.plusDays(1));
            }
        });

        personas.setEditable(true);
        personas.setPrefWidth(90);

        criterioOrden.getItems().setAll(CatalogosUI.CRITERIOS_ORDEN_APARTAMENTOS);
        criterioOrden.setValue(CatalogosUI.CRITERIOS_ORDEN_APARTAMENTOS.getFirst());
    }

    private void configurarResultados() {
        FabricaComponentesUI.agregarColumna(resultados, "Código", fila -> "");
        FabricaComponentesUI.agregarColumna(resultados, "Nombre", fila -> "");
        FabricaComponentesUI.agregarColumna(resultados, "Bloque", fila -> "");
        FabricaComponentesUI.agregarColumna(resultados, "Dormitorios", fila -> "");
        FabricaComponentesUI.agregarColumna(resultados, "Capacidad", fila -> "");
        FabricaComponentesUI.agregarColumna(resultados, "Total estimado", fila -> "");
        resultados.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionado) -> {
            if (seleccionado != null) {
                abrirFormularioReserva();
            }
        });
    }

    private void buscar() {
        LocalDate entrada = fechaEntrada.getValue();
        LocalDate salida = fechaSalida.getValue();
        if (entrada == null || salida == null) {
            notificador().mostrarAdvertencia("Selecciona la fecha de entrada y la de salida.");
            return;
        }
        if (!salida.isAfter(entrada)) {
            notificador().mostrarAdvertencia("La fecha de salida debe ser posterior a la de entrada.");
            return;
        }
        notificador().mostrarInfo("Pendiente de integración: la búsqueda de disponibilidad aún no está conectada al servicio.");
    }

    private void abrirFormularioReserva() {
        ComboBox<String> canal = new ComboBox<>();
        canal.getItems().setAll(CatalogosUI.CANALES_RESERVA.stream().map(FabricaComponentesUI::formatearEstado).toList());

        FormularioFlexible formulario = new FormularioFlexible();
        formulario
                .agregarCampoObligatorio("Documento del titular",
                        FabricaComponentesUI.crearCampoTexto("Huésped registrado"), null)
                .agregarCampoObligatorio("Canal", canal, null)
                .agregarCampo("Hora estimada de llegada", FabricaComponentesUI.crearCampoTexto("HH:mm"),
                        "Se usa para priorizar la preparación del apartamento.");
        formulario.agregarNodo(FabricaComponentesUI.crearBarraAcciones(
                FabricaComponentesUI.crearBotonPrimario("Confirmar reserva", () -> confirmarReserva(formulario)),
                FabricaComponentesUI.crearBotonSecundario("Cancelar", this::ocultarInspector)));

        mostrarInspector("Nueva reserva", formulario);
    }

    private void confirmarReserva(FormularioFlexible formulario) {
        List<String> faltantes = formulario.validarObligatorios();
        if (!faltantes.isEmpty()) {
            notificador().mostrarAdvertencia("Completa los campos obligatorios: " + String.join(", ", faltantes) + ".");
            return;
        }
        String hora = formulario.<TextField>obtenerControl("Hora estimada de llegada").getText();
        if (!hora.isBlank()) {
            try {
                LocalTime.parse(hora);
            } catch (DateTimeParseException e) {
                notificador().mostrarAdvertencia("La hora de llegada debe tener el formato HH:mm.");
                return;
            }
        }
        notificador().ejecutarConManejo(() -> {
            throw new UnsupportedOperationException("crearReserva");
        }, null);
    }

    private static DateCell celdaDesde(LocalDate minima) {
        return new DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setDisable(vacio || fecha.isBefore(minima));
            }
        };
    }
}
