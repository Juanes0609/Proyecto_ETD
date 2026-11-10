package modelo;

import java.time.LocalDate;

public class DetalleNoche {
    private LocalDate fecha;
    private int cantidadPersonasPagas;
    private double tarifaPorPersona;
    private double subtotal;
    private Temporada temporada;

    public DetalleNoche(LocalDate fecha, int cantidadPersonasPagas, double tarifaPorPersona,  double subtotal, Temporada temporada) {
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

    public Temporada getTemporada() {
        return temporada;
    }

    public void setTemporada(Temporada temporada) {
        this.temporada = temporada;
    }
}
