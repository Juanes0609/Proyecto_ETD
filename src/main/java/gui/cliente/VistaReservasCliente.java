package gui.cliente;

import gui.componentes.FabricaComponentesUI;
import gui.componentes.Formato;
import gui.componentes.Iconos;
import gui.integracion.dto.ReservaDTO;
import gui.navegacion.VistaBase;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Reservas del huésped: las actuales (con su estado y lo que puede hacer en cada
 * una) y el historial de estancias. Pagar el anticipo y cancelar se confirman en
 * el mini menú lateral; al cancelar se muestra antes cuánto se devolvería.
 */
public class VistaReservasCliente extends VistaBase {

    private static final String POLITICA = "Cancelación gratuita hasta 7 días antes de la llegada. Entre 2 y 6 días "
            + "se devuelve el 50 % de lo pagado; con menos de 2 días no hay devolución.";

    private final VentanaCliente ventana;
    private final VBox lista = new VBox(14);
    private boolean verHistorial;

    public VistaReservasCliente(VentanaCliente ventana) {
        super(ventana);
        this.ventana = ventana;
    }

    @Override
    protected Node construir() {
        HBox segmentado = FabricaComponentesUI.crearSegmentado(List.of("Actuales", "Historial"), 0, indice -> {
            verHistorial = indice == 1;
            pintar();
        });
        lista.setMaxWidth(820);
        VBox contenido = new VBox(18, FabricaComponentesUI.crearTituloPagina("Mis reservas",
                "Consulta el estado de tus reservas, paga el anticipo o cancela."), segmentado, lista);
        contenido.setPadding(new Insets(8, 0, 24, 0));
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        return scroll;
    }

    @Override
    public void alMostrar() {
        pintar();
    }

    private void pintar() {
        notificador().ejecutarConManejo(() -> {
            List<ReservaDTO> reservas = conector().reservasDeHuesped(usuario().documento()).stream()
                    .filter(r -> r.activa() != verHistorial).toList();
            lista.getChildren().clear();
            if (reservas.isEmpty()) {
                lista.getChildren().add(verHistorial
                        ? FabricaComponentesUI.crearEstadoVacio(Iconos.Icono.RELOJ, "Sin estancias anteriores",
                        "Aquí verás tus estancias finalizadas y canceladas.", null)
                        : FabricaComponentesUI.crearEstadoVacio(Iconos.Icono.BOLSA, "No tienes reservas activas",
                        "Elige un aparta-suite y solicita tu reserva.",
                        FabricaComponentesUI.crearBotonSecundario("Explorar aparta-suites",
                                () -> ventana.irA(VentanaCliente.Pestana.INICIO))));
                return;
            }
            reservas.forEach(r -> lista.getChildren().add(crearTarjeta(r)));
        }, null);
    }

    private VBox crearTarjeta(ReservaDTO r) {
        Label nombre = FabricaComponentesUI.crearTexto(r.nombreApartaSuite(), "suite-nombre");
        Label fechas = new Label(Formato.rango(r.entrada(), r.salida()) + " · "
                + Formato.plural(r.noches(), "noche", "noches") + " · " + Formato.plural(r.personas(), "huésped", "huéspedes"));
        Label codigo = FabricaComponentesUI.crearTexto("Reserva " + r.codigo() + " · canal " + Formato.estado(r.canal())
                .toLowerCase(), "texto-terciario");
        VBox izquierda = new VBox(4, nombre, fechas, codigo);

        Label totalReserva = new Label(Formato.moneda(r.total()));
        totalReserva.getStyleClass().add("encabezado-vista");
        Label pagos = FabricaComponentesUI.crearTexto(pagosTexto(r), "texto-secundario");
        VBox derecha = new VBox(6, FabricaComponentesUI.crearBadgeEstado(r.estado()), totalReserva, pagos);
        derecha.setAlignment(Pos.TOP_RIGHT);

        HBox fila = new HBox(16, izquierda, FabricaComponentesUI.crearEspaciador(), derecha);
        VBox tarjeta = FabricaComponentesUI.crearTarjeta(fila);

        HBox acciones = FabricaComponentesUI.crearBarraAcciones();
        if ("PENDIENTE".equals(r.estado())) {
            acciones.getChildren().addAll(
                    FabricaComponentesUI.crearBotonPrimario("Pagar anticipo · " + Formato.moneda(r.anticipoRequerido()),
                            () -> confirmarPago(r)),
                    FabricaComponentesUI.crearBotonPeligro("Cancelar", () -> confirmarCancelacion(r)));
        } else if ("CONFIRMADA".equals(r.estado())) {
            acciones.getChildren().add(FabricaComponentesUI.crearBotonPeligro("Cancelar", () -> confirmarCancelacion(r)));
        }
        if (!acciones.getChildren().isEmpty()) {
            tarjeta.getChildren().addAll(FabricaComponentesUI.crearSeparador(), acciones);
        }
        return tarjeta;
    }

