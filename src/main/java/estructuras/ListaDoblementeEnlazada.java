package estructuras;

import excepciones.EstructuraVaciaException;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lista doblemente enlazada genérica: cada nodo conoce al anterior y al siguiente,
 * así que se agrega y elimina en los dos extremos en tiempo constante.
 * Se usa para el historial de estancias, los ocupantes de una reserva, el desglose
 * de noches y como base de la cola de prioridad y del conjunto.
 */
public class ListaDoblementeEnlazada<T> implements Iterable<T> {
    private NodoDoble<T> inicio;
    private NodoDoble<T> fin;
    private int tam;

    public ListaDoblementeEnlazada() {
        inicio = null;
        fin = null;
        tam = 0;
    }

    /** Agrega un elemento al inicio. */
    public void agregarInicio(T dato) {
        NodoDoble<T> nuevo = new NodoDoble<>(dato);
        if (estaVacio()) {
            inicio = fin = nuevo;
        } else {
            nuevo.setSiguiente(inicio);
            inicio.setAnterior(nuevo);
            inicio = nuevo;
        }
        tam++;
    }

    /** Agrega un elemento al final. */
    public void agregarFinal(T dato) {
        NodoDoble<T> nuevo = new NodoDoble<>(dato);
        if (estaVacio()) {
            inicio = fin = nuevo;
        } else {
            fin.setSiguiente(nuevo);
            nuevo.setAnterior(fin);
            fin = nuevo;
        }
        tam++;
    }

    /**
     * Inserta el dato antes del primer elemento que sea mayor según el comparador,
     * así la lista queda siempre ordenada (los empates conservan el orden de llegada).
     * Es la base de {@link ColaPrioridad}.
     */
    public void insertarOrdenado(T dato, Comparator<T> comparador) {
        NodoDoble<T> actual = inicio;
        while (actual != null && comparador.compare(dato, actual.getDato()) >= 0) {
            actual = actual.getSiguiente();
        }
        if (actual == null) {
            agregarFinal(dato);
        } else if (actual == inicio) {
            agregarInicio(dato);
        } else {
            NodoDoble<T> nuevo = new NodoDoble<>(dato);
            NodoDoble<T> anterior = actual.getAnterior();
            nuevo.setAnterior(anterior);
            nuevo.setSiguiente(actual);
            anterior.setSiguiente(nuevo);
            actual.setAnterior(nuevo);
            tam++;
        }
    }

    public void eliminarInicio() {
        if (estaVacio()) {
            return;
        }
        if (tam == 1) {
            inicio = fin = null;
        } else {
            inicio = inicio.getSiguiente();
            inicio.setAnterior(null);
        }
        tam--;
    }

    public void eliminarFinal() {
        if (estaVacio()) {
            return;
        }
        if (tam == 1) {
            inicio = fin = null;
        } else {
            fin = fin.getAnterior();
            fin.setSiguiente(null);
        }
        tam--;
    }

    /** Elimina la primera aparición del dato (comparando con {@code equals}). */
    public boolean eliminar(T dato) {
        NodoDoble<T> actual = inicio;
        while (actual != null) {
            if (iguales(actual.getDato(), dato)) {
                if (actual == inicio) {
                    eliminarInicio();
                } else if (actual == fin) {
                    eliminarFinal();
                } else {
                    actual.getAnterior().setSiguiente(actual.getSiguiente());
                    actual.getSiguiente().setAnterior(actual.getAnterior());
                    tam--;
                }
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }

    public boolean contiene(T dato) {
        for (T actual : this) {
            if (iguales(actual, dato)) {
                return true;
            }
        }
        return false;
    }

    /** @throws EstructuraVaciaException si la lista está vacía */
    public T primero() {
        if (estaVacio()) {
            throw new EstructuraVaciaException("La lista está vacía");
        }
        return inicio.getDato();
    }

    /** @throws EstructuraVaciaException si la lista está vacía */
    public T ultimo() {
        if (estaVacio()) {
            throw new EstructuraVaciaException("La lista está vacía");
        }
        return fin.getDato();
    }

    /** Recorre desde el extremo más cercano al índice. */
    public T obtener(int indice) {
        if (indice < 0 || indice >= tam) {
            throw new IndexOutOfBoundsException("Índice: " + indice);
        }
        NodoDoble<T> actual;
        if (indice < tam / 2) {
            actual = inicio;
            for (int i = 0; i < indice; i++) {
                actual = actual.getSiguiente();
            }
        } else {
            actual = fin;
            for (int i = tam - 1; i > indice; i--) {
                actual = actual.getAnterior();
            }
        }
        return actual.getDato();
    }

    public void limpiar() {
        inicio = fin = null;
        tam = 0;
    }

    public int getTam() {
        return tam;
    }

    public boolean estaVacio() {
        return tam == 0;
    }

    private static boolean iguales(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private NodoDoble<T> actual = inicio;

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T dato = actual.getDato();
                actual = actual.getSiguiente();
                return dato;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (NodoDoble<T> n = inicio; n != null; n = n.getSiguiente()) {
            sb.append(n.getDato());
            if (n.getSiguiente() != null) {
                sb.append(", ");
            }
        }
        return sb.append("]").toString();
    }
}
