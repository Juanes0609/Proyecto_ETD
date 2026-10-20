package excepciones;

/**
 * Se lanza al intentar extraer o consultar elementos en una
 * estructura de datos que no contiene elementos.
 */
public class EstructuraVaciaException extends RuntimeException {
    public EstructuraVaciaException(String mensaje) {
        super(mensaje);
    }
}
