package gui.cliente;

import gui.componentes.FabricaComponentesUI;
import gui.componentes.FormularioFlexible;
import gui.componentes.FormularioReserva;
import gui.componentes.Formato;
import gui.componentes.Iconos;
import gui.componentes.NotificadorUI;
import gui.componentes.TarjetaApartaSuite;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.CotizacionDTO;
import gui.integracion.dto.HuespedDTO;
import gui.integracion.dto.NocheDTO;
import gui.navegacion.VistaBase;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/**
 * Detalle completo de un aparta-suite: foto, características, tarifas por
 * temporada y, a la derecha, la estancia con la cotización noche por noche
 * (recorrido de la estancia con Iterator en el núcleo). "Solicitar reserva"
 * abre el mini menú lateral con los datos de quien reserva y sus acompañantes.
 */
public class VistaDetalleApartaSuite extends VistaBase {

    private static final int MAX_NOCHES_VISIBLES = 7;

    private final VentanaCliente ventana;
    private final ApartaSuiteDTO suite;

    private final DatePicker entrada = gui.componentes.FabricaComponentesUI.crearSelectorFecha("dd/mm/aaaa");
    private final DatePicker salida = gui.componentes.FabricaComponentesUI.crearSelectorFecha("dd/mm/aaaa");
    private final Spinner<Integer> personas;
    private final VBox disponibilidad = new VBox();
    private final VBox desglose = new VBox(6);
    private final Label total = new Label("—");
    private final Label resumenTotal = new Label();
    private final Button solicitar;

    public VistaDetalleApartaSuite(VentanaCliente ventana, ApartaSuiteDTO suite) {
        super(ventana);
        this.ventana = ventana;
        this.suite = suite;
        personas = new Spinner<>(1, suite.capacidad(), Math.min(2, suite.capacidad()));
        solicitar = FabricaComponentesUI.crearBotonPrimario("Solicitar reserva", this::solicitarReserva);
    }

    @Override
    protected Node construir() {
        Button volver = FabricaComponentesUI.conIcono(FabricaComponentesUI.crearEnlace("Volver", ventana::volver),
                Iconos.Icono.ATRAS);

        HBox columnas = new HBox(28, crearInformacion(), crearEstancia());
        columnas.setAlignment(Pos.TOP_LEFT);

        VBox pagina = new VBox(14, volver, columnas);
        pagina.setPadding(new Insets(4, 0, 24, 0));
        ScrollPane scroll = new ScrollPane(pagina);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        return scroll;
    }

    // ---------- Columna izquierda: información ----------

    private VBox crearInformacion() {
        StackPane foto = TarjetaApartaSuite.crearFoto(suite, 560, 300, true);
        boolean favorito = conector().favoritos(usuario().documento()).stream()
                .anyMatch(f -> f.codigo().equals(suite.codigo()));
        ToggleButton corazon = TarjetaApartaSuite.crearBotonFavorito(suite, favorito, marcado ->
                notificador().ejecutarConManejo(() -> conector().alternarFavorito(usuario().documento(), suite.codigo()),
                        marcado ? suite.nombre() + " se guardó en tus favoritos." : suite.nombre() + " salió de tus favoritos."));
        foto.getChildren().add(corazon);

        Label nombre = FabricaComponentesUI.crearTexto(suite.nombre(), "titulo-grande");
        Label ubicacion = FabricaComponentesUI.crearTextoSecundario(suite.ubicacion() + " · " + suite.codigo());
        FlowPane datos = new FlowPane(8, 8,
                dato(Iconos.Icono.CAMA, Formato.plural(suite.dormitorios(), "dormitorio", "dormitorios")),
                dato(Iconos.Icono.GRUPO, "Hasta " + Formato.plural(suite.capacidad(), "huésped", "huéspedes")));

        FlowPane caracteristicas = new FlowPane(8, 8);
        suite.caracteristicas().forEach(c -> caracteristicas.getChildren().add(
                FabricaComponentesUI.crearEtiquetaCaracteristica(c)));

        VBox info = new VBox(10, foto, nombre, ubicacion, datos, FabricaComponentesUI.crearSeparador(),
                FabricaComponentesUI.crearEncabezado("Lo que ofrece", null), caracteristicas,
                FabricaComponentesUI.crearTextoSecundario(suite.descripcion()),
                FabricaComponentesUI.crearSeparador(),
                FabricaComponentesUI.crearEncabezado("Tarifas por persona y noche",
                        "El valor de cada noche depende de la temporada en que cae."),
                crearTablaTarifas());
        info.setPrefWidth(560);
        info.setMaxWidth(640);
        HBox.setHgrow(info, Priority.ALWAYS);
        return info;
    }

    private VBox crearTablaTarifas() {
        String vigente = conector().temporadaEn(LocalDate.now());
        VBox filas = new VBox(8);
        for (String temporada : List.of("BAJA", "MEDIA", "ALTA")) {
            Label nombre = new Label("Temporada " + Formato.estado(temporada).toLowerCase());
            Label valor = new Label(Formato.moneda(suite.tarifa(temporada)));
            valor.getStyleClass().add("encabezado-vista");
            HBox fila = new HBox(10, nombre);
            if (temporada.equals(vigente)) {
                Label badge = FabricaComponentesUI.crearBadgeEstado("VIGENTE");
                badge.getStyleClass().add("badge-confirmada");
                fila.getChildren().add(badge);
            }
            fila.getChildren().addAll(FabricaComponentesUI.crearEspaciador(), valor);
            fila.setAlignment(Pos.CENTER_LEFT);
            filas.getChildren().add(fila);
        }
        return filas;
    }

