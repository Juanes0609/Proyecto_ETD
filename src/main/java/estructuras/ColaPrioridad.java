package estructuras;

import excepciones.EstructuraVaciaException;

import java.util.Comparator;
import java.util.Iterator;

/**
 * Cola de prioridad genérica: el orden lo decide el {@link Comparator} recibido.
 * Los elementos se guardan ordenados en una {@link ListaDoblementeEnlazada}, así
 * que el de mayor prioridad siempre está al inicio (consultar y desencolar son
 * O(1); encolar es O(n)). Con prioridades iguales se respeta el orden de llegada.
 * <p>
 * Usos: cola de preparación (entrada hoy primero, luego hora de llegada) y
 * reservas pendientes por hora de vencimiento.
 */
public class ColaPrioridad<T> implements Iterable<T> {

    private final ListaDoblementeEnlazada<T> elementos;
    private final Comparator<T> comparador;

    public ColaPrioridad(Comparator<T> comparador) {
        if (comparador == null) {
            throw new IllegalArgumentException("El comparador es obligatorio");
        }
        this.elementos = new ListaDoblementeEnlazada<>();
        this.comparador = comparador;
    }

    /** Agrega un elemento en la posición que le corresponde según su prioridad. */
    public void encolar(T dato) {
        if (dato == null) {
            throw new IllegalArgumentException("El elemento no puede ser null");
        }
        elementos.insertarOrdenado(dato, comparador);
    }

    /** Elemento de mayor prioridad, sin retirarlo. */
    public T consultarPrioridad() {
        if (estaVacia()) {
            throw new EstructuraVaciaException("La cola de prioridad está vacía");
        }
        return elementos.primero();
    }

    /** Retira y devuelve el elemento de mayor prioridad. */
    public T desencolar() {
        T dato = consultarPrioridad();
        elementos.eliminarInicio();
        return dato;
    }

    /** Quita un elemento cualquiera (por ejemplo, una reserva pendiente que ya se pagó). */
    public boolean eliminar(T dato) {
        return elementos.eliminar(dato);
    }

    public boolean estaVacia() {
        return elementos.estaVacio();
    }

    public int getTam() {
        return elementos.getTam();
    }

    /** Recorre los elementos en orden de prioridad sin modificarlos. */
    @Override
    public Iterator<T> iterator() {
        return elementos.iterator();
    }
}
