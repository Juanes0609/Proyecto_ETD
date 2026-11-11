package modelo;

import estructuras.Grafo;

public class GestorMapaEdificio {
    private Grafo<NodoEdificio> grafo;

    public GestorMapaEdificio() {
        this.grafo = new Grafo<>();
    }

    public void agregarUbicacion(NodoEdificio nodo) {
        if (nodo == null) {
            throw new IllegalArgumentException(
                    "La ubicación no puede ser null"
            );
        }

        Vertice<NodoEdificio> vertice =
                new Vertice<>(nodo, nodo.getCodigo());

        grafo.agregarVertice(vertice);
    }

    public void conectarUbicaciones(
            String codigoOrigen,
            String codigoDestino,
            double distancia) {

        Vertice<NodoEdificio> origen =
                grafo.buscarVertice(codigoOrigen);

        Vertice<NodoEdificio> destino =
                grafo.buscarVertice(codigoDestino);

        if (origen == null || destino == null) {
            throw new IllegalArgumentException(
                    "Las dos ubicaciones deben existir en el mapa"
            );
        }

        if (distancia < 0 ||
                !Double.isFinite(distancia)) {
            throw new IllegalArgumentException(
                    "La distancia debe ser finita y no negativa"
            );
        }

        Arista<NodoEdificio> arista =
                new Arista<>(origen, destino, distancia, true);

        grafo.agregarArista(arista);
    }

    public Grafo<NodoEdificio> getGrafo() {
        return grafo;
    }
}
