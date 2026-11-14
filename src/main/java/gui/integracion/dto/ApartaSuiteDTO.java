package gui.integracion.dto;

import java.util.List;

/**
 * Aparta-suite del complejo, tal como lo muestra la GUI.
 * Las tarifas son por persona y por noche, en pesos colombianos.
 */
public record ApartaSuiteDTO(
        String codigo,
        String nombre,
        String bloque,
        int piso,
        int dormitorios,
        int capacidad,
        double tarifaBaja,
        double tarifaMedia,
        double tarifaAlta,
        List<String> caracteristicas,
        String descripcion,
        String estadoOperativo,
        String rutaImagen,
        /** Por qué está fuera de servicio; vacío o {@code null} en cualquier otro estado. */
        String motivoFueraServicio) implements java.io.Serializable {

    public double tarifa(String tipoTemporada) {
        return switch (tipoTemporada) {
            case "ALTA" -> tarifaAlta;
            case "MEDIA" -> tarifaMedia;
            default -> tarifaBaja;
        };
    }

    public String ubicacion() {
        return "Bloque " + bloque + " · Piso " + piso;
    }

    public boolean reservable() {
        return !"FUERA_DE_SERVICIO".equals(estadoOperativo);
    }
}
