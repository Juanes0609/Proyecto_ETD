package modelo;

import java.time.LocalDate;
import estructuras.ListaDoblementeEnlazada

public class Temporada {
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private TipoTemporada tipoTemporada;
    private ListaDoblementeEnlazada<DetalleNoche> listaDetalleNoche;

    public Temporada(LocalDate fechaInicio, LocalDate fechaFin, TipoTemporada tipoTemporada) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.tipoTemporada = tipoTemporada;
        listaDetalleNoche = new ListaDoblementeEnlazada<>();
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public TipoTemporada getTipoTemporada() {
        return tipoTemporada;
    }

    public void setTipoTemporada(TipoTemporada tipoTemporada) {
        this.tipoTemporada = tipoTemporada;
    }

    public ListaDoblementeEnlazada<DetalleNoche> getListaDetalleNoche() {
        return listaDetalleNoche;
    }

    public void setListaDetalleNoche(ListaDoblementeEnlazada<DetalleNoche> listaDetalleNoche) {
        this.listaDetalleNoche = listaDetalleNoche;
    }
}
