package estructuras;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import sga.excepciones.EstructuraVaciaException;

/** Lista doblemente enlazada genérica, independiente del dominio. */
public class ListaDoble<T> implements Iterable<T> {

    private static class Nodo<T> {
        T dato;
        Nodo<T> anterior, siguiente;
        Nodo(T dato) { this.dato = dato; }
    }

    private Nodo<T> cabeza, cola;
    private int tamanio;

    public int tamanio() { return tamanio; }
    public boolean estaVacia() { return tamanio == 0; }

    public void agregarInicio(T dato) {
        Nodo<T> n = new Nodo<>(dato);
        if (cabeza == null) { cabeza = cola = n; }
        else { n.siguiente = cabeza; cabeza.anterior = n; cabeza = n; }
        tamanio++;
    }

    public void agregarFinal(T dato) {
        Nodo<T> n = new Nodo<>(dato);
        if (cola == null) { cabeza = cola = n; }
        else { n.anterior = cola; cola.siguiente = n; cola = n; }
        tamanio++;
    }

    public T obtener(int indice) {
        return nodoEn(indice).dato;
    }

    private Nodo<T> nodoEn(int indice) {
        if (indice < 0 || indice >= tamanio)
            throw new IndexOutOfBoundsException("Índice " + indice + ", tamaño " + tamanio);
        Nodo<T> actual;
        if (indice < tamanio / 2) {                 // recorre desde el extremo más cercano
            actual = cabeza;
            for (int i = 0; i < indice; i++) actual = actual.siguiente;
        } else {
            actual = cola;
            for (int i = tamanio - 1; i > indice; i--) actual = actual.anterior;
        }
        return actual;
    }

    public T primero() {
        if (cabeza == null) throw new EstructuraVaciaException("La lista está vacía");
        return cabeza.dato;
    }

    public T ultimo() {
        if (cola == null) throw new EstructuraVaciaException("La lista está vacía");
        return cola.dato;
    }

    public T eliminarPrimero() {
        if (cabeza == null) throw new EstructuraVaciaException("La lista está vacía");
        T dato = cabeza.dato;
        desenlazar(cabeza);
        return dato;
    }

    public T eliminarUltimo() {
        if (cola == null) throw new EstructuraVaciaException("La lista está vacía");
        T dato = cola.dato;
        desenlazar(cola);
        return dato;
    }

    /** Elimina la primera ocurrencia (usa equals). */
    public boolean eliminar(T dato) {
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (n.dato == null ? dato == null : n.dato.equals(dato)) {
                desenlazar(n);
                return true;
            }
        }
        return false;
    }

    private void desenlazar(Nodo<T> n) {
        if (n.anterior != null) n.anterior.siguiente = n.siguiente; else cabeza = n.siguiente;
        if (n.siguiente != null) n.siguiente.anterior = n.anterior; else cola = n.anterior;
        tamanio--;
    }

    public boolean contiene(T dato) {
        for (T t : this) if (t == null ? dato == null : t.equals(dato)) return true;
        return false;
    }

    /** Ordenamiento por inserción (estable) usando un Comparator. */
    public void ordenar(Comparator<? super T> comparador) {
        if (tamanio < 2) return;
        for (Nodo<T> n = cabeza.siguiente; n != null; n = n.siguiente) {
            T clave = n.dato;
            Nodo<T> j = n.anterior;
            while (j != null && comparador.compare(j.dato, clave) > 0) {
                j.siguiente.dato = j.dato;
                j = j.anterior;
            }
            (j == null ? cabeza : j.siguiente).dato = clave;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Nodo<T> actual = cabeza;
            @Override public boolean hasNext() { return actual != null; }
            @Override public T next() {
                if (actual == null) throw new NoSuchElementException();
                T dato = actual.dato;
                actual = actual.siguiente;
                return dato;
            }
        };
    }

    /** Iterador de la cola hacia la cabeza (útil para el historial: lo más reciente primero). */
    public Iterator<T> iteradorInverso() {
        return new Iterator<T>() {
            private Nodo<T> actual = cola;
            @Override public boolean hasNext() { return actual != null; }
            @Override public T next() {
                if (actual == null) throw new NoSuchElementException();
                T dato = actual.dato;
                actual = actual.anterior;
                return dato;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            sb.append(n.dato);
            if (n.siguiente != null) sb.append(", ");
        }
        return sb.append("]").toString();
    }
}

