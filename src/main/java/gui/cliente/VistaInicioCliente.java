package gui.cliente;

import gui.componentes.FabricaComponentesUI;
import gui.componentes.FiltroApartaSuites;
import gui.componentes.Formato;
import gui.componentes.Iconos;
import gui.componentes.TarjetaApartaSuite;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.navegacion.VistaBase;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Vista principal del huésped: búsqueda de disponibilidad. Los aparta-suites se
 * muestran como cards, filtrados por fechas, personas y precio, en el orden que
 * elija (precio, capacidad o dormitorios).
 */
public class VistaInicioCliente extends VistaBase {

    private final VentanaCliente ventana;
    private final FlowPane grilla = new FlowPane(20, 20);
    private final VBox contenido = new VBox(18);
    private FiltroApartaSuites filtro;
    private List<ApartaSuiteDTO> suites = List.of();
    private Set<String> favoritos = new HashSet<>();
    private String temporada = "BAJA";

    public VistaInicioCliente(VentanaCliente ventana) {
        super(ventana);
        this.ventana = ventana;
    }

    @Override
    protected Node construir() {
        contenido.setPadding(new javafx.geometry.Insets(8, 0, 24, 0));
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        return scroll;
    }

    @Override
    public void alMostrar() {
        boolean ok = notificador().ejecutarConManejo(() -> {
            suites = conector().listarApartaSuites().stream().filter(ApartaSuiteDTO::reservable).toList();
            temporada = conector().temporadaEn(LocalDate.now());
            favoritos = new HashSet<>(conector().favoritos(usuario().documento()).stream()
                    .map(ApartaSuiteDTO::codigo).toList());
        }, null);
        if (!ok) {
            return;
        }
        if (filtro == null) {
            int capacidad = suites.stream().mapToInt(ApartaSuiteDTO::capacidad).max().orElse(1);
            double minimo = suites.stream().mapToDouble(s -> s.tarifa(temporada)).min().orElse(0);
            double maximo = suites.stream().mapToDouble(s -> s.tarifa(temporada)).max().orElse(0);
            filtro = new FiltroApartaSuites(capacidad, minimo, maximo, this::pintar);
            contenido.getChildren().setAll(
                    FabricaComponentesUI.crearTituloPagina("Aparta-suites",
                            "Precios por persona y por noche · temporada " + Formato.estado(temporada).toLowerCase()
                                    + ". Toca un aparta-suite para ver todos sus detalles."),
                    filtro, grilla);
        }
        pintar();
    }

    private void pintar() {
        List<ApartaSuiteDTO> encontrados = new java.util.ArrayList<>();
        notificador().ejecutarConManejo(() -> {
            // Con fechas elegidas, el precio es el de la temporada en que cae la entrada.
            temporada = conector().temporadaEn(filtro.entrada() != null ? filtro.entrada() : LocalDate.now());
            encontrados.addAll(conector().buscarDisponibles(filtro.entrada(), filtro.salida(),
                    filtro.personas(), filtro.orden()));
        }, null);
        List<ApartaSuiteDTO> visibles = encontrados.stream()
                .filter(s -> filtro.coincidePrecio(s.tarifa(temporada))).toList();
        filtro.setResultado(visibles.size());
        grilla.getChildren().clear();
        grilla.setAlignment(visibles.isEmpty() ? javafx.geometry.Pos.TOP_CENTER : javafx.geometry.Pos.TOP_LEFT);
        if (suites.isEmpty()) {
            grilla.getChildren().add(FabricaComponentesUI.crearEstadoVacio(Iconos.Icono.CAMA,
                    "Aún no hay aparta-suites disponibles",
                    "Vuelve pronto: el catálogo del complejo se está actualizando.", null));
            return;
        }
        if (visibles.isEmpty()) {
            grilla.getChildren().add(FabricaComponentesUI.crearEstadoVacio(Iconos.Icono.FILTRO,
                    "Ningún aparta-suite coincide",
                    "Prueba con otras fechas, menos personas o un precio máximo mayor.",
                    FabricaComponentesUI.crearBotonSecundario("Limpiar filtros", filtro::limpiar)));
            return;
        }
        for (ApartaSuiteDTO suite : visibles) {
            grilla.getChildren().add(new TarjetaApartaSuite(suite, suite.tarifa(temporada), temporada, "Reservar",
                    () -> ventana.abrirDetalle(suite), favoritos.contains(suite.codigo()),
                    marcado -> alternarFavorito(suite, marcado)));
        }
    }

    private void alternarFavorito(ApartaSuiteDTO suite, boolean marcado) {
        notificador().ejecutarConManejo(() -> {
            conector().alternarFavorito(usuario().documento(), suite.codigo());
            if (marcado) {
                favoritos.add(suite.codigo());
            } else {
                favoritos.remove(suite.codigo());
            }
        }, marcado ? suite.nombre() + " se guardó en tus favoritos." : suite.nombre() + " salió de tus favoritos.");
    }
}
