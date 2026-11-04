package gui.integracion.dto;

import java.util.List;

/** Ruta más corta en el edificio: nodos recorridos y distancia total en metros. */
public record RutaDTO(List<String> pasos, double metros) {
}
