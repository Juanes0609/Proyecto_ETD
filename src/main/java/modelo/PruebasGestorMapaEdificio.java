package modelo;

import java.util.ArrayList;
import java.util.List;

public class PruebasGestorMapaEdificio {
    private static int pruebasPasadas = 0;
    public static void main(String[] args){
        System.out.println("=== PRUEBAS GESTOR MAPA EDIFICIO ===\n");

        probarAgregarUbicaciones();
        probarUbicacionesDuplicadas();
        probarCargarApartamentos();
        probarBFS();
        probarDFS();
        probarCaminoMasCorto();
        probarMismoOrigenDestino();
        probarCodigoInexistente();
        probarDestinoDesconectado();
        probarAristaDeshabilitada();
        probarEliminarArista();
        probarEliminarVertice();

        System.out.println("\n==================================");
        System.out.println("Pruebas superadas: " + pruebasPasadas);
        System.out.println("Todas las pruebas terminaron.");
    }

    private static NodoEdificio nodo(
            String codigo, String nombre,
            TipoNodoEdificio tipo) {
        return new NodoEdificio(codigo, nombre, tipo);
    }

    private static GestorMapaEdificio crearMapa() {
        GestorMapaEdificio gestor = new GestorMapaEdificio();

        NodoEdificio recepcion = nodo(
                "REC-01", "Recepción", TipoNodoEdificio.RECEPCION);
        NodoEdificio escaleras = nodo(
                "ESC-01", "Escaleras", TipoNodoEdificio.ESCALERAS);
        NodoEdificio limpieza = nodo(
                "LIM-01", "Limpieza", TipoNodoEdificio.CUARTOLIMPIEZA);

        gestor.agregarUbicacion(recepcion);
        gestor.agregarUbicacion(escaleras);
        gestor.agregarUbicacion(limpieza);

        conectarAmbosSentidos(gestor, "REC-01", "ESC-01", 5);
        conectarAmbosSentidos(gestor, "ESC-01", "LIM-01", 3);

        return gestor;
    }

    private static void conectarAmbosSentidos(
            GestorMapaEdificio gestor,
            String origen, String destino, double distancia) {
        gestor.conectarUbicaciones(origen, destino, distancia);
        gestor.conectarUbicaciones(destino, origen, distancia);
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError("FALLÓ: " + mensaje);
        }

