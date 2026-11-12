package modelo;

import estructuras.ColaPrioridad;
import estructuras.ListaDoblementeEnlazada;

import java.time.LocalDateTime;
import java.util.Comparator;

public class GestorPreparacion {
    private final ColaPrioridad<OrdenPreparacion> cola;
    private final SistemaGestionAlojamiento sistema;

    public GestorPreparacion(SistemaGestionAlojamiento sistema) {
        if (sistema == null) {
            throw new IllegalArgumentException(
                    "El sistema de gestión es obligatorio");
        }
        this.sistema = sistema;

        // Comparator obligatorio: thenComparing
        Comparator<OrdenPreparacion> comparador =
                Comparator.comparingInt(OrdenPreparacion::getPrioridad)
                        .thenComparing(
                                OrdenPreparacion::getHoraLlegadaEstimada,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(OrdenPreparacion::getCodigo);

        this.cola = new ColaPrioridad<>(comparador);
    }

    /**
     * Se llama cuando hay check-out.
     * Crea la orden, deja el apartamento Pendiente y lo encola.
     */
    public OrdenPreparacion encolar(Apartamento apartamento,
                                    boolean tieneEntradaHoy,
                                    LocalDateTime horaLlegadaEstimada) {
        if (apartamento == null) {
            throw new IllegalArgumentException(
                    "El apartamento es obligatorio");
        }

        int prioridad = tieneEntradaHoy ? 1 : 2;

        OrdenPreparacion orden = new OrdenPreparacion(
                apartamento.getCodigo(), // codigo = codigo del apartamento
                LocalDateTime.now(),
                horaLlegadaEstimada,
                prioridad
        );

        apartamento.setEstadoOperativo(EstadoOperativo.PENDIENTEPREPARACION);

        // Si la lista de órdenes del apartamento existe, la dejamos registrada
        if (apartamento.getListOrdenesPreparacion() != null) {
            apartamento.getListOrdenesPreparacion().add(orden);
        }

        cola.encolar(orden);
        return orden;
    }

    /** Saca el siguiente y lo pasa a En preparación. */
    public OrdenPreparacion despacharSiguiente() {
        OrdenPreparacion orden = cola.desencolar();

        Apartamento apartamento =
                sistema.buscarApartamento(orden.getCodigo());

        if (apartamento != null) {
            apartamento.setEstadoOperativo(EstadoOperativo.ENPREPARACION);
        }

        return orden;
    }

    /** Cuando el personal termina, el apartamento queda Preparado. */
    public void marcarPreparado(OrdenPreparacion orden) {
        if (orden == null) {
            throw new IllegalArgumentException("La orden es obligatoria");
        }

        Apartamento apartamento =
                sistema.buscarApartamento(orden.getCodigo());

        if (apartamento != null) {
            apartamento.setEstadoOperativo(EstadoOperativo.PREPARADO);
        }
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

    /** Para mostrar en el panel, sin destruir la cola. */
    public ListaDoblementeEnlazada<OrdenPreparacion> listarPendientes() {
        ListaDoblementeEnlazada<OrdenPreparacion> lista =
                new ListaDoblementeEnlazada<>();
        for (OrdenPreparacion orden : cola) {
            lista.agregarFinal(orden);
        }
        return lista;
    }
}
