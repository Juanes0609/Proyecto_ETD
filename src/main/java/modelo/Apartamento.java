package modelo;

import gui.integracion.ConectorSGADemo;

import java.util.List;

public class Apartamento {
    private String codigo;
    private String nombre;
    private String bloque;
    private int piso;
    private int numeroDormitorios;
    private int capacidadMaxima;
    private String caracteristicas;
    private String motivoFueraServicio;
    private EstadoOperativo estadoOperativo;
    private NodoEdificio nodoEdificio;
    private Reserva reserva;
    private List<OrdenPreparacion> listOrdenesPreparacion;


    public Apartamento(String codigo, String nombre, String bloque, int piso, int numeroDormitorios, int capacidadMaxima, EstadoOperativo estadoOperativo, String caracteristicas, String motivoFueraServicio) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.bloque = bloque;
        this.piso = piso;
        this.numeroDormitorios = numeroDormitorios;
        this.capacidadMaxima = capacidadMaxima;
        this.estadoOperativo = estadoOperativo;
        this.caracteristicas = caracteristicas;
        this.motivoFueraServicio = motivoFueraServicio;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getBloque() {
        return bloque;
    }

    public void setBloque(String bloque) {
        this.bloque = bloque;
    }

    public int getPiso() {
        return piso;
    }

    public void setPiso(int piso) {
        this.piso = piso;
    }

    public int getNumeroDormitorios() {
        return numeroDormitorios;
    }

    public void setNumeroDormitorios(int numeroDormitorios) {
        this.numeroDormitorios = numeroDormitorios;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public void setCapacidadMaxima(int capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public String getCaracteristicas() {
        return caracteristicas;
    }

    public void setCaracteristicas(String caracteristicas) {
        this.caracteristicas = caracteristicas;
    }

    public String getMotivoFueraServicio() {
        return motivoFueraServicio;
    }

    public void setMotivoFueraServicio(String motivoFueraServicio) {
        this.motivoFueraServicio = motivoFueraServicio;
    }

    public EstadoOperativo getEstadoOperativo() {
        return estadoOperativo;
    }

    public void setEstadoOperativo(EstadoOperativo estadoOperativo) {
        this.estadoOperativo = estadoOperativo;
    }

    public NodoEdificio getNodoEdificio() {
        return nodoEdificio;
    }

    public void setNodoEdificio(NodoEdificio nodoEdificio) {
        this.nodoEdificio = nodoEdificio;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public List<OrdenPreparacion> getListOrdenesPreparacion() {
        return listOrdenesPreparacion;
    }

    public void setListOrdenesPreparacion(List<OrdenPreparacion> listOrdenesPreparacion) {
        this.listOrdenesPreparacion = listOrdenesPreparacion;
    }

    @Override
    public String toString() {
        return "Apartamento{" +
                "codigo='" + codigo + '\'' +
                ", nombre='" + nombre + '\'' +
                ", bloque='" + bloque + '\'' +
                ", piso=" + piso +
                ", numeroDormitorios=" + numeroDormitorios +
                ", capacidadMaxima=" + capacidadMaxima +
                ", caracteristicas='" + caracteristicas + '\'' +
                ", motivoFueraServicio='" + motivoFueraServicio + '\'' +
                '}';
    }
}
