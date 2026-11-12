package modelo;

public class PruebasGestorMapaEdificio {
    public static void main(String[] args){
        GestorMapaEdificio gestor = new GestorMapaEdificio();

        // Crear ubicaciones especiales
        NodoEdificio recepcion = new NodoEdificio(
                "REC-01", "Recepción", TipoNodoEdificio.RECEPCION
        );

        NodoEdificio escaleras = new NodoEdificio(
                "ESC-01", "Escaleras principales", TipoNodoEdificio.ESCALERAS
        );

        NodoEdificio limpieza = new NodoEdificio(
                "LIM-01", "Cuarto de limpieza", TipoNodoEdificio.CUARTOLIMPIEZA
        );

        // Agregarlas al grafo
        gestor.agregarUbicacion(recepcion);
        gestor.agregarUbicacion(escaleras);
        gestor.agregarUbicacion(limpieza);

        gestor.conectarUbicaciones("REC-01", "ESC-01", 5.0);
        gestor.conectarUbicaciones("ESC-01", "LIM-01", 3.0);

        System.out.println("Recorrido BFS:");
        for (NodoEdificio nodo : gestor.getGrafo().recorridoBFS("REC-01")) {
            System.out.println(nodo.getNombre());
        }

        System.out.println("Camino más corto:");
        for (NodoEdificio nodo :
                gestor.getGrafo().caminoMasCorto("REC-01", "LIM-01")) {
            System.out.println(nodo.getNombre());
        }
    }
}
