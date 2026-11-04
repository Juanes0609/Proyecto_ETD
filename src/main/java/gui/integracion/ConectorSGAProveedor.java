package gui.integracion;

/**
 * Implementación de {@link ConectorSGA} que delegará en
 * {@code SistemaGestionAlojamiento}. Cada método se sobrescribe aquí a medida
 * que el núcleo (modelo, estructuras y servicio) quede implementado; mientras
 * tanto se heredan los métodos por defecto, que avisan "en construcción".
 * <p>
 * Para usarlo, cambiar {@code new ConectorSGADemo()} por
 * {@code new ConectorSGAProveedor()} en {@code AplicacionSGA}.
 */
public class ConectorSGAProveedor implements ConectorSGA {
}
