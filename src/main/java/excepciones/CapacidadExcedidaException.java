package excepciones;

/**
 * Se lanza cuando el número total de ocupantes supera la
 * capacidad máxima permitida por el apartamento.
 */
public class CapacidadExcedidaException extends RuntimeException {
    public CapacidadExcedidaException(String mensaje) {
        super(mensaje);
    }
}
