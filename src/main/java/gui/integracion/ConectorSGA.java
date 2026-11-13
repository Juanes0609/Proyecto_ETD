package gui.integracion;

import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.ConexionDTO;
import gui.integracion.dto.CotizacionDTO;
import gui.integracion.dto.EmpleadoDTO;
import gui.integracion.dto.HuespedDTO;
import gui.integracion.dto.MovimientoFolioDTO;
import gui.integracion.dto.NodoEdificioDTO;
import gui.integracion.dto.OcupanteDTO;
import gui.integracion.dto.OrdenPreparacionDTO;
import gui.integracion.dto.ReporteDTO;
import gui.integracion.dto.ReservaDTO;
import gui.integracion.dto.Rol;
import gui.integracion.dto.RutaDTO;
import gui.integracion.dto.SolicitudExternaDTO;
import gui.integracion.dto.TemporadaDTO;
import gui.integracion.dto.UsuarioDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Único punto de contacto entre la GUI y el núcleo del SGA.
 * <p>
 * La GUI trabaja con DTOs propios ({@code gui.integracion.dto}) para no depender
 * de las clases internas de {@code modelo}. Hay dos implementaciones:
 * <ul>
 *     <li>{@link ConectorSGADemo}: datos de ejemplo en memoria, para diseñar y probar la interfaz.</li>
 *     <li>{@link ConectorSGAProveedor}: delega en {@code SistemaGestionAlojamiento} a medida que
 *         el equipo lo implemente.</li>
 * </ul>
 * Cada método tiene una implementación por defecto que lanza
 * {@link UnsupportedOperationException}; {@code NotificadorUI} la muestra como
 * "funcionalidad en construcción". Las excepciones de {@code excepciones/}
 * se dejan propagar para que la GUI las presente.
 */
public interface ConectorSGA {

    /** {@code true} solo en la implementación con datos de ejemplo. */
    default boolean esDemo() {
        return false;
    }

    /** Recarga el escenario inicial (persistencia/DatosInicialesSGA). */
    default void cargarEscenarioInicial() {
        throw pendiente("cargarEscenarioInicial");
    }

    // ---------- Sesión ----------

    /** Rol asociado a un correo registrado, para anticiparlo en el login. */
    default Optional<Rol> detectarRol(String correo) {
        return Optional.empty();
    }

    /** @throws IllegalArgumentException si las credenciales no son válidas */
    default UsuarioDTO iniciarSesion(String correo, String clave) {
        throw pendiente("iniciarSesion");
    }

    /** Registra un huésped nuevo con acceso al portal y devuelve su usuario. */
    default UsuarioDTO registrarCuentaHuesped(HuespedDTO datos, String clave) {
        throw pendiente("registrarCuentaHuesped");
    }

    // ---------- Catálogo y cotización ----------

    default List<ApartaSuiteDTO> listarApartaSuites() {
        throw pendiente("listarApartaSuites");
    }

    /** Tipo de temporada (BAJA, MEDIA, ALTA) que aplica en una fecha. */
    default String temporadaEn(LocalDate fecha) {
        throw pendiente("temporadaEn");
    }

    /** {@code true} si no hay reservas activas que se solapen con el rango. */
    default boolean estaDisponible(String codigoApartaSuite, LocalDate entrada, LocalDate salida) {
        throw pendiente("estaDisponible");
    }

    /** Liquidación noche por noche (recorrido de la estancia con Iterator). */
    default CotizacionDTO cotizar(String codigoApartaSuite, LocalDate entrada, LocalDate salida, int personasQuePagan) {
        throw pendiente("cotizar");
    }

    // ---------- Huéspedes ----------

    default HuespedDTO obtenerHuesped(String documento) {
        throw pendiente("obtenerHuesped");
    }

    default void actualizarHuesped(HuespedDTO huesped) {
        throw pendiente("actualizarHuesped");
    }

    default List<HuespedDTO> listarHuespedes() {
        throw pendiente("listarHuespedes");
    }

    default void registrarHuesped(HuespedDTO huesped) {
        throw pendiente("registrarHuesped");
    }

    /** Agrega o quita un favorito (Conjunto sin repetidos). @return {@code true} si quedó como favorito */
    default boolean alternarFavorito(String documentoHuesped, String codigoApartaSuite) {
        throw pendiente("alternarFavorito");
    }

    default List<ApartaSuiteDTO> favoritos(String documentoHuesped) {
        throw pendiente("favoritos");
    }

    default List<ReservaDTO> reservasDeHuesped(String documentoHuesped) {
        throw pendiente("reservasDeHuesped");
    }

    // ---------- Reservas ----------

    /**
     * Crea una reserva. El titular se registra si aún no existe.
     *
     * @param canal PORTAL (huésped) o DIRECTA (recepción)
     */
    default ReservaDTO crearReserva(String codigoApartaSuite, LocalDate entrada, LocalDate salida,
                                    HuespedDTO titular, List<OcupanteDTO> acompanantes,
                                    String canal, LocalTime horaLlegada) {
        throw pendiente("crearReserva");
    }

