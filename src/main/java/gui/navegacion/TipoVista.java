package gui.navegacion;

import gui.componentes.Iconos;
import gui.integracion.dto.Rol;

import java.util.Arrays;
import java.util.List;

/**
 * Destinos del sidebar del personal, agrupados por rol. Son solo destinos
 * (lugares a los que ir), nunca acciones: las acciones viven dentro de cada vista.
 * <p>
 * Agregar un destino nuevo es añadir una constante aquí y registrar su clase
 * en {@code AplicacionSGA#registrarVistas}.
 */
public enum TipoVista {

    // ---------- Recepción ----------
    RECEPCION_RESERVAR(Rol.RECEPCIONISTA, "Reservar", Iconos.Icono.CALENDARIO,
            "Registra reservas por teléfono o en persona y atiende el canal externo."),
    RECEPCION_LLEGADAS(Rol.RECEPCIONISTA, "Llegadas y salidas", Iconos.Icono.MALETA,
            "Check-in y check-out de las reservas activas."),
    RECEPCION_FOLIOS(Rol.RECEPCIONISTA, "Folios", Iconos.Icono.RECIBO,
            "Cargos y pagos de cada huésped según los servicios que solicite."),
    RECEPCION_PREPARACION(Rol.RECEPCIONISTA, "Preparación", Iconos.Icono.PORTAPAPELES,
            "Cola de aseo con prioridad para las llegadas de hoy y ruta más corta."),

    // ---------- Administración ----------
    ADMIN_REPORTES(Rol.ADMINISTRADOR, "Reportes", Iconos.Icono.GRAFICA,
            "Ocupación, ingresos, reservas por canal y tiempos de preparación."),
    ADMIN_EDIFICIO(Rol.ADMINISTRADOR, "Edificio", Iconos.Icono.RED,
            "Conectividad del edificio, pasillos cerrados y agrupación por bloque."),
    ADMIN_CATALOGOS(Rol.ADMINISTRADOR, "Catálogos", Iconos.Icono.CARPETA,
            "Aparta-suites, huéspedes y empleados."),
    ADMIN_TEMPORADAS(Rol.ADMINISTRADOR, "Temporadas y tarifas", Iconos.Icono.ETIQUETA,
            "Fechas de cada temporada y tarifas por aparta-suite.");

    private final Rol rol;
    private final String titulo;
    private final Iconos.Icono icono;
    private final String descripcion;

    TipoVista(Rol rol, String titulo, Iconos.Icono icono, String descripcion) {
        this.rol = rol;
        this.titulo = titulo;
        this.icono = icono;
        this.descripcion = descripcion;
    }

    /** Destinos del rol, en el orden en que aparecen en el sidebar. */
    public static List<TipoVista> deRol(Rol rol) {
        return Arrays.stream(values()).filter(t -> t.rol == rol).toList();
    }

    public Rol getRol() {
        return rol;
    }

    public String getTitulo() {
        return titulo;
    }

    public Iconos.Icono getIcono() {
        return icono;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
