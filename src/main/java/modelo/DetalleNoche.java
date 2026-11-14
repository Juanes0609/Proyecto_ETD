package modelo;

import java.time.LocalDate;

public class DetalleNoche {
    private LocalDate fecha;
    private int cantidadPersonasPagas;
    private double tarifaPorPersona;
    private double subtotal;
    private TipoTemporada temporada;

    public DetalleNoche(LocalDate fecha, int cantidadPersonasPagas, double tarifaPorPersona,  double subtotal, TipoTemporada temporada) {
        this.fecha = fecha;
        this.cantidadPersonasPagas = cantidadPersonasPagas;
        this.tarifaPorPersona = tarifaPorPersona;
        this.subtotal = subtotal;
        this.temporada = temporada;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public int getCantidadPersonasPagas() {
        return cantidadPersonasPagas;
    }

    public void setCantidadPersonasPagas(int cantidadPersonasPagas) {
        this.cantidadPersonasPagas = cantidadPersonasPagas;
    }

    public double getTarifaPorPersona() {
        return tarifaPorPersona;
    }

    public void setTarifaPorPersona(double tarifaPorPersona) {
        this.tarifaPorPersona = tarifaPorPersona;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public TipoTemporada getTemporada() {
        return temporada;
    }

    public void setTemporada(TipoTemporada temporada) {
        this.temporada = temporada;
    }
}
