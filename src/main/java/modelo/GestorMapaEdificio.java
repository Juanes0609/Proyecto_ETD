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
     * 1. Cerrar un pasillo desde el gestor, sin tocar la arista a mano.
     * Esto es lo que usa el Administrador para simular "se cerró un pasillo".
     */
    public boolean cerrarPasillo(String codigoOrigen, String codigoDestino) {
        for (Arista<NodoEdificio> arista : grafo.getAristas()) {
            if (arista.getOrigen().getCodigo().equals(codigoOrigen)
                    && arista.getDestino().getCodigo().equals(codigoDestino)) {
                arista.setHabilitada(false);
                return true;
            }
        }
        return false;
    }

    public boolean abrirPasillo(String codigoOrigen, String codigoDestino) {
        for (Arista<NodoEdificio> arista : grafo.getAristas()) {
            if (arista.getOrigen().getCodigo().equals(codigoOrigen)
                    && arista.getDestino().getCodigo().equals(codigoDestino)) {
                arista.setHabilitada(true);
                return true;
            }
        }
        return false;
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
