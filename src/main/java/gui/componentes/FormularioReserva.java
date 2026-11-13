package gui.componentes;

import gui.integracion.ConectorSGA;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.CotizacionDTO;
import gui.integracion.dto.HuespedDTO;
import gui.integracion.dto.OcupanteDTO;
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
 * "Mini menú" para solicitar una reserva, compartido por el huésped (canal PORTAL)
 * y la recepción (canal DIRECTA).
 * <p>
 * Los datos del aparta-suite se muestran en solo lectura: aquí únicamente se
 * diligencian los datos de quien reserva y de sus acompañantes. El total se
 * recalcula en vivo con las fechas de nacimiento (los menores de 6 años no pagan).
 */
public class FormularioReserva extends VBox {

    private final ConectorSGA conector;
    private final ApartaSuiteDTO suite;
    private final String canal;
    private final Consumer<ReservaDTO> alConfirmar;

    private final DatePicker entrada;
    private final DatePicker salida;
    private final FormularioFlexible formularioEstancia = new FormularioFlexible();
    private final FormularioFlexible formularioTitular = new FormularioFlexible();
    private final VBox listaAcompanantes = new VBox(12);
    private final List<FormularioFlexible> acompanantes = new ArrayList<>();
    private final Button agregarAcompanante;
    private final Label lblTotal = new Label();
    private final Label lblDetalleTotal = new Label();
    private final VBox error = new VBox();

