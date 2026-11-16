package gui.componentes;

import gui.integracion.ConectorSGA;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.HuespedDTO;
import gui.integracion.dto.ReservaDTO;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * "Mini menú" para reservar a un grupo que no cabe en un solo aparta-suite.
 * <p>
 * La combinación de aparta-suites la calcula el núcleo (backtracking: la que
 * usa menos aparta-suites); aquí se muestra cómo queda repartido el grupo, se
 * diligencian los datos de quien reserva y se crea una reserva por aparta-suite.
 */
public class FormularioReservaGrupo extends VBox {

    private final VBox error = new VBox();

    /**
     * @param combinacion aparta-suites asignados, de mayor a menor capacidad
     * @param personas    tamaño total del grupo
     */
    public FormularioReservaGrupo(ConectorSGA conector, List<ApartaSuiteDTO> combinacion, LocalDate entrada,
                                  LocalDate salida, int personas, String canal,
                                  Consumer<List<ReservaDTO>> alConfirmar) {
        setSpacing(18);
        List<Integer> reparto = repartir(combinacion, personas);
        long noches = salida.toEpochDay() - entrada.toEpochDay();

        // ---- Estancia y reparto (solo lectura) ----
        FormularioFlexible estancia = new FormularioFlexible()
                .agregarDatoSoloLectura("Estancia", Formato.rango(entrada, salida) + " · "
                        + Formato.plural(noches, "noche", "noches"))
                .agregarDatoSoloLectura("Grupo", Formato.plural(personas, "persona", "personas") + " en "
                        + Formato.plural(combinacion.size(), "aparta-suite", "aparta-suites"));
        VBox filas = new VBox(10);
        double total = 0;
        for (int i = 0; i < combinacion.size(); i++) {
            ApartaSuiteDTO suite = combinacion.get(i);
            double valor = cotizar(conector, suite, entrada, salida, reparto.get(i));
            total += valor;
            filas.getChildren().add(filaSuite(suite, reparto.get(i), valor, i == 0));
        }
        VBox tarjetaReparto = FabricaComponentesUI.crearTarjeta(filas);

        // ---- Titular ----
        TextField documento = FabricaComponentesUI.crearCampoTexto("Ej. 1094123456");
        TextField nombre = FabricaComponentesUI.crearCampoTexto("Nombres y apellidos");
        DatePicker nacimiento = FabricaComponentesUI.crearSelectorFecha("dd/mm/aaaa");
        TextField telefono = FabricaComponentesUI.crearCampoTexto("Ej. 300 123 4567");
        TextField correo = FabricaComponentesUI.crearCampoTexto("nombre@correo.com");
        FormularioFlexible titular = new FormularioFlexible()
                .agregarCampo("Documento", documento, "Si el huésped ya está registrado, sus datos se completan solos.")
                .conValidacion("Documento", c -> documento.getText().trim().matches("\\d{5,12}")
                        ? null : "Usa solo números (entre 5 y 12 dígitos).")
                .agregarCampo("Nombre completo", nombre, null)
                .agregarCampo("Fecha de nacimiento", nacimiento, null)
                .conValidacion("Fecha de nacimiento", c -> nacimiento.getValue() != null
                        && Period.between(nacimiento.getValue(), LocalDate.now()).getYears() >= 18
                        ? null : "Quien reserva debe ser mayor de edad.")
                .agregarCampoOpcional("Teléfono", telefono, null)
                .agregarCampoOpcional("Correo electrónico", correo, null);
        documento.focusedProperty().addListener((o, antes, enfocado) -> {
            if (!enfocado) {
                autocompletar(conector, documento, nombre, nacimiento, telefono, correo);
            }
        });

        // ---- Hora de llegada ----
        TextField hora = FabricaComponentesUI.crearCampoTexto("HH:mm · Ej. 15:30");
        FormularioFlexible llegada = new FormularioFlexible()
                .agregarCampoOpcional("Hora estimada de llegada", hora, null)
                .conValidacion("Hora estimada de llegada", c -> horaValida(hora.getText()) ? null
                        : "Usa el formato de 24 horas, por ejemplo 15:30.");

        // ---- Total y confirmación ----
        Label lblTotal = new Label(Formato.moneda(total) + " COP");
        lblTotal.getStyleClass().add("suite-precio");
        Label lblDetalle = new Label("Una reserva por aparta-suite, todas a nombre de quien reserva. "
                + "Todos los integrantes se cobran como adultos.");
        lblDetalle.getStyleClass().add("texto-secundario");
        lblDetalle.setWrapText(true);
        VBox resumen = FabricaComponentesUI.crearTarjeta(FabricaComponentesUI.crearTexto("Total estimado",
                "campo-label"), lblTotal, lblDetalle);
        error.setVisible(false);
        error.setManaged(false);

        Button confirmar = FabricaComponentesUI.crearBotonPrimario("Reservar "
                + Formato.plural(combinacion.size(), "aparta-suite", "aparta-suites"), () -> {
            ocultarError();
            if (!(titular.validar() & llegada.validar())) {
                mostrarError("Revisa los datos marcados.");
                return;
            }
            HuespedDTO datos = new HuespedDTO(documento.getText().trim(), nombre.getText().trim(),
                    nacimiento.getValue(), vacioANulo(telefono.getText()), vacioANulo(correo.getText()));
            LocalTime horaLlegada = hora.getText().isBlank() ? null : LocalTime.parse(hora.getText().trim());
            try {
                alConfirmar.accept(conector.crearReservasGrupo(
                        combinacion.stream().map(ApartaSuiteDTO::codigo).toList(), reparto,
                        entrada, salida, datos, canal, horaLlegada));
            } catch (RuntimeException ex) {
                mostrarError(NotificadorUI.mensajeDe(ex));
            }
        });
        confirmar.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(estancia, seccion("Reparto del grupo"), tarjetaReparto,
                seccion("Quién reserva"), titular, FabricaComponentesUI.crearSeparador(), llegada,
                resumen, error, confirmar);
    }

