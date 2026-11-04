package gui.componentes;

import javafx.css.PseudoClass;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/**
 * Iconos vectoriales del SGA dibujados sobre una grilla de 24×24.
 * <p>
 * Cada icono tiene una <em>forma</em> (silueta cerrada) y unos <em>detalles</em>
 * (trazos abiertos). En estado normal todo se dibuja como contorno; con
 * {@link #setRelleno(Node, boolean)} la forma se rellena. Así el elemento
 * activo de una barra de navegación no depende solo del color.
 * El color se hereda del token CSS {@code -icono-color}.
 */
public final class Iconos {

    private static final PseudoClass RELLENO = PseudoClass.getPseudoClass("relleno");

    /** Catálogo de iconos: forma cerrada + detalles de trazo. */
    public enum Icono {
        CASA("M3 10.6 12 3.2l9 7.4V20a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1z", "M9.5 21v-5.5h5V21"),
        CORAZON("M12 20.3s-7.6-4.6-9.3-9.2C1.5 7.8 3.6 4.4 7.2 4.4c2 0 3.5 1.1 4.8 2.9 1.3-1.8 2.8-2.9 4.8-2.9 "
                + "3.6 0 5.7 3.4 4.5 6.7-1.7 4.6-9.3 9.2-9.3 9.2z", ""),
        BOLSA("M5 8.2h14l-1.1 12a1 1 0 0 1-1 .9H7.1a1 1 0 0 1-1-.9z", "M9 8.2V6.6a3 3 0 0 1 6 0v1.6"),
        PERSONA("M4.5 20.5c.6-3.8 3.8-6.3 7.5-6.3s6.9 2.5 7.5 6.3zM12 11.6a3.8 3.8 0 1 0 0-7.6 3.8 3.8 0 0 0 0 7.6z", ""),
        GRUPO("M2.5 19.5c.5-3 2.8-5 5.5-5s5 2 5.5 5zM8 11.5a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM14.5 14.8"
                + "c.6-.2 1.3-.3 2-.3 2.7 0 5 2 5.5 5h-6.3zM16.5 11.5a3 3 0 1 0 0-6 3 3 0 0 0 0 6z", ""),
        FILTRO("", "M4 7h16M7 12h10M10 17h4"),
        CALENDARIO("M4 6.5A1.5 1.5 0 0 1 5.5 5h13A1.5 1.5 0 0 1 20 6.5v12a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 18.5z",
                "M4 10h16M8 3v4M16 3v4"),
        MALETA("M3.5 8.5A1.5 1.5 0 0 1 5 7h14a1.5 1.5 0 0 1 1.5 1.5v10A1.5 1.5 0 0 1 19 20H5a1.5 1.5 0 0 1-1.5-1.5z",
                "M9 7V5.5A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5V7"),
        RECIBO("M6 3h12v18l-2-1.5-2 1.5-2-1.5-2 1.5-2-1.5L6 21z", "M9 8h6M9 12h6M9 16h3"),
        PORTAPAPELES("M6 5h12a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z",
                "M9 3.5h6M8.5 12.5l2.2 2.2 4.3-4.6"),
        GRAFICA("M4 20v-9h4v9zM10 20V4h4v16zM16 20v-6h4v6z", ""),
        RED("M12 7.6a2.6 2.6 0 1 0 0-5.2 2.6 2.6 0 0 0 0 5.2zM5 21.6a2.6 2.6 0 1 0 0-5.2 2.6 2.6 0 0 0 0 5.2z"
                + "M19 21.6a2.6 2.6 0 1 0 0-5.2 2.6 2.6 0 0 0 0 5.2z", "M10.8 7.3 6.2 16.7M13.2 7.3l4.6 9.4M7.6 19h8.8"),
        CARPETA("M3 6.5A1.5 1.5 0 0 1 4.5 5H9l2 2h8.5A1.5 1.5 0 0 1 21 8.5v10a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18.5z",
                ""),
        ETIQUETA("M3 12.2V4.5A1.5 1.5 0 0 1 4.5 3h7.7l8.8 8.8a1.5 1.5 0 0 1 0 2.1l-7.1 7.1a1.5 1.5 0 0 1-2.1 0z",
                "M8 8h.01"),
        OJO("M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z", "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"),
        OJO_TACHADO("M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z", "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM4 4l16 16"),
        ATRAS("", "M15 5l-7 7 7 7"),
        CERRAR("", "M6 6l12 12M18 6 6 18"),
        INFO("M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z", "M12 11v5M12 8h.01"),
        ALERTA("M12 3.5 21.5 20h-19z", "M12 10v4M12 17h.01"),
        CHECK("", "M5 12.5l4.5 4.5L19 7.5"),
        MAS("", "M12 5v14M5 12h14"),
        SALIR("", "M15 4h3.5A1.5 1.5 0 0 1 20 5.5v13a1.5 1.5 0 0 1-1.5 1.5H15M10 8l-4 4 4 4M6 12h10"),
        LLAVE("M8 15.5a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9z", "M12.5 11H21M18 11v3M15.5 11v2"),
        DESHACER("", "M9 14 4 9l5-5M4 9h10.5a5.5 5.5 0 0 1 0 11H11"),
        RELOJ("M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z", "M12 7v5l3 2"),
        CAMA("", "M3 18V7M3 13h18v5M21 13v-2a3 3 0 0 0-3-3h-7v5M6.5 11a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z");

        private final String forma;
        private final String detalles;

        Icono(String forma, String detalles) {
            this.forma = forma;
            this.detalles = detalles;
        }
    }

    private Iconos() {
    }

    /** Icono de {@code tamano} píxeles, en contorno. */
    public static StackPane crear(Icono icono, double tamano) {
        Group dibujo = new Group();
        if (!icono.forma.isEmpty()) {
            dibujo.getChildren().add(trazo(icono.forma, "icono-forma"));
        }
        if (!icono.detalles.isEmpty()) {
            dibujo.getChildren().add(trazo(icono.detalles, "icono-detalle"));
        }
        double escala = tamano / 24.0;
        dibujo.setScaleX(escala);
        dibujo.setScaleY(escala);

        StackPane caja = new StackPane(new Group(dibujo));
        caja.getStyleClass().add("icono");
        caja.setMinSize(tamano, tamano);
        caja.setPrefSize(tamano, tamano);
        caja.setMaxSize(tamano, tamano);
        caja.setMouseTransparent(true);
        return caja;
    }

    /** Cambia entre contorno y relleno (estado activo). */
    public static void setRelleno(Node icono, boolean relleno) {
        icono.pseudoClassStateChanged(RELLENO, relleno);
    }

    private static SVGPath trazo(String contenido, String clase) {
        SVGPath path = new SVGPath();
        path.setContent(contenido);
        path.getStyleClass().add(clase);
        return path;
    }
}
