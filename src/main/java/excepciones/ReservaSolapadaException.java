package excepciones;

/**
 * Se lanza cuando se intenta registrar una reserva en un apartamento
 * cuyas fechas se solapan con al menos una noche de una reserva activa
 * existente.
 */
public class ReservaSolapadaException extends RuntimeException {
    public ReservaSolapadaException(String mensaje) {
        super(mensaje);
    }
}
