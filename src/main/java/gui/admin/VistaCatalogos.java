package gui.admin;

import gui.VentanaPrincipalSGA;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.FormularioFlexible;
import gui.componentes.Formato;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.EmpleadoDTO;
import gui.integracion.dto.HuespedDTO;
import gui.navegacion.VistaBase;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Gestión jerárquica: aparta-suites, huéspedes y empleados, cada catálogo
 * ordenado por su clave (recorrido inorden del árbol binario de búsqueda).
 * Crear y editar se hace en el mini menú lateral. Solo el administrador puede
 * cambiar los datos del alojamiento.
 */
public class VistaCatalogos extends VistaBase {

    private final TextField buscador = FabricaComponentesUI.crearCampoTexto("Buscar");
    private final Button nuevo = FabricaComponentesUI.crearBotonPrimario("Nuevo", null);
    private final StackPane cuerpo = new StackPane();
    private final TableView<ApartaSuiteDTO> tablaSuites = FabricaComponentesUI.crearTabla("Sin aparta-suites",
            "Registra el primer aparta-suite del complejo.");
    private final TableView<HuespedDTO> tablaHuespedes = FabricaComponentesUI.crearTabla("Sin huéspedes",
            "Los huéspedes registrados aparecerán aquí.");
    private final TableView<EmpleadoDTO> tablaEmpleados = FabricaComponentesUI.crearTabla("Sin empleados",
            "Registra a los recepcionistas del alojamiento.");
    private List<ApartaSuiteDTO> suites = List.of();
    private List<HuespedDTO> huespedes = List.of();
    private List<EmpleadoDTO> empleados = List.of();
    private int segmento;

    public VistaCatalogos(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        FabricaComponentesUI.agregarColumna(tablaSuites, "Código", ApartaSuiteDTO::codigo);
        FabricaComponentesUI.agregarColumna(tablaSuites, "Nombre", ApartaSuiteDTO::nombre);
        FabricaComponentesUI.agregarColumna(tablaSuites, "Ubicación", ApartaSuiteDTO::ubicacion);
        FabricaComponentesUI.agregarColumna(tablaSuites, "Dormitorios", s -> String.valueOf(s.dormitorios()));
        FabricaComponentesUI.agregarColumna(tablaSuites, "Capacidad", s -> String.valueOf(s.capacidad()));
        FabricaComponentesUI.agregarColumnaEstado(tablaSuites, "Estado", ApartaSuiteDTO::estadoOperativo);
        alAbrirFila(tablaSuites, this::formularioSuite);

        FabricaComponentesUI.agregarColumna(tablaHuespedes, "Documento", HuespedDTO::documento);
        FabricaComponentesUI.agregarColumna(tablaHuespedes, "Nombre", HuespedDTO::nombre);
        FabricaComponentesUI.agregarColumna(tablaHuespedes, "Nacimiento", h -> Formato.fecha(h.fechaNacimiento()));
        FabricaComponentesUI.agregarColumna(tablaHuespedes, "Teléfono", h -> valor(h.telefono()));
        FabricaComponentesUI.agregarColumna(tablaHuespedes, "Correo", h -> valor(h.correo()));
        alAbrirFila(tablaHuespedes, this::formularioHuesped);

        FabricaComponentesUI.agregarColumna(tablaEmpleados, "Documento", EmpleadoDTO::documento);
        FabricaComponentesUI.agregarColumna(tablaEmpleados, "Nombre", EmpleadoDTO::nombre);
        FabricaComponentesUI.agregarColumna(tablaEmpleados, "Turno", EmpleadoDTO::turno);

        buscador.setPrefColumnCount(26);
        buscador.textProperty().addListener((o, a, b) -> filtrar());
        HBox segmentado = FabricaComponentesUI.crearSegmentado(List.of("Aparta-suites", "Huéspedes", "Empleados"), 0,
                indice -> {
                    segmento = indice;
                    ocultarHoja();
                    buscador.clear();
                    mostrarSegmento();
                });
        HBox barra = FabricaComponentesUI.crearBarraFiltros(segmentado, FabricaComponentesUI.crearEspaciador(),
                buscador, nuevo);
        barra.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        // La tabla ocupa todo el alto disponible: así ninguna fila queda cortada a la mitad.
        javafx.scene.layout.VBox.setVgrow(cuerpo, javafx.scene.layout.Priority.ALWAYS);
        return new VBox(16, barra, FabricaComponentesUI.crearTextoSecundario("Haz clic en una fila para editarla."),
                cuerpo);
    }

    @Override
    public void alMostrar() {
        notificador().ejecutarConManejo(() -> {
            suites = conector().listarApartaSuites().stream()
                    .sorted(java.util.Comparator.comparing(ApartaSuiteDTO::codigo)).toList();
            huespedes = conector().listarHuespedes();
            empleados = conector().listarEmpleados();
        }, null);
        mostrarSegmento();
    }