    /**
     * @param fechasFijas  si es {@code true}, las fechas vienen del detalle y no se editan aquí
     * @param personas     número inicial de personas (titular + acompañantes)
     * @param titular      datos para prellenar al titular (huésped en sesión), o {@code null}
     */
    public FormularioReserva(ConectorSGA conector, ApartaSuiteDTO suite, LocalDate fechaEntrada, LocalDate fechaSalida,
                             boolean fechasFijas, int personas, HuespedDTO titular, String canal,
                             Consumer<ReservaDTO> alConfirmar) {
        this.conector = conector;
        this.suite = suite;
        this.canal = canal;
        this.alConfirmar = alConfirmar;
        setSpacing(18);

        // ---- Aparta-suite (solo lectura) y estancia ----
        entrada = FabricaComponentesUI.crearSelectorFecha("dd/mm/aaaa");
        salida = FabricaComponentesUI.crearSelectorFecha("dd/mm/aaaa");
        entrada.setValue(fechaEntrada);
        salida.setValue(fechaSalida);
        formularioEstancia.agregarDatoSoloLectura("Aparta-suite", suite.nombre() + " · " + suite.ubicacion()
                + " · hasta " + suite.capacidad() + " personas");
        if (fechasFijas) {
            formularioEstancia.agregarDatoSoloLectura("Estancia", Formato.rango(fechaEntrada, fechaSalida) + " · "
                    + Formato.plural(fechaSalida.toEpochDay() - fechaEntrada.toEpochDay(), "noche", "noches"));
        } else {
            formularioEstancia
                    .agregarCampo("Entrada", entrada, null)
                    .agregarCampo("Salida", salida, null)
                    .conValidacion("Salida", c -> entrada.getValue() != null && salida.getValue() != null
                            && !salida.getValue().isAfter(entrada.getValue())
                            ? "La salida debe ser posterior a la entrada." : null);
            entrada.valueProperty().addListener((o, a, b) -> recalcular());
            salida.valueProperty().addListener((o, a, b) -> recalcular());
        }

        // ---- Titular ----
        TextField documento = FabricaComponentesUI.crearCampoTexto("Ej. 1094123456");
        TextField nombre = FabricaComponentesUI.crearCampoTexto("Nombres y apellidos");
        DatePicker nacimiento = FabricaComponentesUI.crearSelectorFecha("dd/mm/aaaa");
        TextField telefono = FabricaComponentesUI.crearCampoTexto("Ej. 300 123 4567");
        TextField correo = FabricaComponentesUI.crearCampoTexto("nombre@correo.com");
        formularioTitular
                .agregarCampo("Documento", documento, titular == null
                        ? "Si el huésped ya está registrado, sus datos se completan solos." : null)
                .conValidacion("Documento", c -> documento.getText().trim().matches("\\d{5,12}")
                        ? null : "Usa solo números (entre 5 y 12 dígitos).")
                .agregarCampo("Nombre completo", nombre, null)
                .agregarCampo("Fecha de nacimiento", nacimiento, null)
                .conValidacion("Fecha de nacimiento", c -> nacimiento.getValue() != null
                        && Period.between(nacimiento.getValue(), LocalDate.now()).getYears() >= 18
                        ? null : "Quien reserva debe ser mayor de edad.")
                .agregarCampoOpcional("Teléfono", telefono, null)
                .agregarCampoOpcional("Correo electrónico", correo, null);
        if (titular != null) {
            documento.setText(titular.documento());
            documento.setEditable(false);
            nombre.setText(titular.nombre());
            nacimiento.setValue(titular.fechaNacimiento());
            telefono.setText(titular.telefono() == null ? "" : titular.telefono());
            correo.setText(titular.correo() == null ? "" : titular.correo());
        } else {
            documento.focusedProperty().addListener((o, antes, enfocado) -> {
                if (!enfocado) {
                    autocompletarTitular(documento, nombre, nacimiento, telefono, correo);
                }
            });
        }
        nacimiento.valueProperty().addListener((o, a, b) -> recalcular());

        // ---- Acompañantes ----
        agregarAcompanante = FabricaComponentesUI.conIcono(
                FabricaComponentesUI.crearEnlace("Agregar acompañante", this::agregarAcompanante), Iconos.Icono.MAS);
        for (int i = 1; i < personas; i++) {
            agregarAcompanante();
        }
        HBox ayudaAcompanantes = FabricaComponentesUI.crearTextoConIcono(Iconos.Icono.INFO,
                "Hasta " + suite.capacidad() + " personas en total. Los menores de 6 años ocupan cupo pero no pagan.",
                "campo-ayuda");
        ayudaAcompanantes.getStyleClass().add("campo-mensaje");

        // ---- Hora de llegada ----
        TextField hora = FabricaComponentesUI.crearCampoTexto("HH:mm · Ej. 15:30");
        FormularioFlexible formularioLlegada = new FormularioFlexible()
                .agregarCampoOpcional("Hora estimada de llegada", hora, null)
                .conValidacion("Hora estimada de llegada", c -> horaValida(hora.getText()) ? null
                        : "Usa el formato de 24 horas, por ejemplo 15:30.");

        // ---- Total y acciones ----
        lblTotal.getStyleClass().add("suite-precio");
        lblDetalleTotal.getStyleClass().add("texto-secundario");
        lblDetalleTotal.setWrapText(true);
        VBox resumen = FabricaComponentesUI.crearTarjeta(FabricaComponentesUI.crearTexto("Total estimado",
                "campo-label"), lblTotal, lblDetalleTotal);
        error.setVisible(false);
        error.setManaged(false);

        Button confirmar = FabricaComponentesUI.crearBotonPrimario("Confirmar reserva", () ->
                confirmar(documento, nombre, nacimiento, telefono, correo, hora));
        confirmar.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(
                formularioEstancia,
                seccion("Quién reserva"), formularioTitular,
                seccion("Acompañantes"), listaAcompanantes, ayudaAcompanantes, agregarAcompanante,
                FabricaComponentesUI.crearSeparador(), formularioLlegada,
                resumen, error, confirmar);
        recalcular();
    }

    private void agregarAcompanante() {
        if (1 + acompanantes.size() >= suite.capacidad()) {
            return;
        }
        int numero = acompanantes.size() + 1;
        TextField nombre = FabricaComponentesUI.crearCampoTexto("Nombres y apellidos");
        TextField documento = FabricaComponentesUI.crearCampoTexto("Ej. 1094123456");
        DatePicker nacimiento = FabricaComponentesUI.crearSelectorFecha("dd/mm/aaaa");
        nacimiento.valueProperty().addListener((o, a, b) -> recalcular());

        FormularioFlexible formulario = new FormularioFlexible()
                .agregarCampo("Nombre completo", nombre, null)
                .agregarCampoOpcional("Documento", documento, "Los menores pueden no tener documento.")
                .agregarCampo("Fecha de nacimiento", nacimiento, null);

        Label titulo = FabricaComponentesUI.crearTexto("Acompañante " + numero, "encabezado-vista");
        VBox bloque = new VBox(10);
        Button quitar = FabricaComponentesUI.crearBotonIcono(Iconos.Icono.CERRAR, "Quitar acompañante " + numero, () -> {
            listaAcompanantes.getChildren().remove(bloque);
            acompanantes.remove(formulario);
            renumerar();
            recalcular();
        });
        HBox encabezado = new HBox(titulo, FabricaComponentesUI.crearEspaciador(), quitar);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        bloque.getChildren().addAll(encabezado, formulario);
        bloque.getStyleClass().add("tarjeta");
        bloque.setUserData(titulo);

        acompanantes.add(formulario);
        listaAcompanantes.getChildren().add(bloque);
        recalcular();
    }

