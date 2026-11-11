package estructuras;
import java.util.Iterator;

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
    @Override
    public Iterator<T> iterator(){
        throw new UnsupportedOperationException("Aun en Construcción");
    }
}
