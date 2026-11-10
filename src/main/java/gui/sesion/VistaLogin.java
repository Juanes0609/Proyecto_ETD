package gui.sesion;

import gui.MarcaApp;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.FormularioFlexible;
import gui.componentes.Iconos;
import gui.integracion.ConectorSGA;
import gui.integracion.ConectorSGADemo;
import gui.integracion.dto.HuespedDTO;
import gui.integracion.dto.Rol;
import gui.integracion.dto.UsuarioDTO;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.Period;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Inicio de sesión único para los tres roles (referencia de diseño: tarjeta
 * centrada con logo, saludo, correo, contraseña con "ver", acción primaria y
 * enlace para crear cuenta).
 * <p>
 * La ventana mide lo mismo que la tarjeta (sin fondo extra ni marca repetida:
 * el logo ya va en el encabezado). Al cambiar de tarjeta la ventana se ajusta.
 * <p>
 * El rol se <b>detecta</b> a partir del correo: en cuanto se reconoce, aparece la
 * pastilla "Ingresarás como …" y, al entrar, se abre la experiencia de ese rol.
 * "Crear cuenta" registra huéspedes en dos pasos dentro de la misma tarjeta.
 */
public class VistaLogin extends StackPane {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ConectorSGA conector;
    private final Consumer<UsuarioDTO> alIngresar;

    public VistaLogin(ConectorSGA conector, Consumer<UsuarioDTO> alIngresar) {
        this.conector = conector;
        this.alIngresar = alIngresar;
        getStyleClass().add("login-fondo");
        mostrarTarjeta(crearTarjetaIngreso());
    }

    // ---------- Ingreso ----------

    private VBox crearTarjetaIngreso() {
        TextField correo = FabricaComponentesUI.crearCampoTexto("nombre@correo.com");
        PasswordField clave = new PasswordField();
        clave.setPromptText("Tu contraseña");
        TextField claveVisible = FabricaComponentesUI.crearCampoTexto("Tu contraseña");

        HBox rolDetectado = new HBox(6);
        rolDetectado.getStyleClass().add("rol-detectado");
        rolDetectado.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        ocultar(rolDetectado);
        correo.textProperty().addListener((obs, antes, ahora) -> actualizarRol(rolDetectado, ahora));

        FormularioFlexible formulario = new FormularioFlexible()
                .agregarCampo("Correo electrónico", correo, null)
                .conValidacion("Correo electrónico", c -> CORREO.matcher(correo.getText().trim()).matches()
                        ? null : "Escribe un correo válido, por ejemplo nombre@correo.com.")
                .agregarNodo(rolDetectado)
                .agregarCampoCompuesto("Contraseña", clave, campoConVer(clave, claveVisible), null)
                .vincularFoco("Contraseña", claveVisible);

        VBox error = new VBox();
        ocultar(error);

        Button entrar = FabricaComponentesUI.crearBotonPrimario("Entrar", null);
        entrar.setMaxWidth(Double.MAX_VALUE);
        entrar.setDefaultButton(true);
        entrar.setOnAction(e -> {
            ocultar(error);
            if (!formulario.validar()) {
                return;
            }
            try {
                alIngresar.accept(conector.iniciarSesion(correo.getText().trim(), clave.getText()));
            } catch (IllegalArgumentException ex) {
                mostrarError(error, ex.getMessage());
                clave.requestFocus();
            } catch (UnsupportedOperationException ex) {
                mostrarError(error, "El inicio de sesión aún no está disponible en el servicio.");
            }
        });

        HBox crearCuenta = new HBox(2, FabricaComponentesUI.crearTextoSecundario("¿Nuevo en " + MarcaApp.NOMBRE + "?"),
                FabricaComponentesUI.crearEnlace("Crear cuenta", () -> mostrarTarjeta(crearTarjetaRegistroPaso1())));
        crearCuenta.setAlignment(Pos.CENTER);

        VBox tarjeta = tarjeta(encabezado("Hola de nuevo", "Inicia sesión para continuar en " + MarcaApp.NOMBRE),
                formulario, error, entrar, crearCuenta);
        if (conector.esDemo()) {
            tarjeta.getChildren().addAll(FabricaComponentesUI.crearSeparador(), FabricaComponentesUI.crearTexto(
                    "Modo demostración · huesped@demo.co, recepcion@demo.co o admin@demo.co · clave "
                            + ConectorSGADemo.CLAVE_DEMO, "texto-terciario"));
        }
        javafx.application.Platform.runLater(correo::requestFocus);
        return tarjeta;
    }

