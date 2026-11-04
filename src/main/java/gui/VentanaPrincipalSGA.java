package gui;

import gui.componentes.BarraNavegacion;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.HojaLateral;
import gui.componentes.Iconos;
import gui.componentes.NotificadorUI;
import gui.integracion.ConectorSGA;
import gui.integracion.dto.EmpleadoDTO;
import gui.integracion.dto.Rol;
import gui.integracion.dto.UsuarioDTO;
import gui.navegacion.GestorNavegacion;
import gui.navegacion.TipoVista;
import gui.navegacion.VistaBase;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * Ventana del personal (recepción y administración), patrón Single-Window:
 * <pre>
 * ┌────────────┬────────────────────────────────────────────────┐
 * │ Marca      │ ← Atrás  Título · descripción     Usuario  Salir│
 * │ Destinos   ├────────────────────────────────────────────────┤
 * │ (3–4)      │ [Notificación inline]                ┌───────┐ │
 * │            │ Contenido del destino activo         │ Hoja  │ │
 * │ Turno      │                                      └───────┘ │
 * └────────────┴────────────────────────────────────────────────┘
 * </pre>
 * El sidebar solo tiene destinos del rol; el activo se marca con icono relleno,
 * label en negrita y barra indicadora. Nunca se abren otros {@code Stage} ni {@code Alert}.
 */
public class VentanaPrincipalSGA extends BorderPane implements ContenedorVistas {

    private final ContextoApp contexto;
    private final Rol rol;
    private final NotificadorUI notificador = new NotificadorUI();
    private final GestorNavegacion navegacion = new GestorNavegacion(this);
    private final HojaLateral hoja = new HojaLateral();
    private final BarraNavegacion<TipoVista> destinos;

    private final Label lblTitulo = new Label();
    private final Label lblDescripcion = new Label();
    private final StackPane contenedorVista = new StackPane();

    public VentanaPrincipalSGA(ContextoApp contexto, Rol rol) {
        this.contexto = contexto;
        this.rol = rol;
        this.destinos = new BarraNavegacion<>(BarraNavegacion.Disposicion.LATERAL, navegacion::navegarA);
        TipoVista.deRol(rol).forEach(t -> destinos.agregar(t, t.getIcono(), t.getTitulo()));
        setLeft(crearSidebar());
        setTop(crearBarraSuperior());
        setCenter(crearAreaCentral());
    }

    /** Muestra el primer destino del rol. Se llama después de registrar las vistas. */
    public void iniciar() {
        List<TipoVista> propios = TipoVista.deRol(rol);
        if (!propios.isEmpty()) {
            navegacion.navegarA(propios.getFirst());
        }
    }

    // ---------- Construcción ----------

    private VBox crearSidebar() {
        Label seccion = new Label(rol.getEtiqueta().toUpperCase());
        seccion.getStyleClass().add("sidebar-seccion");

        VBox sidebar = new VBox(MarcaApp.crear(), seccion, destinos.getNodo(), FabricaComponentesUI.crearEspaciador());
        sidebar.getStyleClass().add("sidebar");
        if (rol == Rol.RECEPCIONISTA) {
            Node turno = crearTurnoActual();
            if (turno != null) {
                sidebar.getChildren().add(turno);
            }
        }
        return sidebar;
    }

    /** Información del turno de recepción (no es un botón: solo se consulta). */
    private Node crearTurnoActual() {
        try {
            EmpleadoDTO turno = getConector().turnoActual();
            Label titulo = new Label("Turno actual");
            Label nombre = new Label(turno.nombre());
            nombre.getStyleClass().add("sidebar-pie-valor");
            Label horario = new Label(turno.turno());
            HBox fila = new HBox(10, Iconos.crear(Iconos.Icono.RELOJ, 18), new VBox(2, titulo, nombre, horario));
            fila.setAlignment(Pos.CENTER_LEFT);
            VBox pie = new VBox(fila);
            pie.getStyleClass().add("sidebar-pie");
            return pie;
        } catch (RuntimeException e) {
            return null; // el servicio de turnos aún no está disponible
        }
    }

    private HBox crearBarraSuperior() {
        Button btnAtras = FabricaComponentesUI.crearBotonIcono(Iconos.Icono.ATRAS, "Volver al destino anterior",
                navegacion::retroceder);
        btnAtras.disableProperty().bind(navegacion.puedeRetrocederProperty().not());

        lblTitulo.getStyleClass().add("titulo-seccion");
        lblDescripcion.getStyleClass().add("texto-secundario");
        VBox titulos = new VBox(0, lblTitulo, lblDescripcion);

        HBox barra = new HBox(btnAtras, titulos, FabricaComponentesUI.crearEspaciador());
        barra.getStyleClass().add("barra-superior");

        if (rol == Rol.ADMINISTRADOR) {
            barra.getChildren().add(FabricaComponentesUI.crearBotonSecundario("Cargar escenario inicial",
                    this::cargarEscenarioInicial));
        }
        barra.getChildren().addAll(crearUsuario(), FabricaComponentesUI.conIcono(
                FabricaComponentesUI.crearEnlace("Cerrar sesión", contexto::cerrarSesion), Iconos.Icono.SALIR));
        return barra;
    }

    private HBox crearUsuario() {
        UsuarioDTO usuario = getUsuario();
        StackPane avatar = new StackPane(new Label(usuario.iniciales()));
        avatar.getStyleClass().add("avatar");
        Label nombre = new Label(usuario.nombre());
        nombre.getStyleClass().add("encabezado-vista");
        Label detalle = new Label(rol.getEtiqueta());
        detalle.getStyleClass().add("texto-terciario");
        HBox caja = new HBox(10, avatar, new VBox(0, nombre, detalle));
        caja.setAlignment(Pos.CENTER_LEFT);
        return caja;
    }

    private StackPane crearAreaCentral() {
        VBox.setVgrow(contenedorVista, Priority.ALWAYS);
        VBox area = new VBox(12, notificador, contenedorVista);
        area.getStyleClass().add("contenido-central");
        return new StackPane(area, hoja);
    }

    private void cargarEscenarioInicial() {
        if (notificador.ejecutarConManejo(getConector()::cargarEscenarioInicial,
                "Escenario inicial cargado de nuevo.")) {
            navegacion.refrescarActual();
        }
    }

    // ---------- API para la navegación ----------

    /** Coloca la vista en el área central. Lo invoca {@link GestorNavegacion}. */
    public void mostrarContenido(TipoVista tipo, VistaBase vista) {
        ocultarHoja();
        notificador.ocultar();
        lblTitulo.setText(tipo.getTitulo());
        lblDescripcion.setText(tipo.getDescripcion());
        destinos.marcar(tipo);

        Node contenido = vista != null
                ? vista.getContenido()
                : FabricaComponentesUI.crearEstadoVacio(Iconos.Icono.INFO, "Destino en construcción",
                "Esta sección aún no está registrada en AplicacionSGA.", null);
        contenedorVista.getChildren().setAll(contenido);
        contenido.setOpacity(0);
        FadeTransition aparecer = new FadeTransition(Duration.millis(160), contenido);
        aparecer.setToValue(1);
        aparecer.play();
    }

    public GestorNavegacion getNavegacion() {
        return navegacion;
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
