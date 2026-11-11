package estructuras;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ColaPrioridad<T> implements Iterable<T> {

    private ListaDoblementeEnlazada<T> elementos;
    private Comparator<T> comparador;

    public ColaPrioridad(Comparator<T> comparador) {
        this.elementos = new ListaDoblementeEnlazada<>();
        this.comparador = comparador;
    }

    // Agregar un elemento respetando el orden de prioridad
    public void encolar(T dato) {
        if (dato == null) {
            throw new IllegalArgumentException(
                    "El elemento no puede ser null"
            );
        }

        if (elementos.getTam() == 0) {
            elementos.agregarInicio(dato);
            return;
        }

        var actual = elementos.getInicio();

        while (actual != null) {

            // Si el nuevo elemento tiene mayor prioridad,
            // se inserta antes del elemento actual.
            if (comparador.compare(dato, actual.getDato()) < 0) {

                if (actual == elementos.getInicio()) {
                    elementos.agregarInicio(dato);
                } else {
                    // Insertar antes de un nodo intermedio
                    var anterior = actual.getAnterior();
                    var nuevo = new NodoDoble<>(dato);

                    nuevo.setAnterior(anterior);
                    nuevo.setSiguiente(actual);

                    anterior.setSiguiente(nuevo);
                    actual.setAnterior(nuevo);

                    elementos.setTam(elementos.getTam() + 1);
                }

                return;
            }

            actual = actual.getSiguiente();
        }

        // Si no encontramos uno con menor prioridad,
        // el nuevo elemento queda al final.
        elementos.agregarFinal(dato);
    }

    // Consultar el elemento de mayor prioridad sin retirarlo
    public T consultarPrioridad() {
        if (estaVacia()) {
            throw new NoSuchElementException(
                    "La cola de prioridad está vacía"
            );
        }

        return elementos.getInicio().getDato();
    }

    // Retirar y devolver el elemento de mayor prioridad
    public T desencolar() {
        T dato = consultarPrioridad();
        elementos.eliminarInicio();
        return dato;
    }

    public boolean estaVacia() {
        return elementos.getTam() == 0;
    }

    public int getTam() {
        return elementos.getTam();
    }

    @Override
    public Iterator<T> iterator() {
        return elementos.iterator();
    }
}
