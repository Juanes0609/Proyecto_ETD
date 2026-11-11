package modelo;

import estructuras.Grafo;
import estructuras.ListaDoblementeEnlazada;

public class PruebaGrafo {
    public static void main(String[] args) {

        System.out.println("====================================");
        System.out.println("          PRUEBAS DEL GRAFO");
        System.out.println("====================================");

        // 1. PROBAR LA LISTA DOBLEMENTE ENLAZADA
        System.out.println("\n1. PRUEBA DE LA LISTA DOBLE");

        ListaDoblementeEnlazada<String> lista =
                new ListaDoblementeEnlazada<>();

        lista.agregarFinal("A");
        lista.agregarFinal("B");
        lista.agregarFinal("C");

        System.out.println("Eliminar B: " + lista.eliminar("B"));
        System.out.println("Eliminar B otra vez: " + lista.eliminar("B"));

        System.out.print("Elementos restantes: ");
        for (String elemento : lista) {
            System.out.print(elemento + " ");
        }
        System.out.println();


        // 2. CREAR EL GRAFO Y LOS VERTICES
        System.out.println("\n2. CREAR EL GRAFO");

        Grafo<NodoEdificio> grafo = new Grafo<>();

        NodoEdificio nodoRecepcion = new NodoEdificio(
                "REC", "Recepcion", TipoNodoEdificio.RECEPCION);

        NodoEdificio nodoEscaleras = new NodoEdificio(
                "ESC", "Escaleras", TipoNodoEdificio.ESCALERAS);

        NodoEdificio nodoApartamento = new NodoEdificio(
                "APT101", "Apartamento 101",
                TipoNodoEdificio.APARTAMENTO);

        Vertice<NodoEdificio> recepcion =
                new Vertice<>(nodoRecepcion, "REC");

        Vertice<NodoEdificio> escaleras =
                new Vertice<>(nodoEscaleras, "ESC");

        Vertice<NodoEdificio> apartamento =
                new Vertice<>(nodoApartamento, "APT101");

        grafo.agregarVertice(recepcion);
        grafo.agregarVertice(escaleras);
        grafo.agregarVertice(apartamento);

        System.out.println("Vértices agregados: REC, ESC, APT101");


        // 3. PROBAR BUSQUEDA
        System.out.println("\n3. BUSQUEDA DE VERTICES");

        System.out.println("Buscar REC: "
                + (grafo.buscarVertice("REC") == recepcion));

        System.out.println("Buscar ESC: "
                + (grafo.buscarVertice("ESC") == escaleras));

        System.out.println("Buscar XXX: "
                + (grafo.buscarVertice("XXX") == null));


        // 4. AGREGAR LAS TRES ARISTAS
        System.out.println("\n4. AGREGAR ARISTAS");

        Arista<NodoEdificio> conexion1 =
                new Arista<>(recepcion, escaleras, 8.0, true);

        Arista<NodoEdificio> conexion2 =
                new Arista<>(recepcion, apartamento, 15.0, true);

        Arista<NodoEdificio> conexion3 =
                new Arista<>(escaleras, apartamento, 2.0, true);

        grafo.agregarArista(conexion1);
        grafo.agregarArista(conexion2);
        grafo.agregarArista(conexion3);

        System.out.println("REC - ESC: peso " + conexion1.getPeso());
        System.out.println("REC - APT101: peso " + conexion2.getPeso());
        System.out.println("ESC - APT101: peso " + conexion3.getPeso());


        // 5. VERIFICAR LAS LISTAS DE ADYACENCIA
        System.out.println("\n5. LISTAS DE ADYACENCIA");

        System.out.println("REC contiene conexion1: "
                + contieneArista(recepcion, conexion1));

        System.out.println("ESC contiene conexion1: "
                + contieneArista(escaleras, conexion1));

        System.out.println("REC contiene conexion2: "
                + contieneArista(recepcion, conexion2));

        System.out.println("APT101 contiene conexion2: "
                + contieneArista(apartamento, conexion2));

        System.out.println("ESC contiene conexion3: "
                + contieneArista(escaleras, conexion3));

        System.out.println("APT101 contiene conexion3: "
                + contieneArista(apartamento, conexion3));

        System.out.println("\nESTADO DE LAS ARISTAS");
        System.out.println("REC-ESC habilitada: "
                + conexion1.isHabilitada());
        System.out.println("REC-APT101 habilitada: "
                + conexion2.isHabilitada());
        System.out.println("ESC-APT101 habilitada: "
                + conexion3.isHabilitada());


        // 6. ITERADOR DEL GRAFO
        System.out.println("\n6. ITERADOR DEL GRAFO");

        for (NodoEdificio nodo : grafo) {
            System.out.println(nodo.getCodigo()
                    + " - " + nodo.getNombre());
        }


        // 7. RECORRIDO BFS
        System.out.println("\n7. RECORRIDO BFS DESDE REC");

        ListaDoblementeEnlazada<NodoEdificio> recorridoBFS =
                grafo.recorridoBFS("REC");

        imprimirRecorrido(recorridoBFS);


        // 8. RECORRIDO DFS
        System.out.println("\n8. RECORRIDO DFS DESDE REC");

        ListaDoblementeEnlazada<NodoEdificio> recorridoDFS =
                grafo.recorridoDFS("REC");

        imprimirRecorrido(recorridoDFS);


        // 9. DIJKSTRA: CAMINO MAS CORTO
        System.out.println("\n9. PRUEBAS DIJKSTRA");

        ListaDoblementeEnlazada<NodoEdificio> camino =
                grafo.caminoMasCorto("REC", "APT101");

        System.out.println("Camino de REC a APT101:");
        imprimirRecorrido(camino);

        System.out.println("Camino esperado: REC -> ESC -> APT101");
        System.out.println("Peso esperado: 10 (8 + 2; directo = 15)");


        System.out.println("\nPRUEBA DIJKSTRA CON RUTA ALTERNATIVA");

        // Deshabilitamos la conexión entre escaleras y apartamento
        conexion3.setHabilitada(false);

        System.out.println("Conexion ESC-APT101 deshabilitada: "
                + !conexion3.isHabilitada());

        System.out.println("Camino alternativo de REC a APT101:");

        ListaDoblementeEnlazada<NodoEdificio> caminoAlternativo =
                grafo.caminoMasCorto("REC", "APT101");

        for (NodoEdificio nodo : caminoAlternativo) {
            System.out.print(nodo.getCodigo() + " -> ");
        }

        System.out.println("FIN");
        // Restauramos la conexión
        conexion3.setHabilitada(true);



        // 10. DIJKSTRA: MISMO ORIGEN Y DESTINO
        System.out.println("\n10. DIJKSTRA: MISMO VERTICE");

        ListaDoblementeEnlazada<NodoEdificio> mismoNodo =
                grafo.caminoMasCorto("REC", "REC");

        imprimirRecorrido(mismoNodo);


        // 11. DIJKSTRA: CODIGOS INEXISTENTES
        System.out.println("\n11. DIJKSTRA: CODIGOS INEXISTENTES");

        System.out.println("Inicio inexistente, camino vacío: "
                + !grafo.caminoMasCorto("XXX", "APT101").iterator().hasNext());

        System.out.println("Destino inexistente, camino vacío: "
                + !grafo.caminoMasCorto("REC", "XXX").iterator().hasNext());

        // 12. RECORRIDOS DESDE UN VERTICE INEXISTENTE
        System.out.println("\n12. RECORRIDOS INEXISTENTES");

        System.out.println("BFS vacío: "
                + !grafo.recorridoBFS("XXX").iterator().hasNext());

        System.out.println("DFS vacío: "
                + !grafo.recorridoDFS("XXX").iterator().hasNext());


        // 13. DESTINO AISLADO
        System.out.println("\n13. DESTINO AISLADO");

        NodoEdificio nodoAislado = new NodoEdificio(
                "APT999", "Apartamento aislado",
                TipoNodoEdificio.APARTAMENTO);

        Vertice<NodoEdificio> aislado =
                new Vertice<>(nodoAislado, "APT999");

        grafo.agregarVertice(aislado);

        ListaDoblementeEnlazada<NodoEdificio> sinConexion =
                grafo.caminoMasCorto("REC", "APT999");

        System.out.println("Camino hacia APT999 vacío: "
                + !sinConexion.iterator().hasNext());

        grafo.eliminarVertice("APT999");


        System.out.println("\n17. PRUEBA DE ARISTAS DESHABILITADAS");

// Deshabilitamos las dos conexiones que salen de REC
        conexion1.setHabilitada(false);
        conexion2.setHabilitada(false);

        System.out.println("REC-ESC deshabilitada: "
                + !conexion1.isHabilitada());

        System.out.println("REC-APT101 deshabilitada: "
                + !conexion2.isHabilitada());

// REC debe ser el único vértice alcanzable desde REC
        System.out.println("BFS solo alcanza REC: "
                + (grafo.recorridoBFS("REC").getTam() == 1));

// Dijkstra no debe encontrar camino hacia APT101
        System.out.println("Dijkstra no encuentra camino: "
                + !grafo.caminoMasCorto("REC", "APT101").iterator().hasNext());

// Restauramos las conexiones para las pruebas siguientes
        conexion1.setHabilitada(true);
        conexion2.setHabilitada(true);

        System.out.println("Aristas restauradas: "
                + (conexion1.isHabilitada() && conexion2.isHabilitada()));


        // 14. ELIMINAR UNA ARISTA
        System.out.println("\n14. ELIMINAR ARISTA");

        System.out.println("Eliminar conexion1: "
                + grafo.eliminarArista(conexion1));

        System.out.println("REC ya no contiene conexion1: "
                + !contieneArista(recepcion, conexion1));

        System.out.println("ESC ya no contiene conexion1: "
                + !contieneArista(escaleras, conexion1));

        System.out.println("Eliminarla otra vez: "
                + grafo.eliminarArista(conexion1));


        // 15. ELIMINAR UN VERTICE
        System.out.println("\n15. ELIMINAR VERTICE REC");

        System.out.println("Eliminar REC: "
                + grafo.eliminarVertice("REC"));

        System.out.println("REC ya no existe: "
                + (grafo.buscarVertice("REC") == null));

        System.out.println("ESC sigue existiendo: "
                + (grafo.buscarVertice("ESC") == escaleras));

        System.out.println("APT101 sigue existiendo: "
                + (grafo.buscarVertice("APT101") == apartamento));

        System.out.println("conexion2 eliminada de APT101: "
                + !contieneArista(apartamento, conexion2));


        // 16. INTENTAR ELIMINAR UN VERTICE INEXISTENTE
        System.out.println("\n16. ELIMINAR VERTICE INEXISTENTE");

        System.out.println("Eliminar REC otra vez: "
                + grafo.eliminarVertice("REC"));


        System.out.println("\n====================================");
        System.out.println("          FIN DE LAS PRUEBAS");
        System.out.println("====================================");
    }


    // Imprime los elementos de un recorrido o camino.
    private static void imprimirRecorrido(
            ListaDoblementeEnlazada<NodoEdificio> recorrido) {

        if (!recorrido.iterator().hasNext()) {
            System.out.println("FIN (camino vacío)");
            return;
        }

        for (NodoEdificio nodo : recorrido) {
            System.out.print(nodo.getCodigo() + " -> ");
        }

        System.out.println("FIN");
    }


    // Comprueba si un vertice contiene una arista concreta.
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
