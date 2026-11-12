package estructuras;
import java.util.Iterator;

public class Conjunto<T> implements Iterable<T> {

    private final ListaDoblementeEnlazada<T> elementos;

    public Conjunto() {
        elementos = new ListaDoblementeEnlazada<>();
    }

    /** Agrega el elemento si todavía no existe. @return true si se agregó. */
    public boolean agregar(T elemento) {
        if (contiene(elemento)) return false;
        elementos.agregarFinal(elemento);
        return true;
    }

    public boolean add(T elemento) { return agregar(elemento); }

    public boolean contiene(T elemento) {
        for (T actual : elementos) {
            if (actual == null ? elemento == null : actual.equals(elemento)) return true;
        }
        return false;
    }

    public boolean contains(T elemento) { return contiene(elemento); }

    public boolean eliminar(T elemento) {
        for (T actual : elementos) {
            if (actual == null ? elemento == null : actual.equals(elemento)) {
                return elementos.eliminar(actual);
            }
        }
        return false;
    }

    public boolean remove(T elemento) { return eliminar(elemento); }
    public int size() { return elementos.getTam(); }
    public int getTam() { return size(); }
    public boolean estaVacio() { return size() == 0; }
    public void limpiar() {
        while (!elementos.estaVacio()) elementos.eliminarInicio();
    }

    @Override
    public Iterator<T> iterator(){
        throw new UnsupportedOperationException("Aun en Construcción");
    }
}
