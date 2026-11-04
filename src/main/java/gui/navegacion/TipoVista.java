package gui.navegacion;

/**
 * Secciones navegables de la aplicación. El menú lateral se genera a partir de
 * {@link #values()}, así que agregar una vista nueva consiste en:
 * <ol>
 *     <li>añadir una constante aquí, y</li>
 *     <li>registrar su clase en {@code AplicacionSGA#registrarVistas}.</li>
 * </ol>
 */
public enum TipoVista {

    MENU_PRINCIPAL("Inicio", "Resumen operativo",
            "Indicadores del día y accesos directos a las operaciones principales."),
    TABLERO_APARTAMENTOS("Tablero", "Tablero de apartamentos",
            "Estado operativo de cada apartamento, filtrable por estado y bloque."),
    REGISTRO_APARTAMENTOS("Apartamentos", "Registro de apartamentos",
            "Catálogo de apartamentos y formulario de registro."),
    REGISTRO_HUESPEDES("Huéspedes", "Registro de huéspedes",
            "Catálogo de huéspedes, favoritos e historial de estancias."),
    BUSQUEDA_DISPONIBILIDAD("Disponibilidad", "Búsqueda de disponibilidad",
            "Encuentra apartamentos libres por fechas y número de personas, y reserva."),
    CONSULTA_RESERVAS("Reservas", "Consulta de reservas",
            "Busca reservas y gestiona anticipo, check-in, check-out y cancelación.");

    private final String etiquetaMenu;
    private final String titulo;
    private final String descripcion;

    TipoVista(String etiquetaMenu, String titulo, String descripcion) {
        this.etiquetaMenu = etiquetaMenu;
        this.titulo = titulo;
        this.descripcion = descripcion;
    }

    public String getEtiquetaMenu() {
        return etiquetaMenu;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
