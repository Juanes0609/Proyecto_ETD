package gui.cliente;

import gui.componentes.FabricaComponentesUI;
import gui.componentes.FormularioFlexible;
import gui.integracion.dto.HuespedDTO;
import gui.integracion.dto.UsuarioDTO;
import gui.navegacion.VistaBase;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.Period;

/**
 * Datos personales del huésped. El documento y el correo (usuario de acceso)
 * son de solo lectura; el resto se puede actualizar.
 */
public class VistaPerfilCliente extends VistaBase {

    private final VentanaCliente ventana;
    private final TextField nombre = FabricaComponentesUI.crearCampoTexto("Nombres y apellidos");
    private final DatePicker nacimiento = FabricaComponentesUI.crearSelectorFecha("dd/mm/aaaa");
    private final TextField telefono = FabricaComponentesUI.crearCampoTexto("Ej. 300 123 4567");
    private final VBox contenido = new VBox(18);
    private HuespedDTO huesped;

    public VistaPerfilCliente(VentanaCliente ventana) {
        super(ventana);
        this.ventana = ventana;
    }

    @Override
    protected Node construir() {
        contenido.setPadding(new Insets(8, 0, 24, 0));
        contenido.setPrefWidth(560);
        contenido.setMaxWidth(560);
        HBox centrado = new HBox(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        return scroll;
    }

    @Override
    public void alMostrar() {
        try {
            huesped = conector().obtenerHuesped(usuario().documento());
        } catch (RuntimeException e) {
            notificador().ejecutarConManejo(() -> { throw e; }, null);
            return;
        }
        nombre.setText(huesped.nombre());
        nacimiento.setValue(huesped.fechaNacimiento());
        telefono.setText(huesped.telefono() == null ? "" : huesped.telefono());

        StackPane avatar = new StackPane(new Label(usuario().iniciales()));
        avatar.getStyleClass().add("avatar");
        avatar.setStyle("-fx-min-width: 56; -fx-min-height: 56; -fx-max-width: 56; -fx-max-height: 56; "
                + "-fx-background-radius: 28;");
        HBox cabecera = new HBox(14, avatar, new VBox(2,
                FabricaComponentesUI.crearTexto(huesped.nombre(), "titulo-seccion"),
                FabricaComponentesUI.crearTextoSecundario(usuario().correo())));
        cabecera.setAlignment(Pos.CENTER_LEFT);

        FormularioFlexible formulario = new FormularioFlexible()
                .agregarDatoSoloLectura("Documento", huesped.documento())
                .agregarDatoSoloLectura("Correo electrónico", usuario().correo())
                .agregarCampo("Nombre completo", nombre, null)
                .conValidacion("Nombre completo", c -> nombre.getText().trim().contains(" ")
                        ? null : "Escribe nombre y apellido.")
                .agregarCampo("Fecha de nacimiento", nacimiento, null)
                .conValidacion("Fecha de nacimiento", c -> nacimiento.getValue() != null
                        && Period.between(nacimiento.getValue(), LocalDate.now()).getYears() >= 18
                        ? null : "Debes ser mayor de edad.")
                .agregarCampoOpcional("Teléfono", telefono, "Lo usamos solo para avisos de tu reserva.");

        Button guardar = FabricaComponentesUI.crearBotonPrimario("Guardar cambios", () -> guardar(formulario));
        contenido.getChildren().setAll(FabricaComponentesUI.crearTituloPagina("Perfil",
                "Tus datos personales para las reservas."), FabricaComponentesUI.crearTarjeta(cabecera),
                FabricaComponentesUI.crearTarjeta(formulario, FabricaComponentesUI.crearBarraAcciones(guardar)));
    }

    private void guardar(FormularioFlexible formulario) {
        if (!formulario.validar()) {
            return;
        }
        HuespedDTO actualizado = new HuespedDTO(huesped.documento(), nombre.getText().trim(), nacimiento.getValue(),
                telefono.getText().isBlank() ? null : telefono.getText().trim(), huesped.correo());
        if (notificador().ejecutarConManejo(() -> conector().actualizarHuesped(actualizado), "Tus datos se guardaron.")) {
            UsuarioDTO u = usuario();
            ventana.actualizarUsuario(new UsuarioDTO(u.correo(), actualizado.nombre(), u.rol(), u.documento()));
            huesped = actualizado;
        }
    }
}
