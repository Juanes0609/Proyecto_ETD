package modelo;

import java.time.LocalDateTime;

public class OrdenPreparacion {
    private String codigo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime horaLlegadaEstimada;
    private int prioridad;

    public OrdenPreparacion(String codigo, LocalDateTime fechaCreacion, LocalDateTime horaLlegadaEstimada, int prioridad) {
        this.codigo = codigo;
        this.fechaCreacion = fechaCreacion;
        this.horaLlegadaEstimada = horaLlegadaEstimada;
        this.prioridad = prioridad;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getHoraLlegadaEstimada() {
        return horaLlegadaEstimada;
    }

    public void setHoraLlegadaEstimada(LocalDateTime horaLlegadaEstimada) {
        this.horaLlegadaEstimada = horaLlegadaEstimada;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(int prioridad) {
        this.prioridad = prioridad;
    }
}
