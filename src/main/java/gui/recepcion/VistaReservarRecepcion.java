package gui.recepcion;

import gui.VentanaPrincipalSGA;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.FiltroApartaSuites;
import gui.componentes.FormularioReserva;
import gui.componentes.Formato;
import gui.componentes.Iconos;
import gui.componentes.TarjetaApartaSuite;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.SolicitudExternaDTO;
import gui.navegacion.VistaBase;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/**
 * Destino principal de recepción. Muestra los aparta-suites como cards (igual que
 * el huésped) para registrar reservas cuando un cliente llama o llega en persona,
 * y la cola del canal externo, que se procesa en orden de llegada (FIFO).
 */
public class VistaReservarRecepcion extends VistaBase {

    private final StackPane cuerpo = new StackPane();
    private final FlowPane grilla = new FlowPane(20, 20);
    private final TableView<SolicitudExternaDTO> tablaExterna = FabricaComponentesUI.crearTabla(
            "Sin solicitudes en espera", "Las solicitudes del canal externo aparecerán aquí en orden de llegada.");
    private final Button procesar = FabricaComponentesUI.crearBotonPrimario("Procesar siguiente", this::procesarSiguiente);
    private HBox segmentado;
    private FiltroApartaSuites filtro;
    private VBox panelSuites;
    private VBox panelExterno;
    private List<ApartaSuiteDTO> suites = List.of();
    private String temporada = "BAJA";
    private int segmento;

    public VistaReservarRecepcion(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        configurarTablaExterna();
        panelSuites = new VBox(16);
        panelExterno = new VBox(16, FabricaComponentesUI.crearBarraAcciones(
                FabricaComponentesUI.crearTextoSecundario("Se atiende siempre la solicitud más antigua. Si el "
                        + "aparta-suite ya está reservado, queda registrada como conflicto de canal."),
                FabricaComponentesUI.crearEspaciador(), procesar), tablaExterna);
        VBox contenido = new VBox(18);
        segmentado = crearSegmentado(0);
        contenido.getChildren().addAll(segmentado, cuerpo);
        return contenido;
    }

    @Override
    public void alMostrar() {
        notificador().ejecutarConManejo(() -> {
            suites = conector().listarApartaSuites().stream().filter(ApartaSuiteDTO::reservable).toList();
            temporada = conector().temporadaEn(LocalDate.now());
            tablaExterna.getItems().setAll(conector().colaCanalExterno());
        }, null);
        procesar.setDisable(tablaExterna.getItems().isEmpty());
        actualizarSegmentado();
        if (filtro == null && !suites.isEmpty()) {
            filtro = new FiltroApartaSuites(suites.stream().mapToInt(ApartaSuiteDTO::capacidad).max().orElse(1),
                    suites.stream().mapToDouble(s -> s.tarifa(temporada)).min().orElse(0),
                    suites.stream().mapToDouble(s -> s.tarifa(temporada)).max().orElse(0), this::pintarSuites);
            ScrollPane scroll = new ScrollPane(grilla);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("scroll-transparente");
            grilla.setPadding(new Insets(0, 0, 16, 0));
            panelSuites.getChildren().setAll(filtro, scroll);
        }
        if (filtro != null) {
            pintarSuites();
        } else {
            panelSuites.getChildren().setAll(FabricaComponentesUI.crearEstadoVacio(Iconos.Icono.CAMA,
                    "No hay aparta-suites reservables",
                    "El administrador puede registrarlos en Catálogos.", null));
        }
        mostrarSegmento(segmento);
    }

    private HBox crearSegmentado(int seleccion) {
        return FabricaComponentesUI.crearSegmentado(List.of("Aparta-suites",
                "Canal externo (" + tablaExterna.getItems().size() + ")"), seleccion, this::mostrarSegmento);
    }

    /** Mantiene el contador de la cola externa al día sin perder la selección. */
    private void actualizarSegmentado() {
        if (segmentado != null) {
            ((ToggleButton) segmentado.getChildren().get(1))
                    .setText("Canal externo (" + tablaExterna.getItems().size() + ")");
        }
    }

    private void mostrarSegmento(int indice) {
        if (indice != segmento) {
            ocultarHoja();
        }
        segmento = indice;
        cuerpo.getChildren().setAll(indice == 0 ? panelSuites : panelExterno);
    }

    private void pintarSuites() {
        List<ApartaSuiteDTO> visibles = suites.stream()
                .filter(s -> filtro.coincide(s.capacidad(), s.tarifa(temporada))).toList();
        filtro.setResultado(visibles.size());
        grilla.getChildren().clear();
        grilla.setAlignment(visibles.isEmpty() ? javafx.geometry.Pos.TOP_CENTER : javafx.geometry.Pos.TOP_LEFT);
        if (visibles.isEmpty()) {
            grilla.getChildren().add(FabricaComponentesUI.crearEstadoVacio(Iconos.Icono.FILTRO,
                    "Ningún aparta-suite coincide", "Ajusta el número de personas o el precio.",
                    FabricaComponentesUI.crearBotonSecundario("Limpiar filtros", filtro::limpiar)));
        }
        for (ApartaSuiteDTO suite : visibles) {
            grilla.getChildren().add(new TarjetaApartaSuite(suite, suite.tarifa(temporada), temporada, "Reservar",
                    () -> abrirReserva(suite), null, null));
        }
    }

    /** Reserva directa (teléfono o presencial): solo se diligencian titular y acompañantes. */
    private void abrirReserva(ApartaSuiteDTO suite) {
        LocalDate hoy = LocalDate.now();
        mostrarHoja("Reserva directa", new FormularioReserva(conector(), suite, hoy, hoy.plusDays(1), false,
                Math.min(filtro.personas(), suite.capacidad()), null, "DIRECTA", reserva -> {
            ocultarHoja();
            notificador().mostrarExito("Reserva " + reserva.codigo() + " confirmada a nombre de "
                    + reserva.nombreTitular() + " (" + Formato.rango(reserva.entrada(), reserva.salida()) + ").");
        }));
    }

    // ---------- Canal externo ----------

    private void configurarTablaExterna() {
        FabricaComponentesUI.agregarColumna(tablaExterna, "Turno", s -> String.valueOf(
                tablaExterna.getItems().indexOf(s) + 1));
        FabricaComponentesUI.agregarColumna(tablaExterna, "Solicitud", SolicitudExternaDTO::id);
        FabricaComponentesUI.agregarColumna(tablaExterna, "Huésped", SolicitudExternaDTO::nombreHuesped);
        FabricaComponentesUI.agregarColumna(tablaExterna, "Aparta-suite", SolicitudExternaDTO::codigoApartaSuite);
        FabricaComponentesUI.agregarColumna(tablaExterna, "Fechas", s -> Formato.rango(s.entrada(), s.salida()));
        FabricaComponentesUI.agregarColumnaNumero(tablaExterna, "Personas", s -> String.valueOf(s.personas()));
        FabricaComponentesUI.agregarColumna(tablaExterna, "Recibida", s -> Formato.fechaHora(s.recepcion()));
        tablaExterna.getColumns().getFirst().setMaxWidth(70);
    }

    private void procesarSiguiente() {
        try {
            String resultado = conector().procesarSiguienteSolicitudExterna();
            if (resultado.startsWith("Conflicto")) {
                notificador().mostrarAdvertencia(resultado);
            } else {
                notificador().mostrarExito(resultado);
            }
        } catch (RuntimeException e) {
            notificador().ejecutarConManejo(() -> { throw e; }, null);
        }
        alMostrar();
    }
}
