package modelo;

import java.time.LocalDateTime;
import java.util.List;

public class Reserva {
    private String codigo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime horaVencimiento;
    private EstadoReserva estadoReserva;
    private Canal canal;
    private List<Ocupante> listOcupantes;
    private SolicitudExterna  solicitudExterna;
    private Folio folio;
    private Apartamento apartamento;
    private Estancia estancia;


    public Reserva(String codigo, LocalDateTime fechaCreacion, LocalDateTime horaVencimiento, EstadoReserva estadoReserva, Canal canal, Apartamento apartamento) {
        this.codigo = codigo;
        this.fechaCreacion = fechaCreacion;
        this.horaVencimiento = horaVencimiento;
        this.estadoReserva = estadoReserva;
        this.canal = canal;
        this.apartamento = apartamento;
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

    public LocalDateTime getHoraVencimiento() {
        return horaVencimiento;
    }

    public void setHoraVencimiento(LocalDateTime horaVencimiento) {
        this.horaVencimiento = horaVencimiento;
    }

    public EstadoReserva getEstadoReserva() {
        return estadoReserva;
    }

    public void setEstadoReserva(EstadoReserva estadoReserva) {
        this.estadoReserva = estadoReserva;
    }

    public Canal getCanal() {
        return canal;
    }

    public void setCanal(Canal canal) {
        this.canal = canal;
    }

    public List<Ocupante> getListOcupantes() {
        return listOcupantes;
    }

    public void setListOcupantes(List<Ocupante> listOcupantes) {
        this.listOcupantes = listOcupantes;
    }

    public SolicitudExterna getSolicitudExterna() {
        return solicitudExterna;
    }

    public void setSolicitudExterna(SolicitudExterna solicitudExterna) {
        this.solicitudExterna = solicitudExterna;
    }

    public Folio getFolio() {
        return folio;
    }

    public void setFolio(Folio folio) {
        this.folio = folio;
    }

    public Apartamento getApartamento() {
        return apartamento;
    }

    public void setApartamento(Apartamento apartamento) {
        this.apartamento = apartamento;
    }

    public Estancia getEstancia() {
        return estancia;
    }

    public void setEstancia(Estancia estancia) {
        this.estancia = estancia;
    }

    @Override
    public String toString() {
        return "Reserva{" +
                "codigo='" + codigo + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                ", horaVencimiento=" + horaVencimiento +
                '}';
    }
}
