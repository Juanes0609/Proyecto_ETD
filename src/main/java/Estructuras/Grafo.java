package Estructuras;
import modelo.Arista;
import modelo.Vertice;

import java.util.Iterator;

public class Grafo<T> implements Iterable<T> {
    private ListaDoblementeEnlazada<Vertice<T>> vertices;
    private ListaDoblementeEnlazada<Arista<T>> aristas;

    public Grafo() {
        this.vertices = new ListaDoblementeEnlazada<>();
        this.aristas = new ListaDoblementeEnlazada<>();
    }
    @Override
    public Iterator<T> iterator(){
        throw new UnsupportedOperationException("Aun en Construcción");
    }
}
