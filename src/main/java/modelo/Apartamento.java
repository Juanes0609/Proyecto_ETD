package modelo;

import estructuras.ArbolBinarioBusqueda;
import estructuras.ListaDoblementeEnlazada;
import gui.integracion.ConectorSGADemo;

import java.util.List;
import java.util.Objects;

public class Apartamento implements Comparable<Apartamento> {
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
    private double tarifaBaja;
    private double tarifaMedia;
    private double tarifaAlta;
    private final ListaDoblementeEnlazada<Reserva> reservas;
    private final ArbolBinarioBusqueda<Reserva> reservasPorFecha;


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
        this.tarifaBaja = 0;
        this.tarifaMedia = 0;
        this.tarifaAlta = 0;
        this.reservas = new ListaDoblementeEnlazada<>();
        this.reservasPorFecha = new ArbolBinarioBusqueda<>();
        this.nodoEdificio = new NodoEdificio(this.codigo, this.nombre, TipoNodoEdificio.APARTAMENTO);
        this.nodoEdificio.setApartamento(this);
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
    public int compareTo(Apartamento otro) {
        if (otro == null) throw new NullPointerException("El apartamento a comparar no puede ser null");
        return codigo.compareToIgnoreCase(otro.codigo);
    }

    public void actualizarTarifas(double baja, double media, double alta) {
        if (baja < 0 || media < 0 || alta < 0) {
            throw new IllegalArgumentException("Las tarifas no pueden ser negativas");
        }
        this.tarifaBaja = baja;
        this.tarifaMedia = media;
        this.tarifaAlta = alta;
    }

    public double getTarifaBaja() { return tarifaBaja; }
    public void setTarifaBaja(double tarifaBaja) {
        if (tarifaBaja < 0) throw new IllegalArgumentException("La tarifa no puede ser negativa");
        this.tarifaBaja = tarifaBaja;
    }
    public double getTarifaMedia() { return tarifaMedia; }
    public void setTarifaMedia(double tarifaMedia) {
        if (tarifaMedia < 0) throw new IllegalArgumentException("La tarifa no puede ser negativa");
        this.tarifaMedia = tarifaMedia;
    }
    public double getTarifaAlta() { return tarifaAlta; }
    public void setTarifaAlta(double tarifaAlta) {
        if (tarifaAlta < 0) throw new IllegalArgumentException("La tarifa no puede ser negativa");
        this.tarifaAlta = tarifaAlta;
    }

    /** Registra la reserva en el historial del apartamento. */
    public void registrarReserva(Reserva reserva) {
        if (reserva == null) return;
        if (!reservas.contiene(reserva)) reservas.agregarFinal(reserva);
        if (reserva.getEstadoReserva() != null && reserva.getEstadoReserva().esActiva()) {
            reservasPorFecha.insertar(reserva);
        }
    }

    public void retirarReservaActiva(Reserva reserva) {
        if (reserva != null) reservasPorFecha.eliminar(reserva);
    }

    public ListaDoblementeEnlazada<Reserva> getReservas() { return reservas; }
    public ArbolBinarioBusqueda<Reserva> getReservasPorFecha() { return reservasPorFecha; }
    public int getCantidadReservas() { return reservas.getTam(); }

    /** Detecta recursivamente, en el árbol por fecha, si se cruza alguna reserva activa. */
    public boolean tieneSolapamiento(Estancia estancia) {
        if (estancia == null) {
            throw new IllegalArgumentException("La estancia es obligatoria");
        }

        Reserva existente = reservasPorFecha.existeSi(reserva -> {
            if (reserva.getEstadoReserva() == null
                    || !reserva.getEstadoReserva().esActiva()) {
                return false;
            }

            Estancia otra = reserva.getEstancia();

            return otra != null
                    && estancia.getFechaEntrada().isBefore(otra.getFechaSalida())
                    && estancia.getFechaSalida().isAfter(otra.getFechaEntrada());
        });

        return existente != null;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof Apartamento)) return false;
        Apartamento otro = (Apartamento) objeto;
        return codigo != null && otro.codigo != null
                && codigo.equalsIgnoreCase(otro.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo == null ? null : codigo.toUpperCase());
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
