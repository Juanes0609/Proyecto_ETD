package gui.general;

import gui.VentanaPrincipalSGA;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.Formato;
import gui.componentes.Iconos;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.ReservaDTO;
import gui.navegacion.VistaBase;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Calendario de ocupación: cuadrícula de aparta-suites (filas) por noches del
 * mes (columnas). Cada noche está ocupada (reserva confirmada, en curso o ya
 * finalizada), bloqueada (retenida por una reserva pendiente de anticipo, o
 * aparta-suite fuera de servicio) o libre. Las bloqueadas llevan borde punteado
 * para no depender solo del color, y cada noche no libre explica el motivo al
 * pasar el cursor. Es una tabla de datos: no se anima.
 */
public class VistaCalendario extends VistaBase {

    private static final Locale ESPANOL = Locale.of("es", "CO");
    private static final double ANCHO_NOMBRES = 190;
    private static final double ANCHO_MIN_NOCHE = 20;

    private enum Noche { LIBRE, OCUPADA, BLOQUEADA }

    private final Label titulo = new Label();
    private final Label resumen = FabricaComponentesUI.crearTexto("", "texto-secundario");
    private final GridPane cuadricula = new GridPane();
    private final VBox cuerpo = new VBox(16);
    private YearMonth mes = YearMonth.now();

    public VistaCalendario(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        titulo.getStyleClass().add("titulo-seccion");
        titulo.setMinWidth(210);
        Button anterior = FabricaComponentesUI.crearBotonIcono(Iconos.Icono.ATRAS, "Mes anterior", () -> cambiarMes(-1));
        Button siguiente = FabricaComponentesUI.crearBotonIcono(Iconos.Icono.FLECHA_DERECHA, "Mes siguiente",
                () -> cambiarMes(1));
        Button hoy = FabricaComponentesUI.crearEnlace("Hoy", () -> {
            mes = YearMonth.now();
            cargar();
        });
        HBox barra = new HBox(10, anterior, titulo, siguiente, hoy, FabricaComponentesUI.crearEspaciador(),
                leyenda("Ocupada", "calendario-ocupada"), leyenda("Bloqueada", "calendario-bloqueada"),
                leyenda("Libre", "calendario-libre"));
        barra.setAlignment(Pos.CENTER_LEFT);

        cuadricula.setHgap(2);
        cuadricula.setVgap(6);
        VBox tarjeta = FabricaComponentesUI.crearTarjeta(cuadricula);
        cuerpo.getChildren().setAll(tarjeta, resumen);
        cuerpo.setPadding(new Insets(0, 0, 16, 0));
        ScrollPane scroll = new ScrollPane(cuerpo);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return new VBox(16, barra, scroll);
    }

    @Override
    public void alMostrar() {
        cargar();
    }

    private void cambiarMes(int delta) {
        mes = mes.plusMonths(delta);
        cargar();
    }

    private void cargar() {
        String nombreMes = mes.getMonth().getDisplayName(TextStyle.FULL, ESPANOL);
        titulo.setText(Character.toUpperCase(nombreMes.charAt(0)) + nombreMes.substring(1) + " de " + mes.getYear());
        notificador().ejecutarConManejo(() -> pintar(conector().listarApartaSuites(),
                conector().reservasEntre(mes.atDay(1), mes.plusMonths(1).atDay(1))), null);
    }

