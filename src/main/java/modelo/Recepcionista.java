package modelo;

/**
 * Empleado de recepción. Orden natural por documento, para guardarlo en el
 * árbol binario de búsqueda de empleados y rotarlo en la lista circular de turnos.
 */
public class Recepcionista extends Persona implements Comparable<Recepcionista> {
    private String codigoEmpleado;
    /** Franja del turno, por ejemplo "06:00 – 14:00". */
    private String horario;

    public Recepcionista(String nombre, String documento, String telefono, String codigoEmpleado) {
        this(nombre, documento, telefono, codigoEmpleado, null);
    }

    public Recepcionista(String nombre, String documento, String telefono, String codigoEmpleado, String horario) {
        super(nombre, documento, telefono);
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException("El documento del empleado es obligatorio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del empleado es obligatorio");
        }
        this.codigoEmpleado = codigoEmpleado;
        this.horario = horario;
    }

    public String getCodigoEmpleado() {
        return codigoEmpleado;
    }

    public void setCodigoEmpleado(String codigoEmpleado) {
        this.codigoEmpleado = codigoEmpleado;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    @Override
    public int compareTo(Recepcionista otro) {
        if (otro == null) {
            throw new NullPointerException("El recepcionista a comparar no puede ser null");
        }
        return getDocumento().compareToIgnoreCase(otro.getDocumento());
    }

    @Override
    public boolean equals(Object objeto) {
        return objeto instanceof Recepcionista otro && getDocumento().equalsIgnoreCase(otro.getDocumento());
    }

    @Override
    public int hashCode() {
        return getDocumento().toUpperCase().hashCode();
    }

    @Override
    public String toString() {
        return getNombre() + " (" + getDocumento() + ")";
    }
}