    private void mostrarSegmento() {
        switch (segmento) {
            case 0 -> {
                nuevo.setText("Nuevo aparta-suite");
                nuevo.setOnAction(e -> formularioSuite(null));
                cuerpo.getChildren().setAll(tablaSuites);
            }
            case 1 -> {
                nuevo.setText("Nuevo huésped");
                nuevo.setOnAction(e -> formularioHuesped(null));
                cuerpo.getChildren().setAll(tablaHuespedes);
            }
            default -> {
                nuevo.setText("Nuevo empleado");
                nuevo.setOnAction(e -> formularioEmpleado());
                cuerpo.getChildren().setAll(tablaEmpleados);
            }
        }
        filtrar();
    }

    private void filtrar() {
        String t = buscador.getText().trim().toLowerCase(Locale.ROOT);
        tablaSuites.getItems().setAll(suites.stream().filter(s -> contiene(t, s.codigo(), s.nombre(), s.bloque())).toList());
        tablaHuespedes.getItems().setAll(huespedes.stream().filter(h -> contiene(t, h.documento(), h.nombre())).toList());
        tablaEmpleados.getItems().setAll(empleados.stream().filter(e -> contiene(t, e.documento(), e.nombre())).toList());
    }

    // ---------- Formularios ----------

    private void formularioSuite(ApartaSuiteDTO existente) {
        TextField codigo = FabricaComponentesUI.crearCampoTexto("Ej. D-101");
        TextField nombre = FabricaComponentesUI.crearCampoTexto("Ej. Suite Salento");
        TextField bloque = FabricaComponentesUI.crearCampoTexto("Ej. D");
        TextField piso = FabricaComponentesUI.crearCampoTexto("Ej. 1");
        TextField dormitorios = FabricaComponentesUI.crearCampoTexto("Ej. 2");
        TextField capacidad = FabricaComponentesUI.crearCampoTexto("Ej. 4");
        TextField caracteristicas = FabricaComponentesUI.crearCampoTexto("Ej. Balcón, Cocineta, Wi-Fi");
        TextArea descripcion = FabricaComponentesUI.crearAreaTexto("Una frase que describa el aparta-suite");

        FormularioFlexible f = new FormularioFlexible();
        if (existente == null) {
            f.agregarCampo("Código", codigo, "Bloque y número, por ejemplo D-101.")
                    .conValidacion("Código", c -> codigo.getText().trim().matches("[A-Za-z]-\\d{3}")
                            ? null : "Usa el formato letra-guion-tres dígitos (D-101).");
        } else {
            f.agregarDatoSoloLectura("Código", existente.codigo());
        }
        f.agregarCampo("Nombre", nombre, null)
                .agregarCampo("Bloque", bloque, null)
                .agregarCampo("Piso", piso, null).conValidacion("Piso", entero(piso, 1, 20))
                .agregarCampo("Dormitorios", dormitorios, null).conValidacion("Dormitorios", entero(dormitorios, 1, 10))
                .agregarCampo("Capacidad máxima", capacidad, "Personas en total, incluidos niños.")
                .conValidacion("Capacidad máxima", entero(capacidad, 1, 20))
                .agregarCampoOpcional("Características", caracteristicas, "Sepáralas con comas.")
                .agregarCampoOpcional("Descripción", descripcion, null);
        if (existente != null) {
            nombre.setText(existente.nombre());
            bloque.setText(existente.bloque());
            piso.setText(String.valueOf(existente.piso()));
            dormitorios.setText(String.valueOf(existente.dormitorios()));
            capacidad.setText(String.valueOf(existente.capacidad()));
            caracteristicas.setText(String.join(", ", existente.caracteristicas()));
            descripcion.setText(existente.descripcion());
        } else {
            f.agregarNodo(FabricaComponentesUI.crearTextoSecundario(
                    "Las tarifas se definen en Temporadas y tarifas."));
        }
        Button guardar = FabricaComponentesUI.crearBotonPrimario("Guardar aparta-suite", () -> {
            if (!f.validar()) {
                return;
            }
            String cod = existente == null ? codigo.getText().trim().toUpperCase(Locale.ROOT) : existente.codigo();
            ApartaSuiteDTO dto = new ApartaSuiteDTO(cod, nombre.getText().trim(), bloque.getText().trim().toUpperCase(),
                    Integer.parseInt(piso.getText().trim()), Integer.parseInt(dormitorios.getText().trim()),
                    Integer.parseInt(capacidad.getText().trim()),
                    existente == null ? 0 : existente.tarifaBaja(), existente == null ? 0 : existente.tarifaMedia(),
                    existente == null ? 0 : existente.tarifaAlta(),
                    Arrays.stream(caracteristicas.getText().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList(),
                    descripcion.getText().trim(), existente == null ? "PREPARADO" : existente.estadoOperativo(), null);
            guardar(() -> conector().guardarApartaSuite(dto), dto.nombre() + " se guardó en el catálogo.");
        });
        guardar.setMaxWidth(Double.MAX_VALUE);
        f.agregarNodo(guardar);
        mostrarHoja(existente == null ? "Nuevo aparta-suite" : "Editar " + existente.nombre(), f);
    }

    private void formularioHuesped(HuespedDTO existente) {
        TextField documento = FabricaComponentesUI.crearCampoTexto("Ej. 1094123456");
        TextField nombre = FabricaComponentesUI.crearCampoTexto("Nombres y apellidos");
        DatePicker nacimiento = FabricaComponentesUI.crearSelectorFecha("dd/mm/aaaa");
        TextField telefono = FabricaComponentesUI.crearCampoTexto("Ej. 300 123 4567");
        TextField correo = FabricaComponentesUI.crearCampoTexto("nombre@correo.com");
        FormularioFlexible f = new FormularioFlexible();
        if (existente == null) {
            f.agregarCampo("Documento", documento, "Sin puntos ni espacios.")
                    .conValidacion("Documento", c -> documento.getText().trim().matches("\\d{5,12}")
                            ? null : "Usa solo números (entre 5 y 12 dígitos).");
        } else {
            f.agregarDatoSoloLectura("Documento", existente.documento());
            nombre.setText(existente.nombre());
            nacimiento.setValue(existente.fechaNacimiento());
            telefono.setText(valorVacio(existente.telefono()));
            correo.setText(valorVacio(existente.correo()));
        }
        f.agregarCampo("Nombre completo", nombre, null)
                .agregarCampo("Fecha de nacimiento", nacimiento, null)
                .agregarCampoOpcional("Teléfono", telefono, null)
                .agregarCampoOpcional("Correo electrónico", correo, null);
        Button guardar = FabricaComponentesUI.crearBotonPrimario("Guardar huésped", () -> {
            if (!f.validar()) {
                return;
            }
            HuespedDTO dto = new HuespedDTO(existente == null ? documento.getText().trim() : existente.documento(),
                    nombre.getText().trim(), nacimiento.getValue(), nuloSiVacio(telefono.getText()),
                    nuloSiVacio(correo.getText()));
            guardar(() -> {
                if (existente == null) {
                    conector().registrarHuesped(dto);
                } else {
                    conector().actualizarHuesped(dto);
                }
            }, dto.nombre() + " se guardó en el catálogo de huéspedes.");
        });
        guardar.setMaxWidth(Double.MAX_VALUE);
        f.agregarNodo(guardar);
        mostrarHoja(existente == null ? "Nuevo huésped" : "Editar huésped", f);
    }

    private void formularioEmpleado() {
        TextField documento = FabricaComponentesUI.crearCampoTexto("Ej. 41900004");
        TextField nombre = FabricaComponentesUI.crearCampoTexto("Nombres y apellidos");
        ComboBox<String> turno = FabricaComponentesUI.crearDesplegable("Elige un turno");
        turno.getItems().setAll("06:00 – 14:00", "14:00 – 22:00", "22:00 – 06:00");
        FormularioFlexible f = new FormularioFlexible()
                .agregarCampo("Documento", documento, null)
                .conValidacion("Documento", c -> documento.getText().trim().matches("\\d{5,12}")
                        ? null : "Usa solo números (entre 5 y 12 dígitos).")
                .agregarCampo("Nombre completo", nombre, null)
                .agregarCampo("Turno", turno, "Los turnos rotan entre los recepcionistas registrados.");
        Button guardar = FabricaComponentesUI.crearBotonPrimario("Guardar empleado", () -> {
            if (f.validar()) {
                EmpleadoDTO dto = new EmpleadoDTO(documento.getText().trim(), nombre.getText().trim(), turno.getValue());
                guardar(() -> conector().registrarEmpleado(dto), dto.nombre() + " se registró como recepcionista.");
            }
        });
        guardar.setMaxWidth(Double.MAX_VALUE);
        f.agregarNodo(guardar);
        mostrarHoja("Nuevo empleado", f);
    }

    // ---------- Apoyo ----------

    private void guardar(Runnable accion, String exito) {
        if (notificador().ejecutarConManejo(accion, exito)) {
            ocultarHoja();
            alMostrar();
        }
    }

    private static <T> void alAbrirFila(TableView<T> tabla, java.util.function.Consumer<T> abrir) {
        tabla.setOnMouseClicked(e -> {
            T fila = tabla.getSelectionModel().getSelectedItem();
            if (fila != null) {
                abrir.accept(fila);
            }
        });
        tabla.setOnKeyPressed(e -> {
            T fila = tabla.getSelectionModel().getSelectedItem();
            if (e.getCode() == KeyCode.ENTER && fila != null) {
                abrir.accept(fila);
            }
        });
    }

    private static Function<javafx.scene.control.Control, String> entero(TextField campo, int min, int max) {
        return c -> {
            String t = campo.getText().trim();
            if (!t.matches("\\d{1,3}") || Integer.parseInt(t) < min || Integer.parseInt(t) > max) {
                return "Escribe un número entre " + min + " y " + max + ".";
            }
            return null;
        };
    }

    private static boolean contiene(String texto, String... campos) {
        if (texto.isEmpty()) {
            return true;
        }
        for (String c : campos) {
            if (c != null && c.toLowerCase(Locale.ROOT).contains(texto)) {
                return true;
            }
        }
        return false;
    }

    private static String valor(String texto) {
        return texto == null || texto.isBlank() ? "—" : texto;
    }

    private static String valorVacio(String texto) {
        return texto == null ? "" : texto;
    }

    private static String nuloSiVacio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
