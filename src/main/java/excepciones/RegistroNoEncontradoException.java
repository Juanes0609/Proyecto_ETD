package excepciones;

public class RegistroNoEncontradoException extends RuntimeException {
    public RegistroNoEncontradoException(String message) {
        super(message);
    }
}
