package modelo;

import estructuras.Grafo;
import estructuras.ListaDoblementeEnlazada;

public class PruebaGrafo {
    public static void main(String[] args) {

        System.out.println("====================================");
        System.out.println("          PRUEBAS DEL GRAFO");
        System.out.println("====================================");

        // ==========================================
        // 1. PROBAR ELIMINACION DE LA LISTA
        // ==========================================

        System.out.println("\n1. PRUEBA DE LA LISTA DOBLE");

        ListaDoblementeEnlazada<String> lista =
                new ListaDoblementeEnlazada<>();

        lista.agregarFinal("A");
        lista.agregarFinal("B");
        lista.agregarFinal("C");

        System.out.println(
                "Eliminar B: " + lista.eliminar("B")
        );

        System.out.println(
                "Intentar eliminar B otra vez: "
                        + lista.eliminar("B")
        );

        System.out.println("Elementos restantes:");

        for (String elemento : lista) {
            System.out.println(elemento);
        }

        // ==========================================
        // 2. CREAR EL GRAFO Y LOS LUGARES
        // ==========================================

        System.out.println("\n2. CREAR EL GRAFO");

        Grafo<NodoEdificio> grafo = new Grafo<>();

        NodoEdificio nodoRecepcion = new NodoEdificio(
                "REC",
                "Recepcion",
                TipoNodoEdificio.RECEPCION
        );

        NodoEdificio nodoEscaleras = new NodoEdificio(
                "ESC",
                "Escaleras",
                TipoNodoEdificio.ESCALERAS
        );

        NodoEdificio nodoApartamento = new NodoEdificio(
                "APT101",
                "Apartamento 101",
                TipoNodoEdificio.APARTAMENTO
        );

        Vertice<NodoEdificio> recepcion =
                new Vertice<>(nodoRecepcion, "REC");

        Vertice<NodoEdificio> escaleras =
                new Vertice<>(nodoEscaleras, "ESC");

        Vertice<NodoEdificio> apartamento =
                new Vertice<>(nodoApartamento, "APT101");

        grafo.agregarVertice(recepcion);
        grafo.agregarVertice(escaleras);
        grafo.agregarVertice(apartamento);

        System.out.println("Se agregaron los tres vertices.");

        // ==========================================
        // 3. PROBAR BUSQUEDA DE VERTICES
        // ==========================================

        System.out.println("\n3. BUSQUEDA DE VERTICES");

        System.out.println(
                "Buscar REC: "
                        + (grafo.buscarVertice("REC") == recepcion)
        );

        System.out.println(
                "Buscar ESC: "
                        + (grafo.buscarVertice("ESC") == escaleras)
        );

        System.out.println(
                "Buscar inexistente: "
                        + (grafo.buscarVertice("XXX") == null)
        );

        // ==========================================
        // 4. CREAR Y AGREGAR ARISTAS
        // ==========================================

        System.out.println("\n4. AGREGAR ARISTAS");

        Arista<NodoEdificio> conexion1 =
                new Arista<>(recepcion, escaleras, 8.0, true);

        Arista<NodoEdificio> conexion2 =
                new Arista<>(recepcion, apartamento, 5.0, true);

        grafo.agregarArista(conexion1);
        grafo.agregarArista(conexion2);

        System.out.println(
                "Peso REC-ESC: " + conexion1.getPeso()
        );

        System.out.println(
                "Peso REC-APT101: " + conexion2.getPeso()
        );

        // ==========================================
        // 5. VERIFICAR LISTAS DE ADYACENCIA
        // ==========================================

        System.out.println("\n5. LISTAS DE ADYACENCIA");

        System.out.println(
                "REC contiene conexion1: "
                        + contieneArista(recepcion, conexion1)
        );

        System.out.println(
                "ESC contiene conexion1: "
                        + contieneArista(escaleras, conexion1)
        );

        System.out.println(
                "REC contiene conexion2: "
                        + contieneArista(recepcion, conexion2)
        );

        System.out.println(
                "APT101 contiene conexion2: "
                        + contieneArista(apartamento, conexion2)
        );

        // ==========================================
        // 6. PROBAR ITERADOR DEL GRAFO
        // ==========================================

        System.out.println("\n6. ITERADOR DEL GRAFO");

        for (NodoEdificio nodo : grafo) {
            System.out.println(
                    nodo.getCodigo() + " - " + nodo.getNombre()
            );
        }

        // ==========================================
        // 7. PROBAR BFS ANTES DE ELIMINAR ARISTAS
        // ==========================================

        System.out.println("\n7. RECORRIDO BFS DESDE REC");

        ListaDoblementeEnlazada<NodoEdificio> recorrido =
                grafo.recorridoBFS("REC");

        for (NodoEdificio nodo : recorrido) {
            System.out.println(
                    nodo.getCodigo() + " - " + nodo.getNombre()
            );
        }

        System.out.println("\n=== PRUEBA DFS ===");

        ListaDoblementeEnlazada<NodoEdificio> recorridoProfundidad =
                grafo.recorridoDFS("REC");

        for (NodoEdificio nodo : recorridoProfundidad) {
            System.out.println(
                    nodo.getCodigo() + " - " + nodo.getNombre()
            );
        }

        System.out.println("\n=== PRUEBAS DIJKSTRA ===");

// Prueba 1: Camino más corto de REC a APT101
        ListaDoblementeEnlazada<NodoEdificio> camino =
                grafo.caminoMasCorto("REC", "APT101");

        System.out.println("Camino de REC a APT101:");

        for (NodoEdificio nodo : camino) {
            System.out.print(nodo.getCodigo() + " -> ");
        }
        System.out.println("FIN");

// Prueba 2: El destino es el mismo vértice de inicio
        ListaDoblementeEnlazada<NodoEdificio> mismoNodo =
                grafo.caminoMasCorto("REC", "REC");

        System.out.println("\nCamino de REC a REC:");

        for (NodoEdificio nodo : mismoNodo) {
            System.out.print(nodo.getCodigo() + " -> ");
        }
        System.out.println("FIN");

// Prueba 3: El vértice de inicio no existe
        ListaDoblementeEnlazada<NodoEdificio> inicioInexistente =
                grafo.caminoMasCorto("XXX", "APT101");

        System.out.println("\nCamino desde XXX: "
                + (inicioInexistente.iterator().hasNext()
                ? "Se encontró un camino"
                : "No existe camino"));

// Prueba 4: El destino no existe
        ListaDoblementeEnlazada<NodoEdificio> destinoInexistente =
                grafo.caminoMasCorto("REC", "XXX");

        System.out.println("Camino hacia XXX: "
                + (destinoInexistente.iterator().hasNext()
                ? "Se encontró un camino"
                : "No existe camino"));

// Prueba 5: Destino desconectado
        Vertice<NodoEdificio> aislado = new Vertice<>(
                new NodoEdificio(
                        "APT999",
                        "Apartamento aislado",
                        TipoNodoEdificio.APARTAMENTO
                ),
                "APT999"
        );

        grafo.agregarVertice(aislado);

        ListaDoblementeEnlazada<NodoEdificio> sinConexion =
                grafo.caminoMasCorto("REC", "APT999");

        System.out.println("Camino hacia APT999: "
                + (sinConexion.iterator().hasNext()
                ? "Se encontró un camino"
                : "No existe camino"));

        grafo.eliminarVertice("APT999");

        // ==========================================
        // 8. PROBAR BFS CON CODIGO INEXISTENTE
        // ==========================================

        System.out.println("\n8. BFS DESDE VERTICE INEXISTENTE");

        ListaDoblementeEnlazada<NodoEdificio> recorridoVacio =
                grafo.recorridoBFS("XXX");

        boolean recorridoVacioCorrecto = true;

        for (NodoEdificio nodo : recorridoVacio) {
            recorridoVacioCorrecto = false;
        }

        System.out.println(
                "El recorrido esta vacio: "
                        + recorridoVacioCorrecto
        );

        // ==========================================
        // 9. ELIMINAR UNA ARISTA DEL GRAFO
        // ==========================================

        System.out.println("\n9. ELIMINAR ARISTA");

        boolean aristaEliminada =
                grafo.eliminarArista(conexion1);

        System.out.println(
                "Se elimino conexion1: " + aristaEliminada
        );

        System.out.println(
                "REC ya no contiene conexion1: "
                        + !contieneArista(recepcion, conexion1)
        );

        System.out.println(
                "ESC ya no contiene conexion1: "
                        + !contieneArista(escaleras, conexion1)
        );

        System.out.println(
                "Intentar eliminarla otra vez: "
                        + grafo.eliminarArista(conexion1)
        );

        // ==========================================
        // 10. ELIMINAR UN VERTICE
        // ==========================================

        System.out.println("\n10. ELIMINAR VERTICE REC");

        boolean verticeEliminado =
                grafo.eliminarVertice("REC");

        System.out.println(
                "Se elimino REC: " + verticeEliminado
        );

        System.out.println(
                "REC ya no existe: "
                        + (grafo.buscarVertice("REC") == null)
        );

        System.out.println(
                "ESC sigue existiendo: "
                        + (grafo.buscarVertice("ESC") == escaleras)
        );

        System.out.println(
                "APT101 sigue existiendo: "
                        + (grafo.buscarVertice("APT101") == apartamento)
        );

        System.out.println(
                "La conexion2 fue eliminada de APT101: "
                        + !contieneArista(apartamento, conexion2)
        );

        // ==========================================
        // 11. INTENTAR ELIMINAR VERTICE INEXISTENTE
        // ==========================================

        System.out.println("\n11. ELIMINAR VERTICE INEXISTENTE");

        System.out.println(
                "Eliminar REC otra vez: "
                        + grafo.eliminarVertice("REC")
        );

        System.out.println("\n====================================");
        System.out.println("         FIN DE LAS PRUEBAS");
        System.out.println("====================================");
    }

    // ==========================================
    // METODO AUXILIAR
    // Comprueba si un vertice contiene una arista
    // sin modificar su lista.
    // ==========================================

    private static boolean contieneArista(
            Vertice<NodoEdificio> vertice,
            Arista<NodoEdificio> buscada) {

        for (Arista<NodoEdificio> arista
                : vertice.getListAristas()) {

            if (arista == buscada) {
                return true;
            }
        }

        return false;
    }

}
