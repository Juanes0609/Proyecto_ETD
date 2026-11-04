package gui.cliente;

import gui.componentes.FabricaComponentesUI;
import gui.componentes.Iconos;
import gui.componentes.TarjetaApartaSuite;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.navegacion.VistaBase;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/** Aparta-suites guardados por el huésped (conjunto sin repetidos). */
public class VistaFavoritosCliente extends VistaBase {

    private final VentanaCliente ventana;
    private final FlowPane grilla = new FlowPane(20, 20);

    public VistaFavoritosCliente(VentanaCliente ventana) {
        super(ventana);
        this.ventana = ventana;
    }

    @Override
    protected Node construir() {
        VBox contenido = new VBox(18, FabricaComponentesUI.crearTituloPagina("Favoritos",
                "Los aparta-suites que guardaste para volver a verlos."), grilla);
        contenido.setPadding(new Insets(8, 0, 24, 0));
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        return scroll;
    }

    @Override
    public void alMostrar() {
        notificador().ejecutarConManejo(this::pintar, null);
    }

    private void pintar() {
        List<ApartaSuiteDTO> favoritos = conector().favoritos(usuario().documento());
        String temporada = conector().temporadaEn(LocalDate.now());
        grilla.getChildren().clear();
        if (favoritos.isEmpty()) {
            grilla.getChildren().add(FabricaComponentesUI.crearEstadoVacio(Iconos.Icono.CORAZON,
                    "Aún no tienes favoritos",
                    "Toca el corazón de un aparta-suite para guardarlo aquí.",
                    FabricaComponentesUI.crearBotonSecundario("Explorar aparta-suites",
                            () -> ventana.irA(VentanaCliente.Pestana.INICIO))));
            return;
        }
        for (ApartaSuiteDTO suite : favoritos) {
            grilla.getChildren().add(new TarjetaApartaSuite(suite, suite.tarifa(temporada), temporada, "Reservar",
                    () -> ventana.abrirDetalle(suite), true, marcado -> quitar(suite)));
        }
    }

    private void quitar(ApartaSuiteDTO suite) {
        notificador().ejecutarConManejo(() -> {
            conector().alternarFavorito(usuario().documento(), suite.codigo());
            pintar();
        }, suite.nombre() + " salió de tus favoritos.");
    }
}
