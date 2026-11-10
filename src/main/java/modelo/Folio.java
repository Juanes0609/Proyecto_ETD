package modelo;

import java.util.List;

public class Folio {
    private double saldoActual;
    private Reserva reserva;
    private List<MovimientoFolio> listMovimientosFolio;

    public Folio(double saldoActual, Reserva reserva) {

        this.saldoActual = saldoActual;
        this.reserva = reserva;
    }
    public double getSaldoActual() {
        return saldoActual;
    }
    public void setSaldoActual(double saldoActual) {
        this.saldoActual = saldoActual;
    }

    public List<MovimientoFolio> getListMovimientosFolio() {
        return listMovimientosFolio;
    }

    public void setListMovimientosFolio(List<MovimientoFolio> listMovimientosFolio) {
        this.listMovimientosFolio = listMovimientosFolio;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }
}
