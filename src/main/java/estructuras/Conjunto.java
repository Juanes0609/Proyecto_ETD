package estructuras;
import java.util.Iterator;

/** Conjunto genérico (sin repetidos, según equals) apoyado en la lista doblemente enlazada propia. */
public class Conjunto<T> implements Iterable<T> {

    private final ListaDoblementeEnlazada<T> elementos = new ListaDoblementeEnlazada<>();

    /** @return true si se agregó; false si ya estaba. */
    public boolean agregar(T elemento) {
        if (elemento == null || elementos.contiene(elemento)) return false;
        elementos.agregarAlFinal(elemento);
        return true;
    }

    public boolean eliminar(T elemento) { return elementos.eliminar(elemento); }
    public boolean contiene(T elemento) { return elementos.contiene(elemento); }
    public int tamanio() { return elementos.tamanio(); }
    public boolean estaVacio() { return elementos.estaVacia(); }

    public Conjunto<T> union(Conjunto<T> otro) {
        Conjunto<T> r = new Conjunto<>();
        for (T t : this) r.agregar(t);
        for (T t : otro) r.agregar(t);
        return r;
    }

    public Conjunto<T> interseccion(Conjunto<T> otro) {
        Conjunto<T> r = new Conjunto<>();
        for (T t : this) if (otro.contiene(t)) r.agregar(t);
        return r;
    }

    @Override
    public Iterator<T> iterator() { return elementos.iterator(); }

    @Override
    public String toString() { return elementos.toString(); }
}
