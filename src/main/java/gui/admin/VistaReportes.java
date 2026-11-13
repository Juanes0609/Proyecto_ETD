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

import java.util.Map;

/**
 * Reportes avanzados: ocupación, ingresos, reservas por canal, conflictos de
 * canal y tiempo promedio de preparación, calculados recorriendo las estructuras.
 * Las barras llevan siempre su valor en texto (no dependen solo del largo o el color).
 */
public class VistaReportes extends VistaBase {

    private final VBox contenido = new VBox(20);

    public VistaReportes(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        return scroll;
    }

    @Override
    public void alMostrar() {
        notificador().ejecutarConManejo(() -> pintar(conector().generarReporte()), null);
    }

    private void pintar(ReporteDTO r) {
        int reservas = r.reservasPorCanal().values().stream().mapToInt(Integer::intValue).sum();
        // Fila de indicadores que reparte el ancho por igual: todas las tarjetas
        // miden lo mismo y, si la ventana es angosta, el detalle se ajusta en 2 líneas.
        HBox indicadores = new HBox(14,
                indicador("Ocupación", Math.round(r.porcentajeOcupacion()) + " %",
                        r.ocupados() + " de " + r.totalApartaSuites() + " ocupados"),
                indicador("Ingresos registrados", Formato.moneda(r.ingresos()), null),
                indicador("Reservas", String.valueOf(reservas), null),
                indicador("Conflictos de canal", String.valueOf(r.conflictosCanal()), null),
                indicador("Preparación promedio", Math.round(r.minutosPromedioPreparacion()) + " min", null));
        indicadores.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        HBox graficas = new HBox(20,
                tarjetaBarras("Reservas por canal", null, r.reservasPorCanal()),
                tarjetaBarras("Estado de los aparta-suites", null,
                        r.apartaSuitesPorEstado()));
        graficas.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        contenido.getChildren().setAll(indicadores, graficas);
    }

    private static VBox indicador(String etiqueta, String valor, String detalle) {
        Label lblValor = new Label(valor);
        VBox tarjeta = FabricaComponentesUI.crearTarjetaIndicador(etiqueta, lblValor);
        if (detalle != null) { // solo datos (ej. "1 de 10 ocupados"), nunca explicaciones
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
        return tarjeta;
    }
}
