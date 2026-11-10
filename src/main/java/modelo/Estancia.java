package modelo;

import java.time.LocalDate;
import java.util.List;

public class Estancia {
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private Reserva reserva;
    private List<DetalleNoche> listDetalleNoche;

    public Estancia(LocalDate fechaEntrada, LocalDate fechaSalida, Reserva reserva) {
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.reserva = reserva;
    }

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }

    public void setFechaEntrada(LocalDate fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public List<DetalleNoche> getListDetalleNoche() {
        return listDetalleNoche;
    }

    public void setListDetalleNoche(List<DetalleNoche> listDetalleNoche) {
        this.listDetalleNoche = listDetalleNoche;
    }
}
