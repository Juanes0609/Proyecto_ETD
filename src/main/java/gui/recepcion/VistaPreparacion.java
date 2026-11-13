package gui.recepcion;

import gui.VentanaPrincipalSGA;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.Formato;
import gui.componentes.Iconos;
import gui.componentes.NotificadorUI;
import gui.integracion.dto.OrdenPreparacionDTO;
import gui.integracion.dto.RutaDTO;
import gui.navegacion.VistaBase;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Preparación (aseo): la cola de prioridad atiende primero los aparta-suites con
 * llegada hoy; "Despachar siguiente" lo pasa a En preparación. Para cualquier
 * aparta-suite se muestra la ruta más corta desde el cuarto de aseo (Dijkstra).
 */
public class VistaPreparacion extends VistaBase {

    private final VBox listaCola = new VBox(10);
    private final VBox listaEnCurso = new VBox(10);
    private final VBox ruta = new VBox(10);
    private final Button despachar = FabricaComponentesUI.crearBotonPrimario("Despachar siguiente", this::despachar);
    /** Botón "Ver ruta" y fila de cada aparta-suite en espera, para marcar cuál se está mostrando. */
    private final java.util.Map<String, Button> botonesRuta = new java.util.HashMap<>();
    private final java.util.Map<String, HBox> filasCola = new java.util.HashMap<>();
    private VBox tarjetaRuta;
    private String rutaMostrada;

    public VistaPreparacion(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        VBox cola = FabricaComponentesUI.crearTarjeta(new HBox(FabricaComponentesUI.crearEncabezado("En espera",
                "Primero las llegadas de hoy; luego por hora de llegada y orden de ingreso."),
                FabricaComponentesUI.crearEspaciador(), despachar), listaCola);
        cola.setSpacing(14);
        VBox enCurso = FabricaComponentesUI.crearTarjeta(FabricaComponentesUI.crearEncabezado("En preparación",
                "Marca cada aparta-suite como preparado al terminar."), listaEnCurso);
        enCurso.setSpacing(14);
        VBox izquierda = new VBox(18, cola, enCurso);
        HBox.setHgrow(izquierda, Priority.ALWAYS);

        tarjetaRuta = FabricaComponentesUI.crearTarjeta(FabricaComponentesUI.crearEncabezado("Ruta más corta",
                "Desde el cuarto de aseo, por los pasillos abiertos."), ruta);
        tarjetaRuta.setSpacing(14);
        tarjetaRuta.setPrefWidth(360);
        tarjetaRuta.setMinWidth(320);
        tarjetaRuta.setMaxHeight(Region.USE_PREF_SIZE);

        HBox columnas = new HBox(20, izquierda, tarjetaRuta);
        ScrollPane scroll = new ScrollPane(columnas);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        return scroll;
    }

    @Override
    public void alMostrar() {
        notificador().ejecutarConManejo(() -> {
            List<OrdenPreparacionDTO> cola = conector().colaPreparacion();
            List<OrdenPreparacionDTO> enCurso = conector().enPreparacion();
            pintarCola(cola);
            pintarEnCurso(enCurso);
            despachar.setDisable(cola.isEmpty());
            // Se conserva la ruta que se estaba viendo si ese aparta-suite sigue en la cola.
            OrdenPreparacionDTO actual = cola.stream().filter(o -> o.codigoApartaSuite().equals(rutaMostrada))
                    .findFirst().orElse(null);
            if (actual != null) {
                mostrarRuta(actual);
            } else if (!cola.isEmpty()) {
                mostrarRuta(cola.getFirst());
            } else if (!enCurso.isEmpty()) {
                mostrarRuta(enCurso.getFirst());
            } else {
                rutaMostrada = null;
                ruta.getChildren().setAll(FabricaComponentesUI.crearTextoSecundario(
                        "No hay aparta-suites pendientes de preparación."));
            }
        }, null);
    }

    private void pintarCola(List<OrdenPreparacionDTO> cola) {
        listaCola.getChildren().clear();
        botonesRuta.clear();
        filasCola.clear();
        if (cola.isEmpty()) {
            listaCola.getChildren().add(FabricaComponentesUI.crearEstadoVacio(Iconos.Icono.CHECK,
                    "Todo al día", "No hay aparta-suites esperando preparación.", null));
        }
        for (int i = 0; i < cola.size(); i++) {
            OrdenPreparacionDTO o = cola.get(i);
            Button verRuta = FabricaComponentesUI.conIcono(FabricaComponentesUI.crearEnlace("Ver ruta",
                    () -> mostrarRuta(o)), Iconos.Icono.FLECHA_DERECHA);
            verRuta.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            verRuta.setAccessibleText("Ver la ruta hasta " + o.nombreApartaSuite() + " en el panel de la derecha");
            HBox fila = crearFila(i + 1, o, verRuta);
            fila.getStyleClass().add("fila-cola");
            botonesRuta.put(o.codigoApartaSuite(), verRuta);
            filasCola.put(o.codigoApartaSuite(), fila);
            listaCola.getChildren().add(fila);
        }
    }