    /** Pastilla con el rol reconocido para el correo escrito. */
    private void actualizarRol(HBox pastilla, String correo) {
        conector.detectarRol(correo).ifPresentOrElse(rol -> {
            pastilla.getChildren().setAll(Iconos.crear(iconoDe(rol), 16), new Label("Ingresarás como " + rol.getEtiqueta()));
            if (!pastilla.isVisible()) {
                mostrarConFundido(pastilla);
            }
        }, () -> ocultar(pastilla));
    }

    private static Iconos.Icono iconoDe(Rol rol) {
        return switch (rol) {
            case HUESPED -> Iconos.Icono.PERSONA;
            case RECEPCIONISTA -> Iconos.Icono.CALENDARIO;
            case ADMINISTRADOR -> Iconos.Icono.GRAFICA;
        };
    }

    // ---------- Registro de huésped (2 pasos) ----------

    private VBox crearTarjetaRegistroPaso1() {
        TextField documento = FabricaComponentesUI.crearCampoTexto("Ej. 1094123456");
        TextField nombre = FabricaComponentesUI.crearCampoTexto("Nombres y apellidos");
        DatePicker nacimiento = FabricaComponentesUI.crearSelectorFecha("dd/mm/aaaa");
        TextField telefono = FabricaComponentesUI.crearCampoTexto("Ej. 300 123 4567");

        FormularioFlexible formulario = new FormularioFlexible()
                .agregarCampo("Documento", documento, "Sin puntos ni espacios.")
                .conValidacion("Documento", c -> documento.getText().trim().matches("\\d{5,12}")
                        ? null : "Usa solo números (entre 5 y 12 dígitos).")
                .agregarCampo("Nombre completo", nombre, null)
                .conValidacion("Nombre completo", c -> nombre.getText().trim().contains(" ")
                        ? null : "Escribe nombre y apellido.")
                .agregarCampo("Fecha de nacimiento", nacimiento, "El titular de una reserva debe ser mayor de edad.")
                .conValidacion("Fecha de nacimiento", c -> nacimiento.getValue() != null
                        && Period.between(nacimiento.getValue(), LocalDate.now()).getYears() >= 18
                        ? null : "Debes tener al menos 18 años.")
                .agregarCampoOpcional("Teléfono", telefono, null);

        Button continuar = FabricaComponentesUI.crearBotonPrimario("Continuar", null);
        continuar.setMaxWidth(Double.MAX_VALUE);
        continuar.setDefaultButton(true);
        continuar.setOnAction(e -> {
            if (formulario.validar()) {
                HuespedDTO parcial = new HuespedDTO(documento.getText().trim(), nombre.getText().trim(),
                        nacimiento.getValue(), telefono.getText().trim(), null);
                mostrarTarjeta(crearTarjetaRegistroPaso2(parcial));
            }
        });

        Button volver = FabricaComponentesUI.conIcono(FabricaComponentesUI.crearEnlace("Ya tengo cuenta",
                () -> mostrarTarjeta(crearTarjetaIngreso())), Iconos.Icono.ATRAS);

        return tarjeta(paso("Paso 1 de 2 · Datos personales"), encabezado("Crea tu cuenta",
                "Para reservar y guardar tus aparta-suites favoritos."), formulario, continuar, centrado(volver));
    }

    private VBox crearTarjetaRegistroPaso2(HuespedDTO parcial) {
        TextField correo = FabricaComponentesUI.crearCampoTexto("nombre@correo.com");
        PasswordField clave = new PasswordField();
        clave.setPromptText("Mínimo 4 caracteres");
        PasswordField confirmacion = new PasswordField();
        confirmacion.setPromptText("Repite la contraseña");

        FormularioFlexible formulario = new FormularioFlexible()
                .agregarCampo("Correo electrónico", correo, "Lo usarás para iniciar sesión.")
                .conValidacion("Correo electrónico", c -> CORREO.matcher(correo.getText().trim()).matches()
                        ? null : "Escribe un correo válido, por ejemplo nombre@correo.com.")
                .agregarCampo("Contraseña", clave, null)
                .conValidacion("Contraseña", c -> clave.getText().length() >= 4 ? null : "Usa al menos 4 caracteres.")
                .agregarCampo("Confirmar contraseña", confirmacion, null)
                .conValidacion("Confirmar contraseña", c -> confirmacion.getText().equals(clave.getText())
                        ? null : "Las contraseñas no coinciden.");

        VBox error = new VBox();
        ocultar(error);

        Button crear = FabricaComponentesUI.crearBotonPrimario("Crear cuenta", null);
        crear.setMaxWidth(Double.MAX_VALUE);
        crear.setDefaultButton(true);
        crear.setOnAction(e -> {
            ocultar(error);
            if (!formulario.validar()) {
                return;
            }
            HuespedDTO huesped = new HuespedDTO(parcial.documento(), parcial.nombre(), parcial.fechaNacimiento(),
                    parcial.telefono(), correo.getText().trim());
            try {
                alIngresar.accept(conector.registrarCuentaHuesped(huesped, clave.getText()));
            } catch (IllegalArgumentException ex) {
                mostrarError(error, ex.getMessage());
            } catch (UnsupportedOperationException ex) {
                mostrarError(error, "El registro aún no está disponible en el servicio.");
            }
        });

        Button atras = FabricaComponentesUI.conIcono(FabricaComponentesUI.crearEnlace("Atrás",
                () -> mostrarTarjeta(crearTarjetaRegistroPaso1())), Iconos.Icono.ATRAS);

        return tarjeta(paso("Paso 2 de 2 · Acceso"), encabezado("Crea tu cuenta", "Hola, " + primerNombre(parcial.nombre())
                + ". Solo falta tu acceso."), formulario, error, crear, centrado(atras));
    }

