package modelo;

public class Arista<T> {
    private Vertice<T> origen;
    private Vertice<T> destino;
    private double peso;
    private boolean habilitada;

    public Arista(Vertice<T> origen, Vertice<T> destino, double peso, boolean habilitada) {
        this.origen = origen;
        this.destino = destino;
        this.peso = peso;
        this.habilitada = false;
    }

    public Vertice<T> getOrigen() {
        return origen;
    }

    public void setOrigen(Vertice<T> origen) {
        this.origen = origen;
    }

    public Vertice<T> getDestino() {
        return destino;
    }

    public void setDestino(Vertice<T> destino) {
        this.destino = destino;
    }

    public double getPeso() {
        return peso;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }

    public boolean isHabilitada() {
        return habilitada;
    }

    public void setHabilitada(boolean habilitada) {
        this.habilitada = habilitada;
    }
}
