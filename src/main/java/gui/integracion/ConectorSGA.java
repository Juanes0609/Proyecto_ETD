package gui.integracion;

/**
 * Único punto de contacto entre la GUI y el núcleo del SGA
 * ({@code SistemaGestionAlojamiento} y el paquete {@code modelo}).
 * <p>
 * Las vistas nunca llaman al núcleo directamente; cuando los integrantes 2, 3 y 4
 * publiquen sus métodos, se declaran aquí y se implementan en
 * {@link ConectorSGAProveedor}. Métodos previstos según el plan del proyecto:
 * <ul>
 *     <li>Indicadores: totales de apartamentos, disponibles, ocupados, en preparación y reservas activas.</li>
 *     <li>Carga del escenario inicial ({@code persistencia.DatosInicialesSGA}).</li>
 *     <li>Apartamentos: {@code registrarApartamento}, listado (recorrido inorden del ABB),
 *         filtro por estado/bloque y {@code cambiarAFueraDeServicio}.</li>
 *     <li>Huéspedes: {@code registrarHuesped}, búsqueda por documento, favoritos e historial.</li>
 *     <li>Disponibilidad: {@code buscarDisponibles(entrada, salida, personas, comparador)} y {@code crearReserva}.</li>
 *     <li>Reservas: consulta, {@code pagarAnticipoPortal}, {@code registrarCheckIn},
 *         {@code registrarCheckOut} y cancelación.</li>
 * </ul>
 * Las excepciones de {@code excepciones/} que lancen esos métodos se dejan
 * propagar: {@code NotificadorUI#ejecutarConManejo} las muestra al usuario.
 */
public interface ConectorSGA {
}
