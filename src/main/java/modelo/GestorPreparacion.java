package modelo;

import estructuras.ColaPrioridad;
import estructuras.ListaDoblementeEnlazada;
import excepciones.TransicionEstadoInvalidaException;

import java.time.LocalDateTime;
import java.util.Comparator;

public class GestorPreparacion {
    private final ColaPrioridad<OrdenPreparacion> cola;
    private final ListaDoblementeEnlazada<OrdenPreparacion> enPreparacion;
    private final ListaDoblementeEnlazada<OrdenPreparacion> terminadas;
    private final SistemaGestionAlojamiento sistema;

    public GestorPreparacion(SistemaGestionAlojamiento sistema) {
        if (sistema == null) {
            throw new IllegalArgumentException(
                    "El sistema de gestión es obligatorio");
        }
        this.sistema = sistema;
        this.enPreparacion = new ListaDoblementeEnlazada<>();
        this.terminadas = new ListaDoblementeEnlazada<>();

        // Comparator obligatorio: entrada hoy primero, luego hora de llegada
        // (thenComparing), y por último el código para desempatar.
        Comparator<OrdenPreparacion> comparador =
                Comparator.comparingInt(OrdenPreparacion::getPrioridad)
                        .thenComparing(
                                OrdenPreparacion::getHoraLlegadaEstimada,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(OrdenPreparacion::getFechaCreacion,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(OrdenPreparacion::getCodigo);

        this.cola = new ColaPrioridad<>(comparador);
    }

    /**
     * Se llama cuando hay check-out. Crea la orden, deja el apartamento
     * Pendiente de preparación y lo encola.
     *
     * @throws TransicionEstadoInvalidaException si el apartamento está fuera
     *         de servicio o ya está pendiente / en preparación.
     */
    public OrdenPreparacion encolar(Apartamento apartamento,
                                    boolean tieneEntradaHoy,
                                    LocalDateTime horaLlegadaEstimada) {
        if (apartamento == null) {
            throw new IllegalArgumentException(
                    "El apartamento es obligatorio");
        }

        EstadoOperativo estado = apartamento.getEstadoOperativo();
        if (estado == EstadoOperativo.FUERASERVICIO) {
            throw new TransicionEstadoInvalidaException(
                    "El apartamento " + apartamento.getCodigo()
                            + " está fuera de servicio y no se puede preparar");
        }
        if (estado == EstadoOperativo.PENDIENTEPREPARACION
                || estado == EstadoOperativo.ENPREPARACION) {
            throw new TransicionEstadoInvalidaException(
                    "El apartamento " + apartamento.getCodigo()
                            + " ya está en el proceso de preparación");
        }

        int prioridad = tieneEntradaHoy ? 1 : 2;

        OrdenPreparacion orden = new OrdenPreparacion(
                apartamento.getCodigo(), // codigo = codigo del apartamento
                LocalDateTime.now(),
                horaLlegadaEstimada,
                prioridad
        );

        apartamento.setEstadoOperativo(EstadoOperativo.PENDIENTEPREPARACION);

        if (apartamento.getListOrdenesPreparacion() != null) {
            apartamento.getListOrdenesPreparacion().agregarFinal(orden);
        }

        cola.encolar(orden);
        return orden;
    }

    /**
     * Saca la orden más prioritaria y pasa el apartamento a En preparación.
     *
     * @throws excepciones.EstructuraVaciaException si no hay órdenes en cola.
     */
    public OrdenPreparacion despacharSiguiente() {
        OrdenPreparacion orden = cola.desencolar();

        Apartamento apartamento = sistema.buscarApartamento(orden.getCodigo());
        if (apartamento != null) {
            apartamento.setEstadoOperativo(EstadoOperativo.ENPREPARACION);
        }

        orden.setHoraInicioPreparacion(LocalDateTime.now());
        enPreparacion.agregarFinal(orden);
        return orden;
    }

    /**
     * Cuando el personal termina, el apartamento queda Preparado.
     *
     * @throws TransicionEstadoInvalidaException si la orden no está En preparación.
     */
    public void marcarPreparado(OrdenPreparacion orden) {
        if (orden == null) {
            throw new IllegalArgumentException("La orden es obligatoria");
        }
        if (!enPreparacion.contiene(orden)) {
            throw new TransicionEstadoInvalidaException(
                    "La orden de " + orden.getCodigo()
                            + " no está En preparación; solo se puede terminar "
                            + "un apartamento que ya fue despachado");
        }

        Apartamento apartamento = sistema.buscarApartamento(orden.getCodigo());
        if (apartamento != null) {
            apartamento.setEstadoOperativo(EstadoOperativo.PREPARADO);
        }

        orden.setHoraFinPreparacion(LocalDateTime.now());
        enPreparacion.eliminar(orden);
        terminadas.agregarFinal(orden);
    }

    /** Marca como preparado el apartamento con ese código (lo usa la GUI). */
    public void marcarPreparado(String codigoApartamento) {
        for (OrdenPreparacion orden : enPreparacion) {
            if (orden.getCodigo().equals(codigoApartamento)) {
                marcarPreparado(orden);
                return;
            }
        }
        throw new TransicionEstadoInvalidaException(
                "El apartamento " + codigoApartamento + " no está En preparación");
    }

    public OrdenPreparacion verSiguiente() {
        return cola.consultarPrioridad();
    }

    public boolean estaVacia() {
        return cola.estaVacia();
    }

    public int cantidadPendientes() {
        return cola.getTam();
    }

    /** Para mostrar en el panel, sin destruir la cola (ya en orden de prioridad). */
    public ListaDoblementeEnlazada<OrdenPreparacion> listarPendientes() {
        ListaDoblementeEnlazada<OrdenPreparacion> lista =
                new ListaDoblementeEnlazada<>();
        for (OrdenPreparacion orden : cola) {
            lista.agregarFinal(orden);
        }
        return lista;
    }

    public ListaDoblementeEnlazada<OrdenPreparacion> listarEnPreparacion() {
        ListaDoblementeEnlazada<OrdenPreparacion> lista =
                new ListaDoblementeEnlazada<>();
        for (OrdenPreparacion orden : enPreparacion) {
            lista.agregarFinal(orden);
        }
        return lista;
    }

    /** Historial de órdenes ya terminadas; sirve para las estadísticas. */
    public ListaDoblementeEnlazada<OrdenPreparacion> listarTerminadas() {
        ListaDoblementeEnlazada<OrdenPreparacion> lista =
                new ListaDoblementeEnlazada<>();
        for (OrdenPreparacion orden : terminadas) {
            lista.agregarFinal(orden);
        }
        return lista;
    }

    /** Tiempo promedio de preparación en minutos (0 si no hay terminadas). */
    public double minutosPromedioPreparacion() {
        long suma = 0;
        int cantidad = 0;
        for (OrdenPreparacion orden : terminadas) {
            long minutos = orden.minutosDePreparacion();
            if (minutos >= 0) {
                suma += minutos;
                cantidad++;
            }
        }
        return cantidad == 0 ? 0 : (double) suma / cantidad;
    }
}