        pruebasPasadas++;
        System.out.println("OK: " + mensaje);
    }

    private static List<String> nombres(Iterable<NodoEdificio> recorrido) {
        List<String> resultado = new ArrayList<>();

        for (NodoEdificio nodo : recorrido) {
            resultado.add(nodo.getNombre());
        }

        return resultado;
    }

    private static void probarAgregarUbicaciones() {
        GestorMapaEdificio gestor = crearMapa();

        verificar(
                gestor.getGrafo().buscarVertice("REC-01") != null
                        && gestor.getGrafo().buscarVertice("ESC-01") != null
                        && gestor.getGrafo().buscarVertice("LIM-01") != null,
                "Agregar ubicaciones especiales"
        );
    }

    private static void probarUbicacionesDuplicadas() {
        GestorMapaEdificio gestor = crearMapa();

        gestor.agregarUbicacion(nodo(
                "REC-01", "Otra recepción",
                TipoNodoEdificio.RECEPCION));

        verificar(
                gestor.getGrafo().buscarVertice("REC-01") != null,
                "No falla al registrar un código duplicado"
        );
    }

    private static void probarCargarApartamentos() {
        SistemaGestionAlojamiento sistema =
                new SistemaGestionAlojamiento();

        Apartamento a101 = new Apartamento(
                "A101", "Apartamento 101", "A", 1, 2, 4,
                EstadoOperativo.PREPARADO, "Dos habitaciones", "");

        Apartamento a102 = new Apartamento(
                "A102", "Apartamento 102", "A", 1, 1, 2,
                EstadoOperativo.PREPARADO, "Una habitación", "");

        sistema.registrarApartamento(a101);
        sistema.registrarApartamento(a102);

        GestorMapaEdificio gestor = new GestorMapaEdificio();
        gestor.cargarApartamentos(sistema);

        verificar(
                gestor.getGrafo().buscarVertice("A101") != null
                        && gestor.getGrafo().buscarVertice("A102") != null,
                "Cargar apartamentos registrados en el sistema"
        );
    }

    private static void probarBFS() {
        GestorMapaEdificio gestor = crearMapa();

        verificar(
                nombres(gestor.getGrafo().recorridoBFS("REC-01"))
                        .equals(List.of("Recepción", "Escaleras", "Limpieza")),
                "BFS recorre las ubicaciones por niveles"
        );
    }

    private static void probarDFS() {
        GestorMapaEdificio gestor = crearMapa();

        verificar(
                nombres(gestor.getGrafo().recorridoDFS("REC-01"))
                        .equals(List.of("Recepción", "Escaleras", "Limpieza")),
                "DFS recorre las ubicaciones conectadas"
        );
    }

    private static void probarCaminoMasCorto() {
        GestorMapaEdificio gestor = crearMapa();

        verificar(
                nombres(gestor.getGrafo().caminoMasCorto(
                        "REC-01", "LIM-01"))
                        .equals(List.of("Recepción", "Escaleras", "Limpieza")),
                "Dijkstra encuentra el camino más corto"
        );
    }

    private static void probarMismoOrigenDestino() {
        GestorMapaEdificio gestor = crearMapa();

        verificar(
                nombres(gestor.getGrafo().caminoMasCorto(
                        "REC-01", "REC-01"))
                        .equals(List.of("Recepción")),
                "Camino de un vértice hacia sí mismo"
        );
    }

    private static void probarCodigoInexistente() {
        GestorMapaEdificio gestor = crearMapa();

        verificar(
                nombres(gestor.getGrafo().recorridoBFS("NO-EXISTE"))
                        .isEmpty(),
                "BFS con código de inicio inexistente"
        );

        verificar(
                nombres(gestor.getGrafo().caminoMasCorto(
                        "REC-01", "NO-EXISTE")).isEmpty(),
                "Dijkstra con destino inexistente"
        );
    }

    private static void probarDestinoDesconectado() {
        GestorMapaEdificio gestor = crearMapa();

        gestor.agregarUbicacion(nodo(
                "AIS-01", "Zona aislada",
                TipoNodoEdificio.CUARTOLIMPIEZA));

        verificar(
                nombres(gestor.getGrafo().caminoMasCorto(
                        "REC-01", "AIS-01")).isEmpty(),
                "No existe camino hacia un destino desconectado"
        );
    }

    private static void probarAristaDeshabilitada() {
        GestorMapaEdificio gestor = crearMapa();

        Vertice<NodoEdificio> origen =
                gestor.getGrafo().buscarVertice("REC-01");

        boolean deshabilitada = false;

        for (Arista<NodoEdificio> arista : origen.getListAristas()) {
            if (arista.getDestino().getCodigo().equals("ESC-01")) {
                arista.setHabilitada(false);
                deshabilitada = true;
                break;
            }
        }

        System.out.println("¿Se encontró la arista? " + deshabilitada);

        System.out.println("Camino después de deshabilitar:");
        List<String> camino = nombres(
                gestor.getGrafo().caminoMasCorto("REC-01", "LIM-01")
        );

        System.out.println(camino);

        verificar(
                deshabilitada && camino.isEmpty(),
                "Deshabilitar una arista impide utilizarla en el camino"
        );
    }

    private static void probarEliminarArista() {
        GestorMapaEdificio gestor = crearMapa();

        gestor.getGrafo().eliminarArista(
                gestor.getGrafo().buscarVertice("REC-01")
                        .getListAristas().getInicio().getDato());

        verificar(
                nombres(gestor.getGrafo().recorridoBFS("REC-01"))
                        .size() < 3,
                "Eliminar una arista modifica el recorrido"
        );
    }

    private static void probarEliminarVertice() {
        GestorMapaEdificio gestor = crearMapa();

        gestor.getGrafo().eliminarVertice("LIM-01");

        verificar(
                gestor.getGrafo().buscarVertice("LIM-01") == null,
                "Eliminar un vértice del grafo"
        );
    }


}
