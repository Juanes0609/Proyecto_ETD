package Estructuras;

import excepciones.EstructuraVaciaException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Pila<T> implements Iterable<T> {

    private static class Nodo<T>{
        T dato;
        Nodo<T> siguiente;

        Nodo (T dato, Nodo<T> siguiente ){
            this.dato = dato;
            this.siguiente = siguiente;
        }
    }

    private Nodo<T> tope;
    private int tamanio;

    public void push(T dato){
        tope = new Nodo<>(dato, tope);
        tamanio++;
    }

    public T pop() {
        if (esVacia()) {
            throw new EstructuraVaciaException("No se puede desapilar una pila que no tiene nada");
        }
        T dato = tope.dato;
        tope = tope.siguiente;
        tamanio--;
        return dato;
    }

    public T peek () {
        if (esVacia()) {
            throw new EstructuraVaciaException("La pila esta vacia, no hay tope que revisar");
        }
        return tope.dato;
    }

    public boolean esVacia() {
        return tope == null;
    }

    public int tamanio() {
        return tamanio;
    }

    @Override
    public Iterator<T> iterator(){
        return new Iterator<T>() {
            private Nodo<T> actual = tope;

            @Override
            public boolean hasNext (){
                return actual != null;
            }

            @Override
            public T next() {
                if (actual == null){
                    throw new NoSuchElementException();
                }
                T dato = actual.dato;
                actual = actual.siguiente;
                return dato;
            }
        };
    }
}