    default void pagarAnticipo(String codigoReserva) {
        throw pendiente("pagarAnticipo");
    }

    /** Valor que se devolvería si la reserva se cancelara hoy. */
    default double calcularDevolucion(String codigoReserva) {
        throw pendiente("calcularDevolucion");
    }

    default void cancelarReserva(String codigoReserva) {
        throw pendiente("cancelarReserva");
    }

    /** Reservas pendientes, confirmadas y en curso. */
    default List<ReservaDTO> reservasActivas() {
        throw pendiente("reservasActivas");
    }

    default void registrarCheckIn(String codigoReserva) {
        throw pendiente("registrarCheckIn");
    }

    default void registrarCheckOut(String codigoReserva) {
        throw pendiente("registrarCheckOut");
    }

    // ---------- Canal externo ----------

    default List<SolicitudExternaDTO> colaCanalExterno() {
        throw pendiente("colaCanalExterno");
    }

    /** Procesa la solicitud más antigua. @return mensaje con el resultado (reserva o conflicto) */
    default String procesarSiguienteSolicitudExterna() {
        throw pendiente("procesarSiguienteSolicitudExterna");
    }

    // ---------- Folio ----------

    default List<MovimientoFolioDTO> movimientosFolio(String codigoReserva) {
        throw pendiente("movimientosFolio");
    }

    default void registrarCargo(String codigoReserva, String concepto, double monto) {
        throw pendiente("registrarCargo");
    }

    default void registrarPago(String codigoReserva, String concepto, double monto) {
        throw pendiente("registrarPago");
    }

    /** Anula la última operación con un movimiento contrario (Pila de deshacer). */
    default MovimientoFolioDTO deshacerUltimaOperacion(String codigoReserva) {
        throw pendiente("deshacerUltimaOperacion");
    }

    // ---------- Preparación y turnos ----------

    /** Cola de prioridad: primero los aparta-suites con entrada hoy. */
    default List<OrdenPreparacionDTO> colaPreparacion() {
        throw pendiente("colaPreparacion");
    }

    default List<OrdenPreparacionDTO> enPreparacion() {
        throw pendiente("enPreparacion");
    }

    default OrdenPreparacionDTO despacharSiguientePreparacion() {
        throw pendiente("despacharSiguientePreparacion");
    }

    default void terminarPreparacion(String codigoApartaSuite) {
        throw pendiente("terminarPreparacion");
    }

    /** Ruta más corta (Dijkstra) desde el cuarto de aseo hasta el aparta-suite. */
    default RutaDTO rutaPreparacion(String codigoApartaSuite) {
        throw pendiente("rutaPreparacion");
    }

    /**
     * Recorrido completo desde el cuarto de aseo por todos los aparta-suites de la
     * cola de preparación, en orden de prioridad (Dijkstra tramo por tramo).
     */
    default RutaDTO rutaRecorridoPreparacion() {
        throw pendiente("rutaRecorridoPreparacion");
    }

    /** Turno vigente de recepción (ListaCircular de recepcionistas). */
    default EmpleadoDTO turnoActual() {
        throw pendiente("turnoActual");
    }

    // ---------- Administración ----------

    default ReporteDTO generarReporte() {
        throw pendiente("generarReporte");
    }

    default List<NodoEdificioDTO> nodosEdificio() {
        throw pendiente("nodosEdificio");
    }

    default List<ConexionDTO> conexionesEdificio() {
        throw pendiente("conexionesEdificio");
    }

    /** Cierra o reabre un pasillo. */
    default void alternarPasillo(String origen, String destino) {
        throw pendiente("alternarPasillo");
    }

    /** Aparta-suites sin camino desde recepción con los pasillos actuales (BFS). */
    default List<String> apartaSuitesInalcanzables() {
        throw pendiente("apartaSuitesInalcanzables");
    }

    default void guardarApartaSuite(ApartaSuiteDTO apartaSuite) {
        throw pendiente("guardarApartaSuite");
    }

    default List<EmpleadoDTO> listarEmpleados() {
        throw pendiente("listarEmpleados");
    }

    default void registrarEmpleado(EmpleadoDTO empleado) {
        throw pendiente("registrarEmpleado");
    }

    default List<TemporadaDTO> listarTemporadas() {
        throw pendiente("listarTemporadas");
    }

    default void agregarTemporada(TemporadaDTO temporada) {
        throw pendiente("agregarTemporada");
    }

    default void actualizarTarifas(String codigoApartaSuite, double baja, double media, double alta) {
        throw pendiente("actualizarTarifas");
    }

    private static UnsupportedOperationException pendiente(String operacion) {
        return new UnsupportedOperationException(operacion);
    }
}