    private void pintar(List<ApartaSuiteDTO> suites, List<ReservaDTO> reservas) {
        int dias = mes.lengthOfMonth();
        LocalDate hoy = LocalDate.now();
        cuadricula.getChildren().clear();
        cuadricula.getColumnConstraints().setAll(new ColumnConstraints(ANCHO_NOMBRES));
        for (int d = 1; d <= dias; d++) {
            ColumnConstraints columna = new ColumnConstraints(ANCHO_MIN_NOCHE, ANCHO_MIN_NOCHE, Double.MAX_VALUE);
            columna.setHgrow(Priority.ALWAYS);
            columna.setHalignment(HPos.CENTER);
            cuadricula.getColumnConstraints().add(columna);
            LocalDate fecha = mes.atDay(d);
            // Inicial del día de la semana sobre el número: "S 14".
            Label dia = new Label(fecha.getDayOfWeek().getDisplayName(TextStyle.NARROW, ESPANOL).toUpperCase(ESPANOL)
                    + "\n" + d);
            dia.getStyleClass().add("calendario-dia");
            if (fecha.equals(hoy)) {
                dia.getStyleClass().add("calendario-hoy");
            }
            cuadricula.add(dia, d, 0);
        }
        cuadricula.setMinWidth(ANCHO_NOMBRES + dias * (ANCHO_MIN_NOCHE + 2));

        if (suites.isEmpty()) {
            resumen.setText("Aún no hay aparta-suites registrados.");
            return;
        }
        int ocupadas = 0;
        int bloqueadas = 0;
        int fila = 1;
        for (ApartaSuiteDTO suite : suites) {
            Label nombre = new Label(suite.codigo() + " · " + suite.nombre());
            nombre.setMaxWidth(ANCHO_NOMBRES - 10); // nombres largos se cortan con "…"
            Tooltip.install(nombre, new Tooltip(suite.nombre() + " · " + suite.ubicacion()));
            cuadricula.add(nombre, 0, fila);
            for (int d = 1; d <= dias; d++) {
                LocalDate fecha = mes.atDay(d);
                ReservaDTO reserva = reservaDe(reservas, suite.codigo(), fecha);
                Noche estado = Noche.LIBRE;
                String motivo = null;
                if (reserva != null) {
                    estado = "PENDIENTE".equals(reserva.estado()) ? Noche.BLOQUEADA : Noche.OCUPADA;
                    motivo = reserva.codigo() + " · " + reserva.nombreTitular() + "\n"
                            + Formato.rango(reserva.entrada(), reserva.salida()) + " · " + Formato.estado(reserva.estado())
                            + (estado == Noche.BLOQUEADA ? "\nRetenida hasta que se pague el anticipo" : "");
                } else if (!suite.reservable() && !fecha.isBefore(hoy)) {
                    estado = Noche.BLOQUEADA;
                    motivo = "Fuera de servicio";
                }
                Region celda = new Region();
                celda.getStyleClass().addAll("calendario-celda", "calendario-" + estado.name().toLowerCase(Locale.ROOT));
                celda.setMaxWidth(Double.MAX_VALUE);
                String texto = suite.codigo() + ", " + Formato.fecha(fecha) + ": " + estado.name().toLowerCase(Locale.ROOT);
                celda.setAccessibleText(motivo == null ? texto : texto + ". " + motivo.replace('\n', ' '));
                if (motivo != null) {
                    Tooltip.install(celda, new Tooltip(Formato.fecha(fecha) + "\n" + motivo));
                }
                GridPane.setHgrow(celda, Priority.ALWAYS);
                cuadricula.add(celda, d, fila);
                ocupadas += estado == Noche.OCUPADA ? 1 : 0;
                bloqueadas += estado == Noche.BLOQUEADA ? 1 : 0;
            }
            fila++;
        }
        int libres = suites.size() * dias - ocupadas - bloqueadas;
        resumen.setText(Formato.plural(ocupadas, "noche ocupada", "noches ocupadas") + " · "
                + Formato.plural(bloqueadas, "bloqueada", "bloqueadas") + " · "
                + Formato.plural(libres, "libre", "libres") + " este mes.");
    }

    /** La noche del día {@code fecha} pertenece a la reserva si entrada ≤ fecha < salida (la de salida no se ocupa). */
    private static ReservaDTO reservaDe(List<ReservaDTO> reservas, String codigoSuite, LocalDate fecha) {
        for (ReservaDTO r : reservas) {
            if (r.codigoApartaSuite().equals(codigoSuite) && !fecha.isBefore(r.entrada()) && fecha.isBefore(r.salida())) {
                return r;
            }
        }
        return null;
    }

    private static HBox leyenda(String texto, String clase) {
        Region muestra = new Region();
        muestra.getStyleClass().addAll("calendario-celda", clase, "calendario-muestra");
        HBox caja = new HBox(6, muestra, FabricaComponentesUI.crearTexto(texto, "texto-secundario"));
        caja.setAlignment(Pos.CENTER_LEFT);
        return caja;
    }
}
