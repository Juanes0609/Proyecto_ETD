package modelo;

import java.util.List;

public class Vertice<T> {
    private T dato;
    private String codigo;
    private double distancia;
    private Vertice<T> predecesor;
    private boolean visitado;
    private List<Arista<T>> listAristas;

   public Vertice(T dato, String codigo) {
       this.dato = dato;
        this.codigo = codigo;
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

    public List<Arista<T>> getListAristas() {
        return listAristas;
    }

    public void setListAristas(List<Arista<T>> listAristas) {
        this.listAristas = listAristas;
    }
}