    // ---------- Apoyo visual ----------

    private void mostrarTarjeta(VBox tarjeta) {
        getChildren().setAll(tarjeta);
        mostrarConFundido(tarjeta);
        ajustarVentana();
    }

    /** La ventana toma el alto de la tarjeta activa (ingreso o registro). */
    private void ajustarVentana() {
        if (getScene() != null && getScene().getWindow() != null) {
            applyCss();
            getScene().getWindow().sizeToScene();
        }
    }

    private static VBox tarjeta(Node... contenido) {
        VBox tarjeta = new VBox(contenido);
        tarjeta.getStyleClass().add("login-card");
        tarjeta.setPrefWidth(420);
        return tarjeta;
    }

    private static HBox encabezado(String titulo, String subtitulo) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("login-titulo");
        Label lblSubtitulo = FabricaComponentesUI.crearTextoSecundario(subtitulo);
        VBox textos = new VBox(2, lblTitulo, lblSubtitulo);
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox fila = new HBox(14, MarcaApp.crearLogo(44), textos);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private static Label paso(String texto) {
        return FabricaComponentesUI.crearTexto(texto, "texto-terciario");
    }

    private static HBox centrado(Node nodo) {
        HBox fila = new HBox(nodo);
        fila.setAlignment(Pos.CENTER);
        return fila;
    }

    /** Contraseña con botón "ver": alterna entre campo oculto y visible conservando el texto. */
    private static StackPane campoConVer(PasswordField oculto, TextField visible) {
        visible.textProperty().bindBidirectional(oculto.textProperty());
        visible.setVisible(false);
        visible.setManaged(false);
        Button ver = new Button();
        ver.getStyleClass().add("boton-icono");
        ver.setGraphic(Iconos.crear(Iconos.Icono.OJO, 18));
        ver.setAccessibleText("Mostrar contraseña");
        ver.setFocusTraversable(false);
        ver.setOnAction(e -> {
            boolean mostrar = !visible.isVisible();
            visible.setVisible(mostrar);
            visible.setManaged(mostrar);
            oculto.setVisible(!mostrar);
            oculto.setManaged(!mostrar);
            ver.setGraphic(Iconos.crear(mostrar ? Iconos.Icono.OJO_TACHADO : Iconos.Icono.OJO, 18));
            ver.setAccessibleText(mostrar ? "Ocultar contraseña" : "Mostrar contraseña");
            (mostrar ? visible : oculto).requestFocus();
            (mostrar ? visible : oculto).end();
        });
        oculto.setPadding(new Insets(9, 44, 9, 12));
        visible.setPadding(new Insets(9, 44, 9, 12));
        StackPane caja = new StackPane(oculto, visible, ver);
        caja.getStyleClass().add("campo-con-boton");
        StackPane.setAlignment(ver, Pos.CENTER_RIGHT);
        StackPane.setMargin(ver, new Insets(0, 6, 0, 0));
        return caja;
    }

    private static void mostrarError(VBox contenedor, String mensaje) {
        contenedor.getChildren().setAll(FabricaComponentesUI.crearErrorInline(mensaje));
        mostrarConFundido(contenedor);
    }

    private static void mostrarConFundido(Node nodo) {
        nodo.setVisible(true);
        nodo.setManaged(true);
        nodo.setOpacity(0);
        FadeTransition fundido = new FadeTransition(Duration.millis(160), nodo);
        fundido.setToValue(1);
        fundido.play();
    }

    private static void ocultar(Node nodo) {
        nodo.setVisible(false);
        nodo.setManaged(false);
    }

    private static String primerNombre(String nombre) {
        return nombre.trim().split("\\s+")[0];
    }
}
