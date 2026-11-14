package gui.integracion.dto;

/** Recepcionista del alojamiento. */
public record EmpleadoDTO(String documento, String nombre, String turno)
        implements java.io.Serializable, Comparable<EmpleadoDTO> {

    /** Orden natural por documento: es lo que permite guardarlos en el árbol binario de búsqueda. */
    @Override
    public int compareTo(EmpleadoDTO otro) {
        return documento.compareTo(otro.documento);
    }
}
