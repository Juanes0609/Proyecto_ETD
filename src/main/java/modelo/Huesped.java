package modelo;

import java.time.LocalDate;
import java.util.List;

public class Huesped extends Persona {
    private LocalDate fechaNacimiento;
    private String correo;
    private String fotografia;
    private List<Reserva> listReservas;

    public Huesped(String nombre, String documento, String telefono, LocalDate fechaNacimiento, String correo, String fotografia) {
        super(nombre, documento, telefono);
        this.fechaNacimiento = fechaNacimiento;
        this.correo = correo;
        this.fotografia = fotografia;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getFotografia() {
        return fotografia;
    }

    public void setFotografia(String fotografia) {
        this.fotografia = fotografia;
    }

    public List<Reserva> getListReservas() {
        return listReservas;
    }

    public void setListReservas(List<Reserva> listReservas) {
        this.listReservas = listReservas;
    }
}
