package modelo;

import java.time.LocalDateTime;

public class OrdenPreparacion {
    private String codigo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime horaLlegadaEstimada;
    private int prioridad;
    private LocalDateTime horaInicioPreparacion;
    private LocalDateTime horaFinPreparacion;

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

    public LocalDateTime getHoraInicioPreparacion() {
        return horaInicioPreparacion;
    }

    public void setHoraInicioPreparacion(LocalDateTime horaInicioPreparacion) {
        this.horaInicioPreparacion = horaInicioPreparacion;
    }

    public LocalDateTime getHoraFinPreparacion() {
        return horaFinPreparacion;
    }

    public void setHoraFinPreparacion(LocalDateTime horaFinPreparacion) {
        this.horaFinPreparacion = horaFinPreparacion;
    }

    /** Minutos entre que se despachó la orden y quedó preparada; -1 si aún no termina. */
    public long minutosDePreparacion() {
        if (horaInicioPreparacion == null || horaFinPreparacion == null) {
            return -1;
        }
        return java.time.Duration.between(horaInicioPreparacion, horaFinPreparacion).toMinutes();
    }
}