    private void renumerar() {
        for (int i = 0; i < listaAcompanantes.getChildren().size(); i++) {
            ((Label) listaAcompanantes.getChildren().get(i).getUserData()).setText("Acompañante " + (i + 1));
        }
    }

    /** Si el documento ya pertenece a un huésped registrado, completa sus datos. */
    private void autocompletarTitular(TextField documento, TextField nombre, DatePicker nacimiento,
                                      TextField telefono, TextField correo) {
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
            // Huésped nuevo: se registrará al confirmar la reserva.
        }
    }

    private List<OcupanteDTO> leerAcompanantes() {
        List<OcupanteDTO> lista = new ArrayList<>();
        for (FormularioFlexible f : acompanantes) {
            DatePicker nacimiento = f.obtenerControl("Fecha de nacimiento");
            String documento = f.texto("Documento");
            lista.add(new OcupanteDTO(f.texto("Nombre completo"), documento.isEmpty() ? null : documento,
                    nacimiento.getValue()));
        }
        return lista;
    }

    private void recalcular() {
        agregarAcompanante.setDisable(1 + acompanantes.size() >= suite.capacidad());
        LocalDate e = entrada.getValue();
        LocalDate s = salida.getValue();
        int personas = 1 + acompanantes.size();
        if (e == null || s == null || !s.isAfter(e)) {
            lblTotal.setText("—");
            lblDetalleTotal.setText("Elige las fechas para calcular el valor.");
            return;
        }
        int quePagan = 1 + (int) leerAcompanantes().stream().filter(o -> o.paga(e)).count();
        try {
            CotizacionDTO cotizacion = conector.cotizar(suite.codigo(), e, s, quePagan);
            lblTotal.setText(Formato.moneda(cotizacion.total()) + " COP");
            String detalle = Formato.plural(cotizacion.numeroNoches(), "noche", "noches") + " · "
                    + Formato.plural(personas, "persona", "personas") + " ("
                    + Formato.plural(quePagan, "paga", "pagan") + ")";
            if ("PORTAL".equals(canal)) {
                detalle += ". Para confirmarla pagas un anticipo de "
                        + Formato.moneda(Math.round(cotizacion.total() * 0.30)) + " (30 %).";
            }
            lblDetalleTotal.setText(detalle);
        } catch (RuntimeException ex) {
            lblTotal.setText("—");
            lblDetalleTotal.setText(NotificadorUI.mensajeDe(ex));
        }
    }

    private void confirmar(TextField documento, TextField nombre, DatePicker nacimiento, TextField telefono,
                           TextField correo, TextField hora) {
        error.setVisible(false);
        error.setManaged(false);
        boolean valido = formularioEstancia.validar() & formularioTitular.validar();
        for (FormularioFlexible f : acompanantes) {
            valido &= f.validar();
        }
        if (!valido || !horaValida(hora.getText())) {
            mostrarError("Revisa los datos marcados.");
            return;
        }
        HuespedDTO titular = new HuespedDTO(documento.getText().trim(), nombre.getText().trim(), nacimiento.getValue(),
                vacioANulo(telefono.getText()), vacioANulo(correo.getText()));
        LocalTime llegada = hora.getText().isBlank() ? null : LocalTime.parse(hora.getText().trim());
        try {
            alConfirmar.accept(conector.crearReserva(suite.codigo(), entrada.getValue(), salida.getValue(), titular,
                    leerAcompanantes(), canal, llegada));
        } catch (RuntimeException ex) {
            mostrarError(NotificadorUI.mensajeDe(ex));
        }
    }

    private void mostrarError(String mensaje) {
        error.getChildren().setAll(FabricaComponentesUI.crearErrorInline(mensaje));
        error.setVisible(true);
        error.setManaged(true);
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
