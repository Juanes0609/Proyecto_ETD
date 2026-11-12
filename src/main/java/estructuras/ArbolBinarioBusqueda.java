package estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ArbolBinarioBusqueda<T extends Comparable<? super T>>
        implements Iterable<T> {

    private NodoArbol<T> raiz;
    private int tamanio;

    public ArbolBinarioBusqueda() {
        raiz = null;
        tamanio = 0;
    }

    public NodoArbol<T> getRaiz() {
        return raiz;
    }

    public int size() {
        return tamanio;
    }

    public boolean estaVacio() {
        return raiz == null;
    }

    // INSERTAR
    public boolean insertar(T dato) {
        if (dato == null) {
            throw new IllegalArgumentException(
                    "No se puede insertar un dato nulo."
            );
        }

        if (buscar(dato) != null) {
            return false;
        }

        raiz = insertarRecursivo(raiz, dato);
        tamanio++;
        return true;
    }

    private NodoArbol<T> insertarRecursivo(
            NodoArbol<T> actual, T dato) {

        if (actual == null) {
            return new NodoArbol<>(dato);
        }

        int comparacion = dato.compareTo(actual.getDato());

        if (comparacion < 0) {
            actual.setIzquierdo(
                    insertarRecursivo(actual.getIzquierdo(), dato)
            );
        } else if (comparacion > 0) {
            actual.setDerecho(
                    insertarRecursivo(actual.getDerecho(), dato)
            );
        }

        return actual;
    }

    // BUSCAR
    public T buscar(T dato) {
        if (dato == null) {
            return null;
        }

        NodoArbol<T> encontrado = buscarRecursivo(raiz, dato);

        return encontrado == null ? null : encontrado.getDato();
    }

    private NodoArbol<T> buscarRecursivo(
            NodoArbol<T> actual, T dato) {

        if (actual == null) {
            return null;
        }

        int comparacion = dato.compareTo(actual.getDato());

        if (comparacion == 0) {
            return actual;
        }

        if (comparacion < 0) {
            return buscarRecursivo(actual.getIzquierdo(), dato);
        }

        return buscarRecursivo(actual.getDerecho(), dato);
    }

    public boolean contiene(T dato) {
        return buscar(dato) != null;
    }

    // ELIMINAR
    public boolean eliminar(T dato) {
        if (dato == null || !contiene(dato)) {
            return false;
        }

        raiz = eliminarRecursivo(raiz, dato);
        tamanio--;
        return true;
    }

    private NodoArbol<T> eliminarRecursivo(
            NodoArbol<T> actual, T dato) {

        if (actual == null) {
            return null;
        }

        int comparacion = dato.compareTo(actual.getDato());

        if (comparacion < 0) {
            actual.setIzquierdo(
                    eliminarRecursivo(actual.getIzquierdo(), dato)
            );
        } else if (comparacion > 0) {
            actual.setDerecho(
                    eliminarRecursivo(actual.getDerecho(), dato)
            );
        } else {
            // Caso 1: no tiene hijos.
            if (actual.getIzquierdo() == null
                    && actual.getDerecho() == null) {
                return null;
            }

            // Caso 2: solo tiene hijo derecho.
            if (actual.getIzquierdo() == null) {
                return actual.getDerecho();
            }

            // Caso 2: solo tiene hijo izquierdo.
            if (actual.getDerecho() == null) {
                return actual.getIzquierdo();
            }

            // Caso 3: tiene dos hijos.
            NodoArbol<T> sucesor =
                    encontrarMinimo(actual.getDerecho());

            actual.setDato(sucesor.getDato());

            actual.setDerecho(
                    eliminarRecursivo(
                            actual.getDerecho(),
                            sucesor.getDato()
                    )
            );
        }

        return actual;
    }

    private NodoArbol<T> encontrarMinimo(NodoArbol<T> actual) {
        if (actual.getIzquierdo() == null) {
            return actual;
        }

        return encontrarMinimo(actual.getIzquierdo());
    }

    // ALTURA: árbol vacío = -1; hoja = 0.
    public int altura() {
        return alturaRecursiva(raiz);
    }

    private int alturaRecursiva(NodoArbol<T> actual) {
        if (actual == null) {
            return -1;
        }

        int alturaIzquierda =
                alturaRecursiva(actual.getIzquierdo());
        int alturaDerecha =
                alturaRecursiva(actual.getDerecho());

        return 1 + Math.max(alturaIzquierda, alturaDerecha);
    }

    // RECORRIDOS RECURSIVOS
    public void recorridoInOrden(Consumer<T> accion) {
        recorridoInOrdenRecursivo(raiz, accion);
    }

    private void recorridoInOrdenRecursivo(
            NodoArbol<T> actual, Consumer<T> accion) {

        if (actual == null) {
            return;
        }

        recorridoInOrdenRecursivo(actual.getIzquierdo(), accion);
        accion.accept(actual.getDato());
        recorridoInOrdenRecursivo(actual.getDerecho(), accion);
    }

    public void recorridoPreOrden(Consumer<T> accion) {
        recorridoPreOrdenRecursivo(raiz, accion);
    }

    private void recorridoPreOrdenRecursivo(
            NodoArbol<T> actual, Consumer<T> accion) {

        if (actual == null) {
            return;
        }

        accion.accept(actual.getDato());
        recorridoPreOrdenRecursivo(actual.getIzquierdo(), accion);
        recorridoPreOrdenRecursivo(actual.getDerecho(), accion);
    }

    public void recorridoPostOrden(Consumer<T> accion) {
        recorridoPostOrdenRecursivo(raiz, accion);
    }

    private void recorridoPostOrdenRecursivo(
            NodoArbol<T> actual, Consumer<T> accion) {

        if (actual == null) {
            return;
        }

        recorridoPostOrdenRecursivo(actual.getIzquierdo(), accion);
        recorridoPostOrdenRecursivo(actual.getDerecho(), accion);
        accion.accept(actual.getDato());
    }

    // BUSCAR UN ELEMENTO QUE CUMPLA UNA CONDICIÓN.
    public T existeSi(Predicate<T> condicion) {
        return buscarCondicionRecursiva(raiz, condicion);
    }

    private T buscarCondicionRecursiva(
            NodoArbol<T> actual, Predicate<T> condicion) {

        if (actual == null) {
            return null;
        }

        if (condicion.test(actual.getDato())) {
            return actual.getDato();
        }

        T resultado = buscarCondicionRecursiva(
                actual.getIzquierdo(), condicion
        );

        if (resultado != null) {
            return resultado;
        }

        return buscarCondicionRecursiva(
                actual.getDerecho(), condicion
        );
    }

    // ITERADOR EN ORDEN ASCENDENTE.
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private T siguiente = obtenerMinimo(raiz);

            @Override
            public boolean hasNext() {
                return siguiente != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(
                            "No hay más elementos en el árbol."
                    );
                }

                T actual = siguiente;
                siguiente = obtenerSucesor(actual);
                return actual;
            }
        };
    }

    private T obtenerMinimo(NodoArbol<T> actual) {
        if (actual == null) {
            return null;
        }

        while (actual.getIzquierdo() != null) {
            actual = actual.getIzquierdo();
        }

        return actual.getDato();
    }

    private T obtenerSucesor(T dato) {
        NodoArbol<T> actual = raiz;
        NodoArbol<T> sucesor = null;

        while (actual != null) {
            int comparacion = dato.compareTo(actual.getDato());

            if (comparacion < 0) {
                sucesor = actual;
                actual = actual.getIzquierdo();
            } else {
                actual = actual.getDerecho();
            }
        }

        return sucesor == null ? null : sucesor.getDato();
    }

    public void setRaiz(NodoArbol<T> raiz) {
        this.raiz = raiz;
    }

    public int getTamanio() {
        return tamanio;
    }

    public void setTamanio(int tamanio) {
        this.tamanio = tamanio;
    }
}
