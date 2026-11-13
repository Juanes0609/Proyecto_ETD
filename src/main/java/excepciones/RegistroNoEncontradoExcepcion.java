package excepciones;

public class RegistroNoEncontradoExcepcion extends RuntimeException {
    public RegistroNoEncontradoExcepcion(String message) {
        super(message);
    }
}
