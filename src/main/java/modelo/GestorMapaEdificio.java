package modelo;

import estructuras.Grafo;
import estructuras.ListaDoblementeEnlazada;

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

        if (grafo.buscarVertice(nodo.getCodigo()) != null) {
            return;
        }

        Vertice<NodoEdificio> vertice =
                new Vertice<>(nodo, nodo.getCodigo());

        grafo.agregarVertice(vertice);
    }
    public void agregarApartamento(Apartamento apartamento) {
        if (apartamento == null) {
            throw new IllegalArgumentException(
                    "El apartamento no puede ser null"
            );
        }

        NodoEdificio nodo = apartamento.getNodoEdificio();

        if (nodo == null) {
            throw new IllegalArgumentException(
                    "El apartamento no tiene un nodo de edificio"
            );
        }

        if (grafo.buscarVertice(nodo.getCodigo()) != null) {
            return;
        }

        agregarUbicacion(nodo);
    }
    public void cargarApartamentos(SistemaGestionAlojamiento sistema) {
        if (sistema == null) {
            throw new IllegalArgumentException(
                    "El sistema de gestión es obligatorio"
            );
        }

        for (Apartamento apartamento : sistema.listarApartamentos()) {
            agregarApartamento(apartamento);
        }
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
    /**
     * Un pasillo real se recorre en los dos sentidos: crea la arista de ida
     * y la de vuelta con el mismo peso.
     */
    public void conectarPasillo(String codigoA, String codigoB, double distancia) {
        conectarUbicaciones(codigoA, codigoB, distancia);
        conectarUbicaciones(codigoB, codigoA, distancia);
    }

    public int contarUbicaciones() {
        return grafo.getVertices().getTam();
    }

    public int contarPasillos() {
        return grafo.getAristas().getTam();
    }

    /**
     * Apartamentos que NO se pueden alcanzar desde un punto,
     * por ejemplo si se cerró un pasillo.
     * Esto es lo que pide el rol de Administrador.
     */
    public ListaDoblementeEnlazada<NodoEdificio> apartamentosInalcanzables(
            String codigoInicio) {

        ListaDoblementeEnlazada<NodoEdificio> alcanzados =
                grafo.recorridoBFS(codigoInicio);

        ListaDoblementeEnlazada<NodoEdificio> inalcanzables =
                new ListaDoblementeEnlazada<>();

        for (Vertice<NodoEdificio> vertice : grafo.getVertices()) {
            NodoEdificio nodo = vertice.getDato();
            boolean esta = false;

            for (NodoEdificio a : alcanzados) {
                if (a.getCodigo().equals(nodo.getCodigo())) {
                    esta = true;
                    break;
                }
            }

            if (!esta) {
                inalcanzables.agregarFinal(nodo);
            }
        }

        return inalcanzables;
    }
    /**
     * Cierra el pasillo entre dos ubicaciones en AMBOS sentidos
     * (así el administrador no deja abierto el regreso por error).
     *
     * @return true si existía al menos una arista entre las dos ubicaciones
     */
    public boolean cerrarPasillo(String codigoA, String codigoB) {
        return cambiarEstadoPasillo(codigoA, codigoB, false);
    }

    public boolean abrirPasillo(String codigoA, String codigoB) {
        return cambiarEstadoPasillo(codigoA, codigoB, true);
    }

    /** true si el pasillo A-B existe y está habilitado en algún sentido. */
    public boolean pasilloAbierto(String codigoA, String codigoB) {
        for (Arista<NodoEdificio> arista : grafo.getAristas()) {
            if (conecta(arista, codigoA, codigoB) && arista.isHabilitada()) {
                return true;
            }
        }
        return false;
    }

    private boolean cambiarEstadoPasillo(String codigoA, String codigoB, boolean habilitada) {
        boolean encontrado = false;
        for (Arista<NodoEdificio> arista : grafo.getAristas()) {
            if (conecta(arista, codigoA, codigoB)) {
                arista.setHabilitada(habilitada);
                encontrado = true;
            }
        }
        return encontrado;
    }

    private boolean conecta(Arista<NodoEdificio> arista, String codigoA, String codigoB) {
        String origen = arista.getOrigen().getCodigo();
        String destino = arista.getDestino().getCodigo();
        return (origen.equals(codigoA) && destino.equals(codigoB))
                || (origen.equals(codigoB) && destino.equals(codigoA));
    }

    /**
     * 2. Ruta completa para recorrer VARIOS pendientes en orden de prioridad.
     * Une los Dijkstra tramo por tramo: REC -> A101 -> A102 -> ...
     * Esto es lo que pide el documento: "ruta para recorrer los pendientes".
     */
    public ListaDoblementeEnlazada<NodoEdificio> rutaParaPreparacion(
            String codigoInicio,
            ListaDoblementeEnlazada<OrdenPreparacion> pendientes) {

        ListaDoblementeEnlazada<NodoEdificio> ruta =
                new ListaDoblementeEnlazada<>();

        String actual = codigoInicio;

        for (OrdenPreparacion orden : pendientes) {
            ListaDoblementeEnlazada<NodoEdificio> tramo =
                    grafo.caminoMasCorto(actual, orden.getCodigo());

            boolean primero = true;
            for (NodoEdificio nodo : tramo) {
                // No repetir el nodo donde ya estamos
                if (primero && ruta.getTam() > 0) {
                    primero = false;
                    continue;
                }
                ruta.agregarFinal(nodo);
                primero = false;
            }

            actual = orden.getCodigo();
        }

        return ruta;
    }

    /** Ruta más corta (Dijkstra) entre dos ubicaciones; vacía si no hay camino. */
    public ListaDoblementeEnlazada<NodoEdificio> rutaMasCorta(
            String codigoOrigen, String codigoDestino) {
        return grafo.caminoMasCorto(codigoOrigen, codigoDestino);
    }

    /**
     * Suma de los pesos (metros) de una ruta ya calculada, tomando en cada
     * tramo el pasillo habilitado más corto entre los dos nodos.
     */
    public double distanciaDeRuta(ListaDoblementeEnlazada<NodoEdificio> ruta) {
        double total = 0;
        NodoEdificio anterior = null;
        for (NodoEdificio nodo : ruta) {
            if (anterior != null) {
                total += pesoDelTramo(anterior.getCodigo(), nodo.getCodigo());
            }
            anterior = nodo;
        }
        return total;
    }

    private double pesoDelTramo(String desde, String hasta) {
        double mejor = Double.POSITIVE_INFINITY;
        for (Arista<NodoEdificio> arista : grafo.getAristas()) {
            if (arista.isHabilitada()
                    && arista.getOrigen().getCodigo().equals(desde)
                    && arista.getDestino().getCodigo().equals(hasta)
                    && arista.getPeso() < mejor) {
                mejor = arista.getPeso();
            }
        }
        return mejor == Double.POSITIVE_INFINITY ? 0 : mejor;
    }

    /** Cantidad de pasillos (aristas) cerrados en este momento. */
    public int contarPasillosCerrados() {
        int cerrados = 0;
        for (Arista<NodoEdificio> arista : grafo.getAristas()) {
            if (!arista.isHabilitada()) {
                cerrados++;
            }
        }
        return cerrados;
    }

    /**
     * 3. Agrupar apartamentos por bloque.
     * Lo pide el rol Administrador: "agrupar por bloque".
     */
    public ListaDoblementeEnlazada<ListaDoblementeEnlazada<NodoEdificio>>
    agruparPorBloque() {

        ListaDoblementeEnlazada<ListaDoblementeEnlazada<NodoEdificio>> grupos =
                new ListaDoblementeEnlazada<>();

        ListaDoblementeEnlazada<String> bloquesVistos =
                new ListaDoblementeEnlazada<>();

        for (Vertice<NodoEdificio> vertice : grafo.getVertices()) {
            NodoEdificio nodo = vertice.getDato();

            if (nodo.getApartamento() == null) {
                continue; // solo apartamentos, no recepción / escaleras
            }

            String bloque = nodo.getApartamento().getBloque();

            // Buscar si ya existe el grupo de ese bloque
            ListaDoblementeEnlazada<NodoEdificio> grupo = null;
            for (ListaDoblementeEnlazada<NodoEdificio> g : grupos) {
                if (g.getTam() > 0
                        && g.getInicio().getDato().getApartamento() != null
                        && g.getInicio().getDato().getApartamento()
                        .getBloque().equals(bloque)) {
                    grupo = g;
                    break;
                }
            }

            if (grupo == null) {
                grupo = new ListaDoblementeEnlazada<>();
                grupos.agregarFinal(grupo);
                bloquesVistos.agregarFinal(bloque);
            }

            grupo.agregarFinal(nodo);
        }

        return grupos;
    }

    public Grafo<NodoEdificio> getGrafo() {
        return grafo;
    }
}
