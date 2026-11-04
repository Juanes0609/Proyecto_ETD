package gui.integracion.dto;

/** Pasillo entre dos nodos del edificio; puede estar cerrado. */
public record ConexionDTO(String origen, String destino, double metros, boolean habilitada) {
}
