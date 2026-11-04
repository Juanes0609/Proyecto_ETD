package gui.integracion.dto;

/** Nodo del mapa del edificio con coordenadas para dibujarlo. */
public record NodoEdificioDTO(String codigo, String nombre, String tipo, String bloque, double x, double y) {
}
