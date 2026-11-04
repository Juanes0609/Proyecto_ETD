package gui.vistas;

import gui.VentanaPrincipalSGA;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.FormularioFlexible;
import gui.navegacion.VistaBase;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Vista 3 — Catálogo de apartamentos (izquierda) y formulario de registro (derecha).
 * <p>
 * La tabla usa {@code Object} como tipo de fila provisional; al implementarse
 * {@code modelo.Apartamento} se cambia el tipo y los extractores de cada columna.
 */
public class VistaRegistroApartamentos extends VistaBase {

    private final TableView<Object> tabla = FabricaComponentesUI.crearTabla(
            "Sin apartamentos registrados",
            "Los apartamentos guardados aparecerán aquí, ordenados por código.");
    private final FormularioFlexible formulario = new FormularioFlexible();

    public VistaRegistroApartamentos(VentanaPrincipalSGA ventana) {
        super(ventana);
    }

    @Override
    protected Node construir() {
        configurarTabla();
        configurarFormulario();

        VBox maestro = new VBox(12,
                FabricaComponentesUI.crearEncabezado("Catálogo", "Selecciona un apartamento para ver su ficha."),
                tabla);

        ScrollPane scrollFormulario = new ScrollPane(formulario);
        scrollFormulario.setFitToWidth(true);
        scrollFormulario.getStyleClass().add("scroll-transparente");
        VBox detalle = FabricaComponentesUI.crearTarjeta(
                FabricaComponentesUI.crearEncabezado("Nuevo apartamento", "Los campos con * son obligatorios."),
                scrollFormulario);
        detalle.setMinWidth(320);

        SplitPane split = new SplitPane(maestro, detalle);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.6);
        return split;
    }

    private void configurarTabla() {
        FabricaComponentesUI.agregarColumna(tabla, "Código", fila -> "");
        FabricaComponentesUI.agregarColumna(tabla, "Nombre", fila -> "");
        FabricaComponentesUI.agregarColumna(tabla, "Bloque", fila -> "");
        FabricaComponentesUI.agregarColumna(tabla, "Piso", fila -> "");
        FabricaComponentesUI.agregarColumna(tabla, "Capacidad", fila -> "");
        FabricaComponentesUI.agregarColumna(tabla, "Estado", fila -> "");
    }

    private void configurarFormulario() {
        formulario
                .agregarSeccion("Identificación")
                .agregarCampoObligatorio("Código", FabricaComponentesUI.crearCampoTexto("Ej. A-101"), null)
                .agregarCampoObligatorio("Nombre", FabricaComponentesUI.crearCampoTexto("Ej. Suite Cocora"), null)
                .agregarCampoObligatorio("Bloque", FabricaComponentesUI.crearCampoTexto("Ej. A"), null)
                .agregarCampoObligatorio("Piso", FabricaComponentesUI.crearCampoTexto("Ej. 1"), null)
                .agregarSeccion("Capacidad")
                .agregarCampoObligatorio("Dormitorios", FabricaComponentesUI.crearCampoTexto("Ej. 2"), null)
                .agregarCampoObligatorio("Capacidad máxima", FabricaComponentesUI.crearCampoTexto("Ej. 4"),
                        "Número máximo de personas, incluidos menores de 6 años.")
                .agregarSeccion("Tarifas por noche y persona")
                .agregarCampoObligatorio("Tarifa temporada baja", FabricaComponentesUI.crearCampoTexto("Ej. 80000"), null)
                .agregarCampoObligatorio("Tarifa temporada media", FabricaComponentesUI.crearCampoTexto("Ej. 95000"), null)
                .agregarCampoObligatorio("Tarifa temporada alta", FabricaComponentesUI.crearCampoTexto("Ej. 120000"), null)
                .agregarSeccion("Descripción")
                .agregarCampo("Características", FabricaComponentesUI.crearAreaTexto("Vista, balcón, cocina..."), null)
                .agregarNodo(FabricaComponentesUI.crearBarraAcciones(
                        FabricaComponentesUI.crearBotonPrimario("Guardar apartamento", this::guardar),
                        FabricaComponentesUI.crearBotonSecundario("Limpiar", formulario::limpiar)));
    }

    private void guardar() {
        List<String> faltantes = formulario.validarObligatorios();
        if (!faltantes.isEmpty()) {
            notificador().mostrarAdvertencia("Completa los campos obligatorios: " + String.join(", ", faltantes) + ".");
            return;
        }
        notificador().mostrarInfo("Pendiente de integración con el servicio: el apartamento aún no se guarda.");
    }
}