    private void pintarEnCurso(List<OrdenPreparacionDTO> enCurso) {
        listaEnCurso.getChildren().clear();
        if (enCurso.isEmpty()) {
            listaEnCurso.getChildren().add(FabricaComponentesUI.crearTextoSecundario("Ninguno en este momento."));
        }
        for (OrdenPreparacionDTO o : enCurso) {
            Button terminar = FabricaComponentesUI.crearBotonSecundario("Marcar como preparado", () -> {
                if (notificador().ejecutarConManejo(() -> conector().terminarPreparacion(o.codigoApartaSuite()),
                        o.nombreApartaSuite() + " quedó preparado y disponible para check-in.")) {
                    alMostrar();
                }
            });
            listaEnCurso.getChildren().add(crearFila(0, o, terminar));
        }
    }

    private HBox crearFila(int posicion, OrdenPreparacionDTO o, Node accion) {
        Label numero = new Label(posicion > 0 ? String.valueOf(posicion) : "•");
        numero.getStyleClass().addAll("avatar");
        Label nombre = FabricaComponentesUI.crearTexto(o.codigoApartaSuite() + " · " + o.nombreApartaSuite(),
                "encabezado-vista");
        Label prioridad = FabricaComponentesUI.crearBadgeEstado(o.entradaHoy() ? "LLEGA_HOY" : "SIN_LLEGADA_HOY");
        prioridad.setText(o.entradaHoy() ? "Llega hoy · " + Formato.hora(o.horaLlegadaSiguiente()) : "Sin llegada hoy");
        prioridad.getStyleClass().add(o.entradaHoy() ? "badge-pendiente" : "badge-neutro");
        Label ingreso = FabricaComponentesUI.crearTexto("En cola desde " + Formato.fechaHora(o.ingresoCola()),
                "texto-terciario");
        VBox textos = new VBox(4, nombre, new HBox(8, prioridad, ingreso));
        HBox fila = new HBox(12, numero, textos, FabricaComponentesUI.crearEspaciador(), accion);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private void despachar() {
        try {
            OrdenPreparacionDTO o = conector().despacharSiguientePreparacion();
            notificador().mostrarExito(o.nombreApartaSuite() + " pasó a preparación.");
        } catch (RuntimeException e) {
            notificador().ejecutarConManejo(() -> { throw e; }, null);
        }
        alMostrar();
    }

    /**
     * Muestra la ruta en el panel derecho. La fila elegida queda resaltada y su
     * botón cambia a "En el panel" (deshabilitado), así se ve qué ruta está a la
     * vista y no parece que el botón no haga nada.
     */
    private void mostrarRuta(OrdenPreparacionDTO o) {
        boolean cambio = !o.codigoApartaSuite().equals(rutaMostrada);
        rutaMostrada = o.codigoApartaSuite();
        botonesRuta.forEach((codigo, boton) -> {
            boolean actual = codigo.equals(rutaMostrada);
            boton.setDisable(actual);
            boton.setText(actual ? "En el panel" : "Ver ruta");
            boton.setGraphic(Iconos.crear(actual ? Iconos.Icono.CHECK : Iconos.Icono.FLECHA_DERECHA, 16));
        });
        filasCola.forEach((codigo, fila) -> {
            fila.getStyleClass().remove("fila-cola-actual");
            if (codigo.equals(rutaMostrada)) {
                fila.getStyleClass().add("fila-cola-actual");
            }
        });
        if (cambio && tarjetaRuta != null) { // breve fundido para notar que el panel cambió
            javafx.animation.FadeTransition f = new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), ruta);
            f.setFromValue(0.2);
            f.setToValue(1);
            f.play();
        }
        ruta.getChildren().setAll(FabricaComponentesUI.crearTexto(o.codigoApartaSuite() + " · " + o.nombreApartaSuite(),
                "encabezado-vista"));
        try {
            RutaDTO r = conector().rutaPreparacion(o.codigoApartaSuite());
            for (int i = 0; i < r.pasos().size(); i++) {
                Label paso = new Label((i + 1) + ".  " + r.pasos().get(i));
                if (i == r.pasos().size() - 1) {
                    paso.getStyleClass().add("encabezado-vista");
                }
                ruta.getChildren().add(paso);
            }
            Label total = new Label(Math.round(r.metros()) + " m");
            total.getStyleClass().add("suite-precio");
            ruta.getChildren().addAll(FabricaComponentesUI.crearSeparador(),
                    FabricaComponentesUI.crearTexto("Distancia total", "campo-label"), total);
        } catch (RuntimeException e) {
            ruta.getChildren().add(FabricaComponentesUI.crearErrorInline(NotificadorUI.mensajeDe(e)));
        }
    }
}
