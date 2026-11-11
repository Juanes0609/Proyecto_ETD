package modelo;

import java.time.LocalDateTime;
import estructuras.ListaDoblementeEnlazada;

public class Reserva implements Comparable<Reserva> {
    private String codigo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime horaVencimiento;
    private EstadoReserva estadoReserva;
    private Canal canal;
    private Huesped titular;
    private ListaDoblementeEnlazada<Ocupante> listOcupantes;
    private SolicitudExterna  solicitudExterna;
    private Folio folio;
    private Apartamento apartamento;
    private Estancia estancia;


    public Reserva(String codigo, LocalDateTime fechaCreacion, LocalDateTime horaVencimiento, EstadoReserva estadoReserva, Canal canal, Apartamento apartamento, Huesped titular, Estancia estancia) {
        this.codigo = codigo;
        this.fechaCreacion = fechaCreacion;
        this.horaVencimiento = horaVencimiento;
        this.estadoReserva = estadoReserva;
        this.canal = canal;
        this.apartamento = apartamento;
        this.titular = titular;
        this.estancia = estancia;
        this.listOcupantes = new ListaDoblementeEnlazada<>();
        this.folio = new Folio(0, null);
    }

    // Orden natural: por fecha de entrada
    @Override
    public int compareTo (Reserva otra) {
        return  this.estancia.getFechaEntrada().compareTo(otra.estancia.getFechaEntrada());
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

    public ListaDoblementeEnlazada<Ocupante> getListOcupantes() {
        return listOcupantes;
    }

    public void setListOcupantes(ListaDoblementeEnlazada<Ocupante> listOcupantes) {
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
        return "RESERVA: " +
                ". Codigo: '" + codigo + '\'' +
                ". Fecha de creacion: " + fechaCreacion +
                ". Hora de vencimiento" + horaVencimiento;
    }
}
