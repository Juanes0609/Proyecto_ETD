import excepciones.TransicionEstadoInvalidaException;
import modelo.*;
import servicio.ServicioReservas;
import java.time.LocalDateTime;

public class PruebaReservas {
    public static void main(String[] args) {
        Reserva r = new Reserva("R1", LocalDateTime.now(), LocalDateTime.now().plusHours(24),
                EstadoReserva.PENDIENTE, Canal.PORTAL, null, null, null);
        ServicioReservas servicio = new ServicioReservas(null);

        servicio.confirmar(r);
        System.out.println(r.getEstadoReserva());     // CONFIRMADA
        servicio.iniciarEstancia(r);
        System.out.println(r.getEstadoReserva());     // ENCURSO
        servicio.finalizar(r);
        System.out.println(r.getEstadoReserva());     // FINALIZADA

        try {
            servicio.cancelar(r);                      // ilegal: ya finalizó
        } catch (TransicionEstadoInvalidaException e) {
            System.out.println("Error esperado: " + e.getMessage());
        }
    }
}