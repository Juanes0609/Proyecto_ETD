package modelo;

public enum EstadoApartamento {
    PREPARADO("Preparado"),
    OCUPADO("Ocupado"),
    PENDIENTE_PREPARACION("Pendiente de preparación"),
    EN_PREPARACION("En preparación"),
    FUERA_DE_SERVICIO("Fuera de servicio");

    private final String etiqueta;
    EstadoApartamento(String etiqueta) { this.etiqueta = etiqueta; }
    @Override public String toString() { return etiqueta; }
}