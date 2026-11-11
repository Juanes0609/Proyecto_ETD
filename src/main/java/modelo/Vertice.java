package modelo;

import Estructuras.ListaDoblementeEnlazada;

import java.util.List;

public class Vertice<T> {
    private T dato;
    private String codigo;
    private double distancia;
    private Vertice<T> predecesor;
    private boolean visitado;
    private ListaDoblementeEnlazada<Arista<T>> listAristas;

   public Vertice(T dato, String codigo) {
       this.dato = dato;
        this.codigo = codigo;
        this.distancia = Double.POSITIVE_INFINITY;
        this.predecesor = null;
        this.visitado = false;
        this.listAristas = new ListaDoblementeEnlazada<>();
   }

    public T getDato() {
        return dato;
    }

    public void setDato(T dato) {
        this.dato = dato;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public double getDistancia() {
        return distancia;
    }

    public void setDistancia(double distancia) {
        this.distancia = distancia;
    }

    public Vertice<T> getPredecesor() {
        return predecesor;
    }

    public void setPredecesor(Vertice<T> predecesor) {
        this.predecesor = predecesor;
    }

    public boolean isVisitado() {
        return visitado;
    }

    public void setVisitado(boolean visitado) {
        this.visitado = visitado;
    }

    public ListaDoblementeEnlazada<Arista<T>> getListAristas() {
        return listAristas;
    }

    public void setListAristas(ListaDoblementeEnlazada<Arista<T>> listAristas) {
        this.listAristas = listAristas;
    }
    public void agregarArista(Arista<T> arista) {
        if (arista != null) {
            listAristas.agregarFinal(arista);
        }
    }
}
