package excepciones;

/**
 * Se lanza cuando el rango de fechas de una instancia es
 * incoherente como fechas nulas, fecha de salida previa o igual
 * a la de entrada, o fechas pasadas
 */
public class FechasInvalidasException extends RuntimeException {
    public FechasInvalidasException(String mensaje) {
        super(mensaje);
    }
}
