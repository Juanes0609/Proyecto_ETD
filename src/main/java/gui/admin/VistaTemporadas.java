package gui.admin;

import gui.VentanaPrincipalSGA;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.FormularioFlexible;
import gui.componentes.Formato;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.TemporadaDTO;
import gui.navegacion.VistaBase;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Temporadas y tarifas: fechas de cada temporada (baja, media, alta) y tarifas
 * por persona y noche de cada aparta-suite. Las fechas sin temporada definida se
 * cobran como temporada baja.
 */
public class VistaTemporadas extends VistaBase {

    private final TableView<TemporadaDTO> temporadas = FabricaComponentesUI.crearTabla("Sin temporadas",
            "Sin temporadas definidas todas las noches se cobran como temporada baja.");
    private final TableView<ApartaSuiteDTO> tarifas = FabricaComponentesUI.crearTabla("Sin aparta-suites",
            "Registra aparta-suites en Catálogos para definir sus tarifas.");

    public VistaTemporadas(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        FabricaComponentesUI.agregarColumnaEstado(temporadas, "Temporada", TemporadaDTO::tipo);
        FabricaComponentesUI.agregarColumna(temporadas, "Inicio", t -> Formato.fecha(t.inicio()));
        FabricaComponentesUI.agregarColumna(temporadas, "Fin", t -> Formato.fecha(t.fin()));
        FabricaComponentesUI.agregarColumnaNumero(temporadas, "Días", t -> String.valueOf(
                ChronoUnit.DAYS.between(t.inicio(), t.fin()) + 1));

        FabricaComponentesUI.agregarColumna(tarifas, "Aparta-suite", s -> s.codigo() + " · " + s.nombre());
        FabricaComponentesUI.agregarColumnaNumero(tarifas, "Baja", s -> Formato.moneda(s.tarifaBaja()));
        FabricaComponentesUI.agregarColumnaNumero(tarifas, "Media", s -> Formato.moneda(s.tarifaMedia()));
        FabricaComponentesUI.agregarColumnaNumero(tarifas, "Alta", s -> Formato.moneda(s.tarifaAlta()));
        tarifas.setOnMouseClicked(e -> {
            ApartaSuiteDTO s = tarifas.getSelectionModel().getSelectedItem();
            if (s != null) {
                editarTarifas(s);
            }
        });

        HBox encabezadoIzquierdo = new HBox(16, FabricaComponentesUI.crearEncabezado("Temporadas",
                "Las noches sin temporada se cobran como baja."), FabricaComponentesUI.crearEspaciador(),
                FabricaComponentesUI.crearBotonPrimario("Nueva temporada", this::nuevaTemporada));
        encabezadoIzquierdo.setAlignment(Pos.BOTTOM_LEFT);
        Node encabezadoDerecho = FabricaComponentesUI.crearEncabezado("Tarifas por persona y noche",
                "Haz clic en un aparta-suite para editar sus tarifas.");

        // Rejilla de 2×2: los encabezados comparten fila y las tablas también, así ambas
        // tablas empiezan y terminan a la misma altura aunque un encabezado sea más alto.
        GridPane columnas = new GridPane();
        columnas.setHgap(24);
        columnas.setVgap(12);
        ColumnConstraints colIzquierda = new ColumnConstraints();
        colIzquierda.setPercentWidth(44);
        ColumnConstraints colDerecha = new ColumnConstraints();
        colDerecha.setPercentWidth(56);
        columnas.getColumnConstraints().setAll(colIzquierda, colDerecha);
        RowConstraints filaEncabezados = new RowConstraints();
        filaEncabezados.setValignment(VPos.BOTTOM);
        RowConstraints filaTablas = new RowConstraints();
        filaTablas.setVgrow(Priority.ALWAYS);
        filaTablas.setFillHeight(true);
        columnas.getRowConstraints().setAll(filaEncabezados, filaTablas);
        columnas.add(encabezadoIzquierdo, 0, 0);
        columnas.add(encabezadoDerecho, 1, 0);
        columnas.add(temporadas, 0, 1);
        columnas.add(tarifas, 1, 1);
        temporadas.setMaxHeight(Double.MAX_VALUE);
        tarifas.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(columnas, Priority.ALWAYS);
        return new VBox(columnas);
    }

