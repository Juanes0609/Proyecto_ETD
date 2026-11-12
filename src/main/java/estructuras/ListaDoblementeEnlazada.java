package estructuras;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ListaDoblementeEnlazada<T> implements Iterable<T> {
    private NodoDoble<T> inicio;
    private NodoDoble<T> fin;

    private int tam;

    public ListaDoblementeEnlazada(){
        inicio = null;
        fin = null;

        tam = 0;
    }

    //Agregar un elemento al inicio
    public void agregarInicio(T dato){
        NodoDoble<T> nuevo = new NodoDoble<>(dato);

        if(inicio == null && fin == null && tam == 0){
            inicio = nuevo;
            fin = nuevo;
        }else{
            nuevo.setSiguiente(inicio);
            inicio.setAnterior(nuevo);

            inicio = nuevo;
        }
        tam++;
    }

    //Agregar un elemento al final
    public void agregarFinal(T dato){
        NodoDoble<T> nuevo = new NodoDoble<>(dato);

        if(inicio == null && fin == null && tam == 0){
            inicio = fin = nuevo;
        }else{
            fin.setSiguiente(nuevo);
            nuevo.setAnterior(fin);

            fin = nuevo;
        }
        tam++;
    }

    public void eliminarInicio(){
        if(inicio == null && fin == null && tam == 0)
            return;

        if(tam == 1){
            inicio = fin = null;
        }else{
            inicio = inicio.getSiguiente();
            inicio.setAnterior(null);
        }
        tam--;
    }

    public void eliminarFinal(){
        if(inicio == null && fin == null && tam == 0)
            return;

        if(tam == 1){
            inicio = fin = null;
        }else{
            fin = fin.getAnterior();
            fin.setSiguiente(null);
        }
        tam--;
    }

    public boolean eliminar(T dato) {
        NodoDoble<T> actual = inicio;

        while (actual != null) {

            if (actual.getDato() == dato) {

                // Caso 1: el nodo es el primero
                if (actual == inicio) {
                    eliminarInicio();
                }
                // Caso 2: el nodo es el último
                else if (actual == fin) {
                    eliminarFinal();
                }
                // Caso 3: el nodo está en medio
                else {
                    actual.getAnterior().setSiguiente(
                            actual.getSiguiente()
                    );

                    actual.getSiguiente().setAnterior(
                            actual.getAnterior()
                    );

                    tam--;
                }

                return true;
            }

            actual = actual.getSiguiente();
        }

        return false;
    }

    public int size() { return tam; }

    public boolean estaVacio() { return tam == 0; }

    public boolean isEmpty() { return estaVacio(); }

    public boolean contiene(T dato) {
        for (T actual : this) {
            if (actual == null ? dato == null : actual.equals(dato)) return true;
        }
        return false;
    }

    public T obtener(int indice) {
        if (indice < 0 || indice >= tam) throw new IndexOutOfBoundsException("Índice: " + indice);
        NodoDoble<T> actual;
        if (indice < tam / 2) {
            actual = inicio;
            for (int i = 0; i < indice; i++) actual = actual.getSiguiente();
        } else {
            actual = fin;
            for (int i = tam - 1; i > indice; i--) actual = actual.getAnterior();
        }
        return actual.getDato();
    }

    public NodoDoble<T> getInicio() {
        return inicio;
    }

    public void setInicio(NodoDoble<T> inicio) {
        this.inicio = inicio;
    }

    public NodoDoble<T> getFin() {
        return fin;
    }

    public void setFin(NodoDoble<T> fin) {
        this.fin = fin;
    }

    public int getTam() {
        return tam;
    }

    public void setTam(int tam) {
        this.tam = tam;
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
}
