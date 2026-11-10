package modelo;

public class Recepcionista extends Persona{
    private String codigoEmpleado;

    public Recepcionista(String nombre, String documento, String telefono,String codigoEmpleado) {
        super(nombre, documento, telefono);
        this.codigoEmpleado = codigoEmpleado;
    }
}
