package gui.admin;

import gui.VentanaPrincipalSGA;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.Formato;
import gui.integracion.dto.ReporteDTO;
import gui.navegacion.VistaBase;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Reportes avanzados: ocupación, ingresos, reservas por canal, conflictos de
 * canal y tiempo promedio de preparación, calculados recorriendo las estructuras.
 * Las barras llevan siempre su valor en texto (no dependen solo del largo o el color).
 */
public class VistaReportes extends VistaBase {

    /** Periodos que se pueden consultar: las noches ocupadas y los ingresos se calculan sobre el elegido. */
    private static final List<String> PERIODOS = List.of("Este mes", "Últimos 30 días", "Este año");

    private final VBox contenido = new VBox(20);
    private int periodo;

    public VistaReportes(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        HBox segmentado = FabricaComponentesUI.crearSegmentado(PERIODOS, 0, indice -> {
            periodo = indice;
            cargar();
        });
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return new VBox(16, segmentado, scroll);
    }

    @Override
    public void alMostrar() {
        cargar();
    }

    private void cargar() {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = switch (periodo) {
            case 1 -> hoy.minusDays(29);
            case 2 -> hoy.withDayOfYear(1);
            default -> hoy.withDayOfMonth(1);
        };
        LocalDate hasta = switch (periodo) { // el final no se incluye
            case 1 -> hoy.plusDays(1);
            case 2 -> hoy.withDayOfYear(1).plusYears(1);
            default -> hoy.withDayOfMonth(1).plusMonths(1);
        };
        notificador().ejecutarConManejo(() -> pintar(conector().generarReporte(desde, hasta)), null);
    }

    private void pintar(ReporteDTO r) {
        int reservas = r.reservasPorCanal().values().stream().mapToInt(Integer::intValue).sum();
        String rango = Formato.rango(r.desde(), r.hasta().minusDays(1));

        // Fila de indicadores que reparte el ancho por igual: todas las tarjetas
        // miden lo mismo y, si la ventana es angosta, el detalle se ajusta en 2 líneas.
        HBox indicadores = new HBox(14,
                indicador("Ocupación ahora", Math.round(r.porcentajeOcupacion()) + " %",
                        r.ocupados() + " de " + r.totalApartaSuites() + " ocupados"),
                indicador("Ingresos del periodo", Formato.moneda(r.ingresos()), "Pagos netos · " + rango),
                indicador("Reservas", String.valueOf(reservas), "Incluye canceladas y no-show"),
                indicador("Conflictos de canal", String.valueOf(r.conflictosCanal()), "Externas rechazadas"),
                indicador("Preparación promedio", Math.round(r.minutosPromedioPreparacion()) + " min",
                        "Desde que se despacha"));
        indicadores.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        contenido.getChildren().setAll(indicadores,
                fila(tarjetaBarras("Reservas por canal", "Todas las registradas.", r.reservasPorCanal()),
                        tarjetaBarras("Reservas por estado", "Incluye canceladas y no-show.", r.reservasPorEstado())),
                fila(tarjetaBarras("Noches ocupadas por temporada", rango, r.nochesPorTemporada()),
                        tarjetaBarras("Estado de los aparta-suites", "En este momento.", r.apartaSuitesPorEstado())),
                tarjetaBarras("Noches ocupadas por aparta-suite", rango, r.nochesPorApartaSuite()));
    }

    private static HBox fila(VBox izquierda, VBox derecha) {
        HBox fila = new HBox(20, izquierda, derecha);
        fila.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        return fila;
    }

    private static VBox indicador(String etiqueta, String valor, String detalle) {
        Label lblValor = new Label(valor);
        VBox tarjeta = FabricaComponentesUI.crearTarjetaIndicador(etiqueta, lblValor);
        if (detalle != null) {
            tarjeta.getChildren().add(FabricaComponentesUI.crearTexto(detalle, "texto-terciario"));
        }
        tarjeta.setPrefWidth(178);
        tarjeta.setMinWidth(130);
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        return tarjeta;
    }

    private static VBox tarjetaBarras(String titulo, String descripcion, Map<String, Integer> datos) {
        int maximo = Math.max(1, datos.values().stream().mapToInt(Integer::intValue).max().orElse(1));
        VBox filas = new VBox(12);
        datos.forEach((clave, valor) -> {
            Label nombre = new Label(Formato.estado(clave));
            nombre.setMinWidth(170);
            nombre.setPrefWidth(170);
            Region barra = new Region();
            barra.getStyleClass().add("barra-dato");
            StackPane pista = new StackPane(barra);
            pista.getStyleClass().add("barra-fondo");
            pista.setAlignment(Pos.CENTER_LEFT);
            pista.setMinHeight(12);
            HBox.setHgrow(pista, Priority.ALWAYS);
            barra.maxWidthProperty().bind(pista.widthProperty().multiply(valor / (double) maximo));
            barra.setMinWidth(valor > 0 ? 6 : 0);
            Label cifra = new Label(String.valueOf(valor));
            cifra.getStyleClass().add("encabezado-vista");
            cifra.setMinWidth(32);
            cifra.setAlignment(Pos.CENTER_RIGHT);
            HBox fila = new HBox(12, nombre, pista, cifra);
            fila.setAlignment(Pos.CENTER_LEFT);
            filas.getChildren().add(fila);
        });
        VBox tarjeta = FabricaComponentesUI.crearTarjeta(FabricaComponentesUI.crearEncabezado(titulo, descripcion), filas);
        tarjeta.setSpacing(16);
        tarjeta.setMinWidth(360);
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        return tarjeta;
    }
}
