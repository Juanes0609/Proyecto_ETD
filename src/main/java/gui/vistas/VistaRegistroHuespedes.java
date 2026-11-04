package gui.vistas;

import gui.VentanaPrincipalSGA;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.FormularioFlexible;
import gui.navegacion.VistaBase;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/**
 * Vista 4 — Catálogo de huéspedes con buscador por documento (izquierda),
 * formulario de registro y pestañas de favoritos e historial (derecha).
 * <p>
 * Las tablas usan {@code Object} como tipo provisional hasta que existan
 * {@code modelo.Huesped}, {@code modelo.Apartamento} y {@code modelo.Reserva}.
 */
public class VistaRegistroHuespedes extends VistaBase {

    private final TextField buscador = new TextField();
    private final TableView<Object> tablaHuespedes = FabricaComponentesUI.crearTabla(
            "Sin huéspedes registrados",
            "Los huéspedes registrados aparecerán aquí, ordenados por documento.");
    private final TableView<Object> tablaFavoritos = FabricaComponentesUI.crearTabla(
            "Sin favoritos", "Selecciona un huésped para ver sus apartamentos favoritos.");
    private final TableView<Object> tablaHistorial = FabricaComponentesUI.crearTabla(
            "Sin estancias", "Selecciona un huésped para ver su historial de estancias.");
    private final FormularioFlexible formulario = new FormularioFlexible();

    public VistaRegistroHuespedes(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        configurarTablas();
        configurarFormulario();

        buscador.setPromptText("Buscar por documento");
        buscador.setAccessibleText("Buscar huésped por documento");
        buscador.setOnAction(e -> buscar());

        VBox maestro = new VBox(12,
                FabricaComponentesUI.crearBarraFiltros(buscador,
                        FabricaComponentesUI.crearBotonSecundario("Buscar", this::buscar)),
                tablaHuespedes);

        ScrollPane scrollFormulario = new ScrollPane(formulario);
        scrollFormulario.setFitToWidth(true);
        scrollFormulario.getStyleClass().add("scroll-transparente");

        TabPane pestanas = new TabPane(
                crearPestana("Registro", scrollFormulario),
                crearPestana("Favoritos", tablaFavoritos),
                crearPestana("Historial", tablaHistorial));
        VBox.setVgrow(pestanas, Priority.ALWAYS);

        VBox detalle = FabricaComponentesUI.crearTarjeta(pestanas);
        detalle.setMinWidth(340);

        SplitPane split = new SplitPane(maestro, detalle);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.55);
        return split;
    }

    private void configurarTablas() {
        FabricaComponentesUI.agregarColumna(tablaHuespedes, "Documento", fila -> "");
        FabricaComponentesUI.agregarColumna(tablaHuespedes, "Nombre", fila -> "");
        FabricaComponentesUI.agregarColumna(tablaHuespedes, "Teléfono", fila -> "");
        FabricaComponentesUI.agregarColumna(tablaHuespedes, "Correo", fila -> "");

        FabricaComponentesUI.agregarColumna(tablaFavoritos, "Código", fila -> "");
        FabricaComponentesUI.agregarColumna(tablaFavoritos, "Apartamento", fila -> "");

        FabricaComponentesUI.agregarColumna(tablaHistorial, "Reserva", fila -> "");
        FabricaComponentesUI.agregarColumna(tablaHistorial, "Apartamento", fila -> "");
        FabricaComponentesUI.agregarColumna(tablaHistorial, "Entrada", fila -> "");
        FabricaComponentesUI.agregarColumna(tablaHistorial, "Salida", fila -> "");
        FabricaComponentesUI.agregarColumna(tablaHistorial, "Estado", fila -> "");
    }

    private void configurarFormulario() {
        DatePicker fechaNacimiento = new DatePicker();
        fechaNacimiento.setPromptText("dd/mm/aaaa");
        fechaNacimiento.setDayCellFactory(selector -> new DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setDisable(vacio || fecha.isAfter(LocalDate.now()));
            }
        });

        formulario
                .agregarCampoObligatorio("Documento", FabricaComponentesUI.crearCampoTexto("Número de documento"), null)
                .agregarCampoObligatorio("Nombre completo", FabricaComponentesUI.crearCampoTexto("Nombres y apellidos"), null)
                .agregarCampoObligatorio("Fecha de nacimiento", fechaNacimiento, null)
                .agregarCampo("Teléfono", FabricaComponentesUI.crearCampoTexto("Ej. 300 123 4567"), null)
                .agregarCampo("Correo", FabricaComponentesUI.crearCampoTexto("nombre@correo.com"), null)
                .agregarNodo(FabricaComponentesUI.crearBarraAcciones(
                        FabricaComponentesUI.crearBotonPrimario("Guardar huésped", this::guardar),
                        FabricaComponentesUI.crearBotonSecundario("Limpiar", formulario::limpiar)));
    }

    private Tab crearPestana(String titulo, Node contenido) {
        Tab pestana = new Tab(titulo, contenido);
        pestana.setClosable(false);
        return pestana;
    }

    private void buscar() {
        if (buscador.getText().isBlank()) {
            notificador().mostrarAdvertencia("Escribe un documento para buscar.");
            return;
        }
        notificador().mostrarInfo("Pendiente de integración: la búsqueda en el catálogo de huéspedes aún no está conectada.");
    }

    private void guardar() {
        List<String> faltantes = formulario.validarObligatorios();
        if (!faltantes.isEmpty()) {
            notificador().mostrarAdvertencia("Completa los campos obligatorios: " + String.join(", ", faltantes) + ".");
            return;
        }
        notificador().mostrarInfo("Pendiente de integración con el servicio: el huésped aún no se guarda.");
    }
}
