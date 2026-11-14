package excepciones;

/** Ya existe un registro con la misma clave (documento o código). */
public class RegistroDuplicadoException extends RuntimeException {
    public RegistroDuplicadoException(String mensaje) {
        super(mensaje);
    }
}