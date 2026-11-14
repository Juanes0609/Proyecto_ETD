package gui.general;

import gui.VentanaPrincipalSGA;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.Formato;
import gui.componentes.Iconos;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.ReservaDTO;
import gui.navegacion.VistaBase;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inicio: tablero con todos los aparta-suites y su estado operativo (verde
 * Preparado, azul Ocupado, amarillo Pendiente o En preparación, rojo Fuera de
 * servicio). El estado va siempre escrito, no solo en el color. Es de consulta:
 * las acciones viven en Llegadas y salidas y en Preparación.
 */
public class VistaTablero extends VistaBase {

    /** Estados del enunciado en el orden en que se resumen arriba: título → estados que agrupa. */
    private static final Map<String, List<String>> GRUPOS = new LinkedHashMap<>();

    static {
        GRUPOS.put("Preparados", List.of("PREPARADO"));
        GRUPOS.put("Ocupados", List.of("OCUPADO"));
        GRUPOS.put("Por preparar", List.of("PENDIENTE_PREPARACION", "EN_PREPARACION"));
        GRUPOS.put("Fuera de servicio", List.of("FUERA_DE_SERVICIO"));
    }

    private final VBox contenido = new VBox(20);

    public VistaTablero(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        contenido.setPadding(new Insets(0, 0, 16, 0));
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        return scroll;
    }

    @Override
    public void alMostrar() {
        notificador().ejecutarConManejo(() -> pintar(conector().listarApartaSuites(), conector().reservasActivas()),
                null);
    }

    private void pintar(List<ApartaSuiteDTO> suites, List<ReservaDTO> reservas) {
        if (suites.isEmpty()) {
            contenido.getChildren().setAll(FabricaComponentesUI.crearEstadoVacio(Iconos.Icono.CAMA,
                    "Aún no hay aparta-suites", "El administrador puede registrarlos en Catálogos.", null));
            return;
        }
        HBox resumen = new HBox(14);
        GRUPOS.forEach((titulo, estados) -> {
            long cantidad = suites.stream().filter(s -> estados.contains(s.estadoOperativo())).count();
            VBox tarjeta = FabricaComponentesUI.crearTarjetaIndicador(titulo, new Label(String.valueOf(cantidad)));
            tarjeta.getStyleClass().addAll("tablero-celda", claseDe(estados.getFirst()));
            tarjeta.setMinWidth(130);
            tarjeta.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(tarjeta, Priority.ALWAYS);
            resumen.getChildren().add(tarjeta);
        });

        List<Node> secciones = new ArrayList<>();
        secciones.add(resumen);
        Map<String, FlowPane> porBloque = new LinkedHashMap<>();
        for (ApartaSuiteDTO suite : suites) { // llegan ordenados por código: el bloque sale en orden
            porBloque.computeIfAbsent(suite.bloque(), b -> new FlowPane(12, 12))
                    .getChildren().add(crearCelda(suite, reservas));
        }
        porBloque.forEach((bloque, celdas) -> secciones.add(new VBox(10,
                FabricaComponentesUI.crearEncabezado("Bloque " + bloque,
                        Formato.plural(celdas.getChildren().size(), "aparta-suite", "aparta-suites")), celdas)));
        contenido.getChildren().setAll(secciones);
    }

    private static VBox crearCelda(ApartaSuiteDTO suite, List<ReservaDTO> reservas) {
        Label codigo = FabricaComponentesUI.crearTexto(suite.codigo(), "encabezado-vista");
        codigo.setWrapText(false);
        codigo.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE); // el código nunca se parte en dos líneas
        Label nombre = FabricaComponentesUI.crearTexto(suite.nombre(), "texto-secundario");
        nombre.setWrapText(false); // nombres largos se cortan con "…" en vez de deformar la celda
        Label detalle = FabricaComponentesUI.crearTexto("Piso " + suite.piso() + " · "
                + Formato.plural(suite.capacidad(), "persona", "personas"), "texto-terciario");
        detalle.setWrapText(false);
        HBox cabecera = new HBox(8, codigo, FabricaComponentesUI.crearEspaciador(),
                FabricaComponentesUI.crearBadgeEstado(suite.estadoOperativo()));
        VBox celda = new VBox(4, cabecera, nombre, detalle);
        String movimiento = movimientoDe(suite, reservas);
        if (movimiento != null) {
            Label lbl = FabricaComponentesUI.crearTexto(movimiento, "texto-terciario");
            lbl.setWrapText(false);
            celda.getChildren().add(lbl);
        }
        celda.getStyleClass().addAll("tablero-celda", claseDe(suite.estadoOperativo()));
        celda.setPrefWidth(264);
        celda.setPrefHeight(108); // todas iguales, tengan o no la línea de llegada o salida
        celda.setAccessibleText(suite.codigo() + " " + suite.nombre() + ": " + Formato.estado(suite.estadoOperativo()));
        return celda;
    }

    /** Lo siguiente que pasa en el aparta-suite (quién sale o llega hoy) o, si está fuera de servicio, el motivo. */
    private static String movimientoDe(ApartaSuiteDTO suite, List<ReservaDTO> reservas) {
        if (!suite.reservable()) {
            String motivo = suite.motivoFueraServicio();
            return motivo == null || motivo.isBlank() ? null : motivo;
        }
        LocalDate hoy = LocalDate.now();
        for (ReservaDTO r : reservas) {
            if (!r.codigoApartaSuite().equals(suite.codigo())) {
                continue;
            }
            if ("EN_CURSO".equals(r.estado())) {
                return "Sale el " + Formato.diaMes(r.salida());
            }
            if ("CONFIRMADA".equals(r.estado()) && r.entrada().equals(hoy)) {
                return r.horaLlegada() == null ? "Llegada hoy" : "Llegada hoy · " + Formato.hora(r.horaLlegada());
            }
        }
        return null;
    }

    private static String claseDe(String estado) {
        return switch (estado) {
            case "PREPARADO" -> "tablero-preparado";
            case "OCUPADO" -> "tablero-ocupado";
            case "FUERA_DE_SERVICIO" -> "tablero-fuera";
            default -> "tablero-pendiente";
        };
    }
}