    private static String pagosTexto(ReservaDTO r) {
        return switch (r.estado()) {
            case "PENDIENTE" -> "Anticipo pendiente";
            case "CANCELADA" -> "Pagado " + Formato.moneda(r.pagado());
            default -> "Pagado " + Formato.moneda(r.pagado()) + " · saldo " + Formato.moneda(Math.max(0, r.saldo()));
        };
    }

    private void confirmarPago(ReservaDTO r) {
        Label monto = new Label(Formato.moneda(r.anticipoRequerido()) + " COP");
        monto.getStyleClass().add("suite-precio");
        Button pagar = FabricaComponentesUI.crearBotonPrimario("Pagar " + Formato.moneda(r.anticipoRequerido()), () -> {
            if (notificador().ejecutarConManejo(() -> conector().pagarAnticipo(r.codigo()),
                    "Anticipo pagado. Tu reserva " + r.codigo() + " quedó confirmada.")) {
                ocultarHoja();
                pintar();
            }
        });
        pagar.setMaxWidth(Double.MAX_VALUE);
        mostrarHoja("Pagar anticipo", new VBox(14,
                FabricaComponentesUI.crearEncabezado(r.nombreApartaSuite(), Formato.rango(r.entrada(), r.salida())),
                FabricaComponentesUI.crearTexto("Anticipo (30 % del total)", "campo-label"), monto,
                FabricaComponentesUI.crearTextoSecundario("Total de la estancia: " + Formato.moneda(r.total())
                        + ". El saldo restante se paga en recepción al llegar."),
                pagar, FabricaComponentesUI.crearBotonSecundario("Ahora no", this::ocultarHoja)));
    }

    private void confirmarCancelacion(ReservaDTO r) {
        double devolucion;
        try {
            devolucion = conector().calcularDevolucion(r.codigo());
        } catch (RuntimeException e) {
            notificador().ejecutarConManejo(() -> { throw e; }, null);
            return;
        }
        Label monto = new Label(Formato.moneda(devolucion) + " COP");
        monto.getStyleClass().add("suite-precio");
        Button cancelar = new Button("Cancelar reserva");
        cancelar.getStyleClass().add("boton-peligro-solido");
        cancelar.setMaxWidth(Double.MAX_VALUE);
        cancelar.setOnAction(e -> {
            if (notificador().ejecutarConManejo(() -> conector().cancelarReserva(r.codigo()),
                    "Reserva " + r.codigo() + " cancelada." + (devolucion > 0
                            ? " Te devolveremos " + Formato.moneda(devolucion) + "." : ""))) {
                ocultarHoja();
                pintar();
            }
        });
        Button conservar = FabricaComponentesUI.crearBotonSecundario("Conservar reserva", this::ocultarHoja);
        conservar.setMaxWidth(Double.MAX_VALUE);
        VBox hoja = new VBox(14, FabricaComponentesUI.crearEncabezado(r.nombreApartaSuite(),
                Formato.rango(r.entrada(), r.salida()) + " · reserva " + r.codigo()));
        if (r.pagado() <= 0) {
            hoja.getChildren().add(FabricaComponentesUI.crearTextoConIcono(Iconos.Icono.CHECK,
                    "Aún no has pagado el anticipo, así que cancelar no tiene costo.", "texto-exito"));
        } else {
            hoja.getChildren().addAll(
                    FabricaComponentesUI.crearTexto("Pagado hasta hoy: " + Formato.moneda(r.pagado()), "texto-secundario"),
                    FabricaComponentesUI.crearTexto("Te devolveríamos", "campo-label"), monto,
                    FabricaComponentesUI.crearTextoConIcono(Iconos.Icono.INFO, POLITICA, "campo-ayuda"));
        }
        hoja.getChildren().addAll(cancelar, conservar);
        mostrarHoja("Cancelar reserva", hoja);
    }
}
