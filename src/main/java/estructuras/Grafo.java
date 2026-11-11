package estructuras;
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
    public void agregarVertice(Vertice<T> vertice) {
        if (vertice == null) {
            throw new IllegalArgumentException(
                    "El vertice no puede ser null"
            );
        }

        if (buscarVertice(vertice.getCodigo()) != null) {
            throw new IllegalArgumentException(
                    "Ya existe un vertice con ese codigo"
            );
        }

        vertices.agregarFinal(vertice);
    }

    public Vertice<T> buscarVertice(String codigo) {
        if (codigo == null) {
            return null;
        }

        for (Vertice<T> vertice : vertices) {
            if (codigo.equals(vertice.getCodigo())) {
                return vertice;
            }
        }

        return null;
    }

    public void agregarArista(Arista<T> arista) {
        if (arista == null) {
            throw new IllegalArgumentException(
                    "La arista no puede ser null"
            );
        }

        Vertice<T> origen = arista.getOrigen();
        Vertice<T> destino = arista.getDestino();

        if (origen == null || destino == null) {
            throw new IllegalArgumentException(
                    "Los vertices de la arista son obligatorios"
            );
        }

        if (buscarVertice(origen.getCodigo()) != origen
                || buscarVertice(destino.getCodigo()) != destino) {
            throw new IllegalArgumentException(
                    "Ambos vertices deben pertenecer al grafo"
            );
        }

        aristas.agregarFinal(arista);
        origen.agregarArista(arista);

        if (origen != destino) {
            destino.agregarArista(arista);
        }
    }
    public boolean eliminarArista(Arista<T> arista) {

        if (arista == null) {
            return false;
        }

        // Verificar que la arista pertenezca al grafo
        boolean existe = false;

        for (Arista<T> actual : aristas) {
            if (actual == arista) {
                existe = true;
                break;
            }
        }

        if (!existe) {
            return false;
        }

        // Quitarla de la lista global
        aristas.eliminar(arista);

        // Quitarla de los dos vertices conectados
        arista.getOrigen().getListAristas().eliminar(arista);

        if (arista.getDestino() != arista.getOrigen()) {
            arista.getDestino().getListAristas().eliminar(arista);
        }

        return true;
    }
    public boolean eliminarVertice(String codigo) {

        Vertice<T> vertice = buscarVertice(codigo);

        if (vertice == null) {
            return false;
        }

        // Buscar y eliminar todas las aristas conectadas
        while (true) {

            Arista<T> encontrada = null;

            for (Arista<T> arista : aristas) {

                if (arista.getOrigen() == vertice
                        || arista.getDestino() == vertice) {

                    encontrada = arista;
                    break;
                }
            }

            if (encontrada == null) {
                break;
            }

            eliminarArista(encontrada);
        }

        // Eliminar el vertice de la lista global
        vertices.eliminar(vertice);

        return true;
    }
    public ListaDoblementeEnlazada<T> recorridoBFS(String codigoInicio) {

        ListaDoblementeEnlazada<T> recorrido =
                new ListaDoblementeEnlazada<>();

        Vertice<T> inicial = buscarVertice(codigoInicio);

        if (inicial == null) {
            return recorrido;
        }

        // Reiniciar los estados de visita
        for (Vertice<T> vertice : vertices) {
            vertice.setVisitado(false);
        }

        Cola<Vertice<T>> cola = new Cola<>();

        inicial.setVisitado(true);
        cola.encolar(inicial);

        while (!cola.estaVacia()) {

            Vertice<T> actual = cola.desencolar();

            recorrido.agregarFinal(actual.getDato());

            for (Arista<T> arista : actual.getListAristas()) {

                if (!arista.isHabilitada()) {
                    continue;
                }

                Vertice<T> vecino;

                if (arista.getOrigen() == actual) {
                    vecino = arista.getDestino();
                } else if (arista.getDestino() == actual) {
                    vecino = arista.getOrigen();
                } else {
                    continue;
                }

                if (!vecino.isVisitado()) {
                    vecino.setVisitado(true);
                    cola.encolar(vecino);
                }
            }
        }

        // Dejar los vertices listos para futuros recorridos
        for (Vertice<T> vertice : vertices) {
            vertice.setVisitado(false);
        }

        return recorrido;
    }

    public ListaDoblementeEnlazada<T> recorridoDFS(String codigoInicio) {

        ListaDoblementeEnlazada<T> recorrido =
                new ListaDoblementeEnlazada<>();

        Vertice<T> inicial = buscarVertice(codigoInicio);

        if (inicial == null) {
            return recorrido;
        }

        // Reiniciar los vertices antes del recorrido
        for (Vertice<T> vertice : vertices) {
            vertice.setVisitado(false);
        }

        // Iniciar el recorrido en profundidad
        recorridoDFSRecursivo(inicial, recorrido);

        // Dejar los vertices listos para otros recorridos
        for (Vertice<T> vertice : vertices) {
            vertice.setVisitado(false);
        }

        return recorrido;
    }

    private void recorridoDFSRecursivo(
            Vertice<T> actual,
            ListaDoblementeEnlazada<T> recorrido) {

        // Marcar y registrar el vertice actual
        actual.setVisitado(true);
        recorrido.agregarFinal(actual.getDato());

        // Explorar las conexiones del vertice actual
        for (Arista<T> arista : actual.getListAristas()) {

            if (!arista.isHabilitada()) {
                continue;
            }

            Vertice<T> vecino;

            if (arista.getOrigen() == actual) {
                vecino = arista.getDestino();
            } else if (arista.getDestino() == actual) {
                vecino = arista.getOrigen();
            } else {
                continue;
            }

            // Continuar solo si el vecino no ha sido visitado
            if (!vecino.isVisitado()) {
                recorridoDFSRecursivo(vecino, recorrido);
            }
        }
    }
    public ListaDoblementeEnlazada<T> caminoMasCorto(
            String codigoInicio, String codigoDestino) {

        ListaDoblementeEnlazada<T> camino =
                new ListaDoblementeEnlazada<>();

        Vertice<T> inicio = buscarVertice(codigoInicio);
        Vertice<T> destino = buscarVertice(codigoDestino);

        if (inicio == null || destino == null) {
            return camino;
        }

        // Reiniciar los datos del algoritmo.
        for (Vertice<T> vertice : vertices) {
            vertice.setDistancia(Double.POSITIVE_INFINITY);
            vertice.setPredecesor(null);
            vertice.setVisitado(false);
        }

        inicio.setDistancia(0);

        while (true) {

            // Buscar el vértice no visitado con menor distancia.
            Vertice<T> actual = null;

            for (Vertice<T> vertice : vertices) {
                if (!vertice.isVisitado()
                        && (actual == null
                        || vertice.getDistancia()
                        < actual.getDistancia())) {
                    actual = vertice;
                }
            }

            // No quedan vértices alcanzables.
            if (actual == null
                    || actual.getDistancia()
                    == Double.POSITIVE_INFINITY) {
                break;
            }

            actual.setVisitado(true);

            // Ya llegamos al destino.
            if (actual == destino) {
                break;
            }

            // Revisar las conexiones del vértice actual.
            for (Arista<T> arista : actual.getListAristas()) {

                if (!arista.isHabilitada()) {
                    continue;
                }

                Vertice<T> vecino;

                if (arista.getOrigen() == actual) {
                    vecino = arista.getDestino();
                } else if (arista.getDestino() == actual) {
                    vecino = arista.getOrigen();
                } else {
                    continue;
                }

                if (vecino.isVisitado()) {
                    continue;
                }

                double nuevaDistancia =
                        actual.getDistancia() + arista.getPeso();

                if (nuevaDistancia < vecino.getDistancia()) {
                    vecino.setDistancia(nuevaDistancia);
                    vecino.setPredecesor(actual);
                }
            }
        }

        // Reconstruir el camino desde el destino hacia el inicio.
        if (destino.getDistancia()
                != Double.POSITIVE_INFINITY) {

            Vertice<T> actual = destino;

            while (actual != null) {
                camino.agregarInicio(actual.getDato());
                actual = actual.getPredecesor();
            }
        }

        // Dejar el grafo listo para otro recorrido.
        for (Vertice<T> vertice : vertices) {
            vertice.setDistancia(Double.POSITIVE_INFINITY);
            vertice.setPredecesor(null);
            vertice.setVisitado(false);
        }

        return camino;
    }

    public ListaDoblementeEnlazada<Arista<T>> getAristas() {
        return aristas;
    }

    public void setAristas(ListaDoblementeEnlazada<Arista<T>> aristas) {
        this.aristas = aristas;
    }

    public ListaDoblementeEnlazada<Vertice<T>> getVertices() {
        return vertices;
    }

    public void setVertices(ListaDoblementeEnlazada<Vertice<T>> vertices) {
        this.vertices = vertices;
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<Vertice<T>> iteradorVertices = vertices.iterator();

        return new Iterator<T>() {

            @Override
            public boolean hasNext() {
                return iteradorVertices.hasNext();
            }

            @Override
            public T next() {
                return iteradorVertices.next().getDato();
            }
        };
    }

}