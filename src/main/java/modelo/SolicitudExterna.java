package modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SolicitudExterna {
    private String codigo;
    private LocalDateTime fechaSolicitud;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private int cantidadPersonas;
    private String codigoApartamentoSolicitado;
    private Reserva reserva;
    private ConflictoCanal conflictoCanal;

    public SolicitudExterna(String codigo, LocalDateTime fechaSolicitud, LocalDate fechaEntrada, LocalDate fechaSalida, int cantidadPersonas,  String codigoApartamentoSolicitado) {
        this.codigo = codigo;
        this.fechaSolicitud = fechaSolicitud;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.cantidadPersonas = cantidadPersonas;
        this.codigoApartamentoSolicitado = codigoApartamentoSolicitado;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
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

    public int getCantidadPersonas() {
        return cantidadPersonas;
    }

    public void setCantidadPersonas(int cantidadPersonas) {
        this.cantidadPersonas = cantidadPersonas;
    }

    public String getCodigoApartamentoSolicitado() {
        return codigoApartamentoSolicitado;
    }

    public void setCodigoApartamentoSolicitado(String codigoApartamentoSolicitado) {
        this.codigoApartamentoSolicitado = codigoApartamentoSolicitado;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public ConflictoCanal getConflictoCanal() {
        return conflictoCanal;
    }

    public void setConflictoCanal(ConflictoCanal conflictoCanal) {
        this.conflictoCanal = conflictoCanal;
    }
}
