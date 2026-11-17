package estructuras;

import excepciones.EstructuraVaciaException;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ListaCircular<T> implements Iterable<T> {
    private NodoCircular<T> inicio;
    private NodoCircular<T> actual;
    private int tam;

    public ListaCircular() {
        inicio = null;
        actual = null;
        tam = 0;
    }

    // Agregar un elemento al final
    public void agregar(T dato) {
        if (dato == null) {
            throw new IllegalArgumentException(
                    "El dato no puede ser null"
            );
        }

        NodoCircular<T> nuevo = new NodoCircular<>(dato);

        if (inicio == null) {
            inicio = nuevo;
            nuevo.setSiguiente(inicio);
            actual = inicio;
        } else {
            NodoCircular<T> ultimo = inicio;

            while (ultimo.getSiguiente() != inicio) {
                ultimo = ultimo.getSiguiente();
            }

            ultimo.setSiguiente(nuevo);
            nuevo.setSiguiente(inicio);
        }

        tam++;
    }

    // Consultar el primer elemento
    public T getInicio() {
        if (estaVacia()) {
            throw new EstructuraVaciaException(
                    "La lista circular está vacía"
            );
        }

        return inicio.getDato();
    }

    // Consultar el elemento en el que está el cursor
    public T getActual() {
        if (estaVacia()) {
            throw new EstructuraVaciaException(
                    "La lista circular está vacía"
            );
        }

        return actual.getDato();
    }

    // Mover el cursor al siguiente elemento (después del último vuelve al primero)
    public void avanzar() {
        if (estaVacia()) {
            throw new EstructuraVaciaException(
                    "La lista circular está vacía"
            );
        }

        actual = actual.getSiguiente();
    }

    // Volver el cursor al primer elemento
    public void reiniciar() {
        if (estaVacia()) {
            throw new EstructuraVaciaException(
                    "La lista circular está vacía"
            );
        }

        actual = inicio;
    }

    public boolean estaVacia() {
        return tam == 0;
    }

    public int getTam() {
        return tam;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private NodoCircular<T> nodo = inicio;
            private int recorridos = 0;

            @Override
            public boolean hasNext() {
                return recorridos < tam;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                T dato = nodo.getDato();
                nodo = nodo.getSiguiente();
                recorridos++;

                return dato;
            }
        };
    }
}