    @Override
    public void alMostrar() {
        notificador().ejecutarConManejo(() -> {
            temporadas.getItems().setAll(conector().listarTemporadas());
            tarifas.getItems().setAll(conector().listarApartaSuites().stream()
                    .sorted(Comparator.comparing(ApartaSuiteDTO::codigo)).toList());
        }, null);
    }

    private void nuevaTemporada() {
        ComboBox<String> tipo = FabricaComponentesUI.crearDesplegable("Elige el tipo");
        tipo.getItems().setAll(List.of("BAJA", "MEDIA", "ALTA"));
        DatePicker inicio = FabricaComponentesUI.crearSelectorFecha("dd/mm/aaaa");
        DatePicker fin = FabricaComponentesUI.crearSelectorFecha("dd/mm/aaaa");
        FormularioFlexible f = new FormularioFlexible()
                .agregarCampo("Tipo", tipo, null)
                .agregarCampo("Inicio", inicio, null)
                .agregarCampo("Fin", fin, "No puede cruzarse con otra temporada.")
                .conValidacion("Fin", c -> inicio.getValue() != null && fin.getValue() != null
                        && !fin.getValue().isAfter(inicio.getValue()) ? "Debe ser posterior al inicio." : null);
        Button guardar = FabricaComponentesUI.crearBotonPrimario("Guardar temporada", () -> {
            if (f.validar() && notificador().ejecutarConManejo(() -> conector().agregarTemporada(
                    new TemporadaDTO(tipo.getValue(), inicio.getValue(), fin.getValue())), "Temporada guardada.")) {
                ocultarHoja();
                alMostrar();
            }
        });
        guardar.setMaxWidth(Double.MAX_VALUE);
        f.agregarNodo(guardar);
        mostrarHoja("Nueva temporada", f);
    }

    private void editarTarifas(ApartaSuiteDTO s) {
        TextField baja = campo(s.tarifaBaja());
        TextField media = campo(s.tarifaMedia());
        TextField alta = campo(s.tarifaAlta());
        FormularioFlexible f = new FormularioFlexible()
                .agregarDatoSoloLectura("Aparta-suite", s.codigo() + " · " + s.nombre())
                .agregarCampo("Temporada baja", baja, "Por persona y noche, en pesos.")
                .conValidacion("Temporada baja", validarMonto(baja))
                .agregarCampo("Temporada media", media, null).conValidacion("Temporada media", validarMonto(media))
                .agregarCampo("Temporada alta", alta, null).conValidacion("Temporada alta", validarMonto(alta));
        Button guardar = FabricaComponentesUI.crearBotonPrimario("Guardar tarifas", () -> {
            if (f.validar() && notificador().ejecutarConManejo(() -> conector().actualizarTarifas(s.codigo(),
                            Double.parseDouble(baja.getText().trim()), Double.parseDouble(media.getText().trim()),
                            Double.parseDouble(alta.getText().trim())),
                    "Tarifas de " + s.nombre() + " actualizadas.")) {
                ocultarHoja();
                alMostrar();
            }
        });
        guardar.setMaxWidth(Double.MAX_VALUE);
        f.agregarNodo(guardar);
        mostrarHoja("Tarifas", f);
    }

    private static TextField campo(double valor) {
        TextField campo = FabricaComponentesUI.crearCampoTexto("Ej. 90000");
        if (valor > 0) {
            campo.setText(String.valueOf(Math.round(valor)));
        }
        return campo;
    }

    private static java.util.function.Function<javafx.scene.control.Control, String> validarMonto(TextField campo) {
        return c -> campo.getText().trim().matches("\\d{4,9}") ? null : "Escribe un valor en pesos, sin puntos.";
    }
}
