package gui.componentes;

import gui.integracion.dto.ApartaSuiteDTO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Card de un aparta-suite (referencia de diseño: foto arriba, nombre, ubicación,
 * datos clave, separador, precio grande con su unidad, nota de cancelación y
 * acción primaria a la derecha).
 * <p>
 * Toda la card abre el detalle; el corazón (solo para huéspedes) guarda el
 * favorito sin abrirlo. Los datos del aparta-suite son de solo lectura.
 */
public class TarjetaApartaSuite extends VBox {

    public static final double ANCHO = 280;
    private static final double ALTO_FOTO = 160;

    /**
     * @param tarifaVigente      tarifa por persona y noche de la temporada actual
     * @param temporadaVigente   BAJA, MEDIA o ALTA
     * @param textoAccion        texto del botón primario ("Reservar")
     * @param alAbrir            abrir el detalle o el formulario de reserva
     * @param favorito           estado inicial del corazón, o {@code null} para no mostrarlo
     * @param alAlternarFavorito recibe el nuevo estado del corazón
     */
    public TarjetaApartaSuite(ApartaSuiteDTO suite, double tarifaVigente, String temporadaVigente,
                              String textoAccion, Runnable alAbrir,
                              Boolean favorito, Consumer<Boolean> alAlternarFavorito) {
        getStyleClass().add("tarjeta-suite");
        setPrefWidth(ANCHO);
        setMaxWidth(ANCHO);
        setFocusTraversable(true);
        setAccessibleText(suite.nombre() + ", " + Formato.moneda(tarifaVigente) + " por persona por noche");

        StackPane foto = crearFoto(suite);
        if (favorito != null) {
            foto.getChildren().add(crearBotonFavorito(suite, favorito, alAlternarFavorito));
        }

        Label nombre = new Label(suite.nombre());
        nombre.getStyleClass().add("suite-nombre");
        nombre.setWrapText(true);
        Label ubicacion = FabricaComponentesUI.crearTextoSecundario(suite.ubicacion());
        Label datos = FabricaComponentesUI.crearTexto(Formato.plural(suite.dormitorios(), "dormitorio", "dormitorios")
                + " · hasta " + Formato.plural(suite.capacidad(), "huésped", "huéspedes"));
        VBox.setMargin(datos, new Insets(6, 0, 10, 0));

        Label precio = new Label(Formato.moneda(tarifaVigente));
        precio.getStyleClass().add("suite-precio");
        precio.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Label unidad = FabricaComponentesUI.crearTexto("COP por persona / noche", "texto-secundario");
        HBox cancelacion = FabricaComponentesUI.crearTextoConIcono(Iconos.Icono.CHECK,
                "Cancelación gratuita", "texto-exito");
        VBox bloquePrecio = new VBox(2, precio, unidad, cancelacion);
        HBox.setHgrow(bloquePrecio, Priority.ALWAYS);

        Button accion = FabricaComponentesUI.crearBotonPrimario(textoAccion, alAbrir);
        accion.setOnMouseClicked(javafx.event.Event::consume); // evita abrir dos veces (botón + card)
        accion.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        HBox filaInferior = new HBox(12, bloquePrecio, accion);
        filaInferior.setAlignment(Pos.BOTTOM_LEFT);
        VBox.setMargin(filaInferior, new Insets(12, 0, 0, 0));

        VBox cuerpo = new VBox(nombre, ubicacion, datos, FabricaComponentesUI.crearSeparador(), filaInferior);
        cuerpo.getStyleClass().add("tarjeta-suite-cuerpo");
        getChildren().addAll(foto, cuerpo);

        setOnMouseClicked(e -> alAbrir.run());
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                alAbrir.run();
            }
        });
    }

    /** Foto del aparta-suite; si aún no hay imagen se reserva el espacio con un fondo por bloque. */
    public static StackPane crearFoto(ApartaSuiteDTO suite, double ancho, double alto, boolean esquinasCompletas) {
        StackPane foto = new StackPane();
        foto.getStyleClass().addAll("suite-foto", "suite-foto-" + suite.bloque().toLowerCase());
        foto.setMinSize(ancho, alto);
        foto.setPrefSize(ancho, alto);
        foto.setMaxSize(Double.MAX_VALUE, alto);
        if (esquinasCompletas) {
            foto.setStyle("-fx-background-radius: 18;");
        }

        Image imagen = cargarImagen(suite.rutaImagen());
        if (imagen != null) {
            ImageView vista = new ImageView(imagen);
            vista.setFitWidth(ancho);
            vista.setFitHeight(alto);
            vista.setPreserveRatio(false);
            Rectangle recorte = new Rectangle(ancho, alto);
            recorte.setArcWidth(36);
            recorte.setArcHeight(36);
            vista.setClip(recorte);
            foto.getChildren().add(vista);
        } else {
            foto.getChildren().add(Iconos.crear(Iconos.Icono.CAMA, 44));
            Label codigo = new Label(suite.codigo());
            codigo.getStyleClass().add("suite-foto-codigo");
            StackPane.setAlignment(codigo, Pos.BOTTOM_LEFT);
            StackPane.setMargin(codigo, new Insets(0, 0, 12, 16));
            foto.getChildren().add(codigo);
        }
        return foto;
    }

    private static StackPane crearFoto(ApartaSuiteDTO suite) {
        return crearFoto(suite, ANCHO, ALTO_FOTO, false);
    }

    /** Corazón para guardar el favorito; no abre el detalle. */
    public static ToggleButton crearBotonFavorito(ApartaSuiteDTO suite, boolean favorito,
                                                  Consumer<Boolean> alAlternar) {
        StackPane icono = Iconos.crear(Iconos.Icono.CORAZON, 20);
        ToggleButton boton = new ToggleButton();
        boton.setGraphic(icono);
        boton.getStyleClass().add("boton-favorito");
        boton.setSelected(favorito);
        Iconos.setRelleno(icono, favorito);
        actualizarTextoFavorito(boton, suite, favorito);
        boton.setOnMouseClicked(javafx.event.Event::consume);
        boton.setOnAction(e -> {
            Iconos.setRelleno(icono, boton.isSelected());
            actualizarTextoFavorito(boton, suite, boton.isSelected());
            alAlternar.accept(boton.isSelected());
            e.consume();
        });
        StackPane.setAlignment(boton, Pos.TOP_RIGHT);
        StackPane.setMargin(boton, new Insets(12));
        return boton;
    }

    private static void actualizarTextoFavorito(ToggleButton boton, ApartaSuiteDTO suite, boolean favorito) {
        String texto = favorito ? "Quitar " + suite.nombre() + " de favoritos" : "Guardar " + suite.nombre() + " en favoritos";
        boton.setAccessibleText(texto);
        boton.setTooltip(new javafx.scene.control.Tooltip(texto));
    }

    private static Image cargarImagen(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return null;
        }
        try (InputStream entrada = TarjetaApartaSuite.class.getResourceAsStream(ruta)) {
            return entrada == null ? null : new Image(entrada);
        } catch (Exception e) {
            return null;
        }
    }
}
