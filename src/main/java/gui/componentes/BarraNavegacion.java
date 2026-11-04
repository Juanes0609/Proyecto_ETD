package gui.componentes;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Navegación por destinos con dos disposiciones:
 * <ul>
 *     <li>{@link Disposicion#PESTANAS_INFERIORES}: tab bar del huésped (icono arriba, label abajo).</li>
 *     <li>{@link Disposicion#LATERAL}: sidebar de recepción y administración (icono + label en fila).</li>
 * </ul>
 * El destino activo nunca depende solo del color: el icono pasa a relleno,
 * el label sube de peso y aparece un indicador (pastilla bajo el icono o barra lateral).
 * Solo contiene destinos; las acciones viven dentro de cada vista.
 */
public class BarraNavegacion<T> {

    public enum Disposicion { PESTANAS_INFERIORES, LATERAL }

    private final Disposicion disposicion;
    private final Pane contenedor;
    private final ToggleGroup grupo = new ToggleGroup();
    private final Map<T, ToggleButton> botones = new LinkedHashMap<>();
    private final Consumer<T> alSeleccionar;

    public BarraNavegacion(Disposicion disposicion, Consumer<T> alSeleccionar) {
        this.disposicion = disposicion;
        this.alSeleccionar = alSeleccionar;
        if (disposicion == Disposicion.PESTANAS_INFERIORES) {
            HBox fila = new HBox(4);
            fila.getStyleClass().add("barra-pestanas");
            fila.setAlignment(Pos.CENTER);
            fila.setMaxWidth(Region.USE_PREF_SIZE);
            contenedor = fila;
        } else {
            VBox columna = new VBox(4);
            columna.getStyleClass().add("lista-destinos");
            contenedor = columna;
        }
    }

    public BarraNavegacion<T> agregar(T destino, Iconos.Icono icono, String etiqueta) {
        StackPane imagen = Iconos.crear(icono, disposicion == Disposicion.LATERAL ? 20 : 24);
        Label texto = new Label(etiqueta);
        texto.getStyleClass().add("destino-label");
        Region indicador = new Region();
        indicador.getStyleClass().add("destino-indicador");

        ToggleButton boton = new ToggleButton();
        boton.setToggleGroup(grupo);
        boton.setAccessibleText(etiqueta);
        boton.setFocusTraversable(true);
        if (disposicion == Disposicion.PESTANAS_INFERIORES) {
            VBox contenido = new VBox(3, imagen, texto, indicador);
            contenido.setAlignment(Pos.CENTER);
            boton.setGraphic(contenido);
            boton.getStyleClass().add("pestana");
        } else {
            HBox contenido = new HBox(12, indicador, imagen, texto);
            contenido.setAlignment(Pos.CENTER_LEFT);
            boton.setGraphic(contenido);
            boton.getStyleClass().add("destino-lateral");
            boton.setMaxWidth(Double.MAX_VALUE);
        }
        boton.selectedProperty().addListener((obs, antes, ahora) -> Iconos.setRelleno(imagen, ahora));
        boton.setOnAction(e -> {
            boton.setSelected(true); // el destino activo no se puede "deseleccionar"
            alSeleccionar.accept(destino);
        });
        botones.put(destino, boton);
        contenedor.getChildren().add(boton);
        return this;
    }

    /** Marca un destino como activo sin disparar la navegación (la vista ya cambió). */
    public void marcar(T destino) {
        ToggleButton boton = botones.get(destino);
        if (boton != null) {
            boton.setSelected(true);
        }
    }

    public Pane getNodo() {
        return contenedor;
    }
}
