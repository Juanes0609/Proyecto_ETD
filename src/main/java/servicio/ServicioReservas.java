package servicio;

import excepciones.TransicionEstadoInvalidaException;
import modelo.EstadoReserva;
import modelo.Reserva;

public class ServicioReservas {

    public void cambiarEstado(Reserva reserva, EstadoReserva destino) {
        EstadoReserva actual = reserva.getEstadoReserva();
        if (!actual.puedeTransicionarA(destino)) {
            throw new TransicionEstadoInvalidaException(
                    "No se puede pasar de " + actual + " a " + destino);
        }
        reserva.setEstadoReserva(destino);
    }

    public void confirmar(Reserva reserva) {
        cambiarEstado(reserva, EstadoReserva.CONFIRMADA);
    }

    public void cancelar(Reserva reserva) {
        cambiarEstado(reserva, EstadoReserva.CANCELADA);
    }

    public void iniciarEstancia(Reserva reserva) {
        cambiarEstado(reserva, EstadoReserva.ENCURSO);
    }

    public void finalizar(Reserva reserva) {
        cambiarEstado(reserva, EstadoReserva.FINALIZADA);
    }

    public void marcarNoShow(Reserva reserva) {
        cambiarEstado(reserva, EstadoReserva.NOSHOW);
    }
}
