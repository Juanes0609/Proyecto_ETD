package estructuras;
import excepciones.EstructuraVaciaException;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Cola<T> implements Iterable<T> {
    private NodoCola<T> inicio;
    private NodoCola<T> fin;
    private int tam;

    public Cola() {
        inicio = null;
        fin = null;
        tam = 0;
    }

    // Agregar un elemento al final de la cola
    public void encolar(T dato) {

        NodoCola<T> nuevo = new NodoCola<>(dato);

        if (estaVacia()) {
            inicio = nuevo;
            fin = nuevo;
        } else {
            fin.setSiguiente(nuevo);
            fin = nuevo;
        }

        tam++;
    }

    // Retirar y devolver el primer elemento
    public T desencolar() {

        if (estaVacia()) {
            throw new EstructuraVaciaException(
                    "No se puede desencolar una cola vacia"
            );
        }

        T dato = inicio.getDato();

        inicio = inicio.getSiguiente();
        tam--;

        if (inicio == null) {
            fin = null;
        }

        return dato;
    }

    // Consultar el primer elemento sin retirarlo
    public T consultarFrente() {

        if (estaVacia()) {
            throw new EstructuraVaciaException(
                    "La cola esta vacia"
            );
        }

        return inicio.getDato();
    }

    public boolean estaVacia() {
        return inicio == null;
    }

    public int getTam() {
        return tam;
    }

    // Recorrer la cola sin modificarla
    @Override
    public Iterator<T> iterator() {

        return new Iterator<T>() {

            private NodoCola<T> actual = inicio;

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
}
