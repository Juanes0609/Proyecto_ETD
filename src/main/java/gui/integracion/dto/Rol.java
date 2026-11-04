package gui.integracion.dto;

/** Roles que pueden iniciar sesión en el SGA. */
public enum Rol {
    HUESPED("Huésped"),
    RECEPCIONISTA("Recepción"),
    ADMINISTRADOR("Administración");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
