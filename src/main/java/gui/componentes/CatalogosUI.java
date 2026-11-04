package gui.componentes;

import java.util.List;

/**
 * Valores que muestran los filtros y combos de la GUI mientras los enums de
 * {@code modelo} están vacíos. Siguen los nombres del plan del proyecto.
 * <p>
 * Al implementarse el modelo, cada lista se reemplaza por los
 * {@code values()} del enum correspondiente desde {@code ConectorSGA}.
 */
public final class CatalogosUI {

    /** {@code modelo.EstadoOperativo}. */
    public static final List<String> ESTADOS_OPERATIVOS = List.of(
            "PREPARADO", "OCUPADO", "PENDIENTE_PREPARACION", "EN_PREPARACION", "FUERA_DE_SERVICIO");

    /** {@code modelo.EstadoReserva}. */
    public static final List<String> ESTADOS_RESERVA = List.of(
            "PENDIENTE", "CONFIRMADA", "EN_CURSO", "FINALIZADA", "CANCELADA", "NO_SHOW");

    /** {@code modelo.Canal} (llamado {@code CanalReserva} en el plan). */
    public static final List<String> CANALES_RESERVA = List.of("PORTAL", "DIRECTA", "EXTERNA");

    /** Comparadores de {@code comparadores/} para ordenar apartamentos disponibles. */
    public static final List<String> CRITERIOS_ORDEN_APARTAMENTOS = List.of(
            "Precio", "Capacidad", "Dormitorios", "Número de reservas");

    private CatalogosUI() {
    }
}