    /** Llena los aparta-suites en orden (el primero es el más grande y lleva al titular). */
    static List<Integer> repartir(List<ApartaSuiteDTO> combinacion, int personas) {
        List<Integer> reparto = new ArrayList<>();
        int faltan = personas;
        for (ApartaSuiteDTO suite : combinacion) {
            int aqui = Math.min(suite.capacidad(), faltan);
            reparto.add(aqui);
            faltan -= aqui;
        }
        // Si el último quedó vacío (no debería con la combinación mínima), pasa una persona del anterior.
        for (int i = reparto.size() - 1; i > 0 && reparto.get(i) == 0; i--) {
            reparto.set(i, 1);
            reparto.set(i - 1, reparto.get(i - 1) - 1);
        }
        return reparto;
    }

    private static HBox filaSuite(ApartaSuiteDTO suite, int personas, double valor, boolean conTitular) {
        Label nombre = new Label(suite.codigo() + " · " + suite.nombre());
        nombre.getStyleClass().add("menu-fila-titulo");
        Label detalle = new Label(Formato.plural(personas, "persona", "personas") + " de "
                + suite.capacidad() + (conTitular ? " · con el titular" : ""));
        detalle.getStyleClass().add("texto-terciario");
        Label precio = new Label(Formato.moneda(valor));
        precio.getStyleClass().add("texto-secundario");
        HBox fila = new HBox(10, new VBox(2, nombre, detalle), FabricaComponentesUI.crearEspaciador(), precio);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private static double cotizar(ConectorSGA conector, ApartaSuiteDTO suite, LocalDate entrada, LocalDate salida,
                                  int personas) {
        try {
            return conector.cotizar(suite.codigo(), entrada, salida, personas).total();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static void autocompletar(ConectorSGA conector, TextField documento, TextField nombre,
                                      DatePicker nacimiento, TextField telefono, TextField correo) {
        String doc = documento.getText().trim();
        if (doc.isEmpty() || !nombre.getText().isBlank()) {
            return;
        }
        try {
            HuespedDTO existente = conector.obtenerHuesped(doc);
            nombre.setText(existente.nombre());
            nacimiento.setValue(existente.fechaNacimiento());
            telefono.setText(existente.telefono() == null ? "" : existente.telefono());
            correo.setText(existente.correo() == null ? "" : existente.correo());
        } catch (RuntimeException e) {
            // Huésped nuevo: se registra al confirmar.
        }
    }

    private void mostrarError(String mensaje) {
        error.getChildren().setAll(FabricaComponentesUI.crearErrorInline(mensaje));
        error.setVisible(true);
        error.setManaged(true);
    }

    private void ocultarError() {
        error.setVisible(false);
        error.setManaged(false);
    }

    private static boolean horaValida(String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        try {
            LocalTime.parse(texto.trim());
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static Label seccion(String titulo) {
        Label lbl = new Label(titulo);
        lbl.getStyleClass().add("encabezado-vista");
        return lbl;
    }
}
