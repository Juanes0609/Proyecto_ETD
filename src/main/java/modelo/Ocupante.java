package modelo;

import java.time.LocalDate;

/**
 * Persona que se aloja en una reserva. La fecha de nacimiento decide si paga
 * (los menores de 6 años ocupan cupo pero no pagan); el documento es opcional
 * porque los menores pueden no tenerlo.
 */
public class Ocupante {
    private String nombre;
    private String documento;
    private LocalDate fechaNacimiento;

    public Ocupante(String nombre, LocalDate fechaNacimiento) {
        this(nombre, null, fechaNacimiento);
    }

    public Ocupante(String nombre, String documento, LocalDate fechaNacimiento) {
        this.nombre = nombre;
        this.documento = documento;
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }
}