    private static HBox dato(Iconos.Icono icono, String texto) {
        HBox caja = new HBox(6, Iconos.crear(icono, 16), new Label(texto));
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.getStyleClass().add("etiqueta-caracteristica");
        return caja;
    }

    // ---------- Columna derecha: estancia y cotización ----------

    private VBox crearEstancia() {
        LocalDate hoy = LocalDate.now();
        entrada.setValue(hoy.plusDays(7));
        salida.setValue(hoy.plusDays(10));
        personas.setEditable(true);

        FormularioFlexible formulario = new FormularioFlexible()
                .agregarCampo("Entrada", entrada, null)
                .agregarCampo("Salida", salida, null)
                .agregarCampo("Personas", personas, "Incluye niños; los menores de 6 años no pagan.");
        entrada.valueProperty().addListener((o, a, b) -> {
            if (b != null && (salida.getValue() == null || !salida.getValue().isAfter(b))) {
                salida.setValue(b.plusDays(1));
            }
            cotizar();
        });
        salida.valueProperty().addListener((o, a, b) -> cotizar());
        personas.valueProperty().addListener((o, a, b) -> cotizar());

        total.getStyleClass().add("suite-precio");
        resumenTotal.getStyleClass().add("texto-secundario");
        resumenTotal.setWrapText(true);
        solicitar.setMaxWidth(Double.MAX_VALUE);

        VBox tarjeta = FabricaComponentesUI.crearTarjeta(
                FabricaComponentesUI.crearEncabezado("Tu estancia", "Elige fechas para ver el valor noche por noche."),
                formulario, disponibilidad, FabricaComponentesUI.crearSeparador(),
                FabricaComponentesUI.crearTexto("Desglose por noche", "campo-label"), desglose,
                FabricaComponentesUI.crearSeparador(), total, resumenTotal, solicitar);
        tarjeta.setSpacing(12);
        tarjeta.setPrefWidth(360);
        tarjeta.setMinWidth(340);
        tarjeta.setMaxWidth(380);
        cotizar();
        return tarjeta;
    }

    private void cotizar() {
        desglose.getChildren().clear();
        disponibilidad.getChildren().clear();
        LocalDate e = entrada.getValue();
        LocalDate s = salida.getValue();
        solicitar.setDisable(true);
        if (e == null || s == null || !s.isAfter(e)) {
            total.setText("—");
            resumenTotal.setText("Elige una salida posterior a la entrada.");
            return;
        }
        if (e.isBefore(LocalDate.now())) {
            total.setText("—");
            resumenTotal.setText("La entrada no puede estar en el pasado.");
            return;
        }
        try {
            boolean libre = conector().estaDisponible(suite.codigo(), e, s);
            disponibilidad.getChildren().add(libre
                    ? FabricaComponentesUI.crearTextoConIcono(Iconos.Icono.CHECK, "Disponible en esas fechas", "texto-exito")
                    : FabricaComponentesUI.crearErrorInline("Ya está reservado en parte de esas fechas. Prueba otras."));
            CotizacionDTO cotizacion = conector().cotizar(suite.codigo(), e, s, personas.getValue());
            List<NocheDTO> noches = cotizacion.noches();
            for (NocheDTO noche : noches.subList(0, Math.min(MAX_NOCHES_VISIBLES, noches.size()))) {
                Label fecha = new Label(Formato.diaSemana(noche.fecha()));
                Label temporada = FabricaComponentesUI.crearBadgeEstado(noche.temporada());
                Label subtotal = new Label(Formato.moneda(noche.subtotal()));
                HBox fila = new HBox(8, fecha, temporada, FabricaComponentesUI.crearEspaciador(), subtotal);
                fila.setAlignment(Pos.CENTER_LEFT);
                desglose.getChildren().add(fila);
            }
            if (noches.size() > MAX_NOCHES_VISIBLES) {
                desglose.getChildren().add(FabricaComponentesUI.crearTexto("y "
                        + Formato.plural(noches.size() - MAX_NOCHES_VISIBLES, "noche más", "noches más")
                        + " incluidas en el total", "texto-terciario"));
            }
            total.setText(Formato.moneda(cotizacion.total()) + " COP");
            resumenTotal.setText(Formato.plural(cotizacion.numeroNoches(), "noche", "noches") + " · "
                    + Formato.plural(personas.getValue(), "persona", "personas")
                    + ". El total final descuenta a los menores de 6 años.");
            solicitar.setDisable(!libre);
        } catch (RuntimeException ex) {
            total.setText("—");
            resumenTotal.setText(NotificadorUI.mensajeDe(ex));
        }
    }

    private void solicitarReserva() {
        HuespedDTO titular;
        try {
            titular = conector().obtenerHuesped(usuario().documento());
        } catch (RuntimeException ex) {
            titular = null;
        }
        mostrarHoja("Solicitar reserva", new FormularioReserva(conector(), suite, entrada.getValue(), salida.getValue(),
                true, personas.getValue(), titular, "PORTAL", reserva -> {
            ocultarHoja();
            ventana.irA(VentanaCliente.Pestana.RESERVAS);
            notificador().mostrarExito("Reserva " + reserva.codigo() + " creada. Paga el anticipo de "
                    + Formato.moneda(reserva.anticipoRequerido()) + " para confirmarla.");
        }));
    }
}
