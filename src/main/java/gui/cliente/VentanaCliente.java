package gui.cliente;

import estructuras.Pila;
import gui.ContenedorVistas;
import gui.ContextoApp;
import gui.MarcaApp;
import gui.componentes.BarraNavegacion;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.HojaLateral;
import gui.componentes.Iconos;
import gui.componentes.NotificadorUI;
import gui.integracion.ConectorSGA;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.UsuarioDTO;
import gui.navegacion.VistaBase;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Experiencia del huésped: una vista principal (Inicio, con las cards) y el
 * resto de secciones desde la barra inferior de pestañas, al estilo de una app.
 * <pre>
 * ┌──────────────────────────────────────────────────────┐
 * │ Marca                               Hola, Ana · Salir │
 * │ [Notificación inline]                                 │
 * │ Contenido de la pestaña / detalle         ┌────────┐ │
 * │                                           │  Hoja  │ │
 * │        ( Inicio  Favoritos  Reservas  Perfil )       │
 * └──────────────────────────────────────────────────────┘
 * </pre>
 * El detalle de un aparta-suite se apila sobre la pestaña actual (Pila propia)
 * y "Volver" regresa a donde estaba el huésped.
 */
public class VentanaCliente extends BorderPane implements ContenedorVistas {

    /** Pestañas de la barra inferior (3–5, según las guías de Apple; Perfil al final). */
    public enum Pestana { INICIO, FAVORITOS, RESERVAS, PERFIL }

    private final ContextoApp contexto;
    private final NotificadorUI notificador = new NotificadorUI();
    private final HojaLateral hoja = new HojaLateral();
    private final StackPane contenedorVista = new StackPane();
    private final Map<Pestana, VistaBase> vistas = new EnumMap<>(Pestana.class);
    private final Pila<VistaBase> apiladas = new Pila<>();
    private final BarraNavegacion<Pestana> pestanas;
    private final Label saludo = new Label();
    private VistaBase actual;

    public VentanaCliente(ContextoApp contexto) {
        this.contexto = contexto;
        pestanas = new BarraNavegacion<Pestana>(BarraNavegacion.Disposicion.PESTANAS_INFERIORES, this::irA)
                .agregar(Pestana.INICIO, Iconos.Icono.CASA, "Inicio")
                .agregar(Pestana.FAVORITOS, Iconos.Icono.CORAZON, "Favoritos")
                .agregar(Pestana.RESERVAS, Iconos.Icono.BOLSA, "Reservas")
                .agregar(Pestana.PERFIL, Iconos.Icono.PERSONA, "Perfil");

        setTop(crearBarraSuperior());
        setCenter(crearCentro());
        HBox inferior = new HBox(pestanas.getNodo());
        inferior.setAlignment(Pos.CENTER);
        inferior.setPadding(new Insets(6, 0, 18, 0));
        setBottom(inferior);
        irA(Pestana.INICIO);
    }

    private HBox crearBarraSuperior() {
        StackPane avatar = new StackPane(new Label(getUsuario().iniciales()));
        avatar.getStyleClass().add("avatar");
        saludo.getStyleClass().add("encabezado-vista");
        actualizarSaludo();
        HBox barra = new HBox(12, MarcaApp.crear(), FabricaComponentesUI.crearEspaciador(), avatar, saludo,
                FabricaComponentesUI.conIcono(FabricaComponentesUI.crearEnlace("Cerrar sesión", contexto::cerrarSesion),
                        Iconos.Icono.SALIR));
        barra.getStyleClass().add("barra-cliente");
        return barra;
    }

    private StackPane crearCentro() {
        VBox.setVgrow(contenedorVista, Priority.ALWAYS);
        VBox area = new VBox(contenedorVista);
        area.setPadding(new Insets(4, 32, 0, 32));
        StackPane central = new StackPane(area, hoja);
        notificador.colocarEn(central); // flota justo encima de la barra de pestañas
        return central;
    }

    // ---------- Navegación ----------

    /** Cambia de pestaña. Vacía los detalles apilados. */
    public void irA(Pestana pestana) {
        while (!apiladas.esVacia()) {
            apiladas.pop();
        }
        pestanas.marcar(pestana);
        mostrar(vistas.computeIfAbsent(pestana, this::crearVista));
    }

    /** Abre el detalle de un aparta-suite encima de la pestaña actual. */
    public void abrirDetalle(ApartaSuiteDTO suite) {
        if (actual != null) {
            apiladas.push(actual);
        }
        mostrar(new VistaDetalleApartaSuite(this, suite));
    }

    /** Regresa a la vista desde la que se abrió el detalle. */
    public void volver() {
        if (!apiladas.esVacia()) {
            mostrar(apiladas.pop());
        }
    }

    private VistaBase crearVista(Pestana pestana) {
        return switch (pestana) {
            case INICIO -> new VistaInicioCliente(this);
            case FAVORITOS -> new VistaFavoritosCliente(this);
            case RESERVAS -> new VistaReservasCliente(this);
            case PERFIL -> new VistaPerfilCliente(this);
        };
    }

    private void mostrar(VistaBase vista) {
        ocultarHoja();
        notificador.ocultar();
        if (actual != null) {
            actual.alOcultar();
        }
        actual = vista;
        Node contenido = vista.getContenido();
        contenedorVista.getChildren().setAll(contenido);
        vista.alMostrar();
        contenido.setOpacity(0);
        FadeTransition aparecer = new FadeTransition(Duration.millis(160), contenido);
        aparecer.setToValue(1);
        aparecer.play();
    }

    // ---------- Sesión ----------

    /** Actualiza el usuario en sesión tras editar el perfil. */
    public void actualizarUsuario(UsuarioDTO usuario) {
        contexto.setUsuario(usuario);
        actualizarSaludo();
    }

    private void actualizarSaludo() {
        saludo.setText("Hola, " + getUsuario().nombre().trim().split("\\s+")[0]);
    }

    // ---------- ContenedorVistas ----------

    @Override
    public NotificadorUI getNotificador() {
        return notificador;
    }

    @Override
    public ConectorSGA getConector() {
        return contexto.getConector();
    }

    @Override
    public UsuarioDTO getUsuario() {
        return contexto.getUsuario();
    }

    @Override
    public void mostrarHoja(String titulo, Node contenido) {
        hoja.mostrar(titulo, contenido);
    }

    @Override
    public void ocultarHoja() {
        hoja.ocultar();
    }
}
