package excepciones;

/**
 * Se lanza cuando se intenta realizar un cambio de estado no permitido
 * según las reglas operativas del negocio.
 */
public class TransicionEstadoInvalidaException extends RuntimeException {
    public TransicionEstadoInvalidaException(String mensaje) {
        super(mensaje);
    }
}
