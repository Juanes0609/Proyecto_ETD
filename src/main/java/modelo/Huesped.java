package modelo;

import estructuras.Conjunto;
import estructuras.ListaDoblementeEnlazada;

import java.time.LocalDate;
import java.util.List;

public class Huesped extends Persona implements Comparable<Huesped> {
    private LocalDate fechaNacimiento;
    private String correo;
    private String fotografia;
    private final ListaDoblementeEnlazada<Reserva> listReservas;
    private final Conjunto<Apartamento> favoritos;

    public Huesped(String nombre, String documento, String telefono,
                   LocalDate fechaNacimiento, String correo, String fotografia) {
        super(nombre, documento, telefono);
        if (documento == null || documento.isBlank())
            throw new IllegalArgumentException("El documento es obligatorio");
        if (nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        this.fechaNacimiento = fechaNacimiento;
        this.correo = correo;
        this.fotografia = fotografia;
        this.listReservas = new ListaDoblementeEnlazada<>();
        this.favoritos = new Conjunto<>();
    }

    @Override
    public int compareTo(Huesped otro) {
        if (otro == null) throw new NullPointerException("El huésped a comparar no puede ser null");
        return getDocumento().compareToIgnoreCase(otro.getDocumento());
    }

    public void agregarReserva(Reserva reserva) {
        if (reserva != null && !listReservas.contiene(reserva)) listReservas.agregarFinal(reserva);
    }


    public boolean agregarFavorito(Apartamento apartamento) { return favoritos.agregar(apartamento); }
    public boolean quitarFavorito(Apartamento apartamento) { return favoritos.eliminar(apartamento); }
    public boolean esFavorito(Apartamento apartamento) { return favoritos.contiene(apartamento); }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getFotografia() { return fotografia; }
    public void setFotografia(String fotografia) { this.fotografia = fotografia; }
    public ListaDoblementeEnlazada<Reserva> getListReservas() { return listReservas; }
    public ListaDoblementeEnlazada<Reserva> getHistorialEstancias() { return listReservas; }
    public Conjunto<Apartamento> getFavoritos() { return favoritos; }
}