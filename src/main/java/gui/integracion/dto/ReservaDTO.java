package gui.integracion.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/** Reserva con su estado y resumen financiero. */
public record ReservaDTO(
        String codigo,
        String documentoTitular,
        String nombreTitular,
        String codigoApartaSuite,
        String nombreApartaSuite,
        LocalDate entrada,
        LocalDate salida,
        int personas,
        String canal,
        String estado,
        LocalTime horaLlegada,
        double total,
        double anticipoRequerido,
        double pagado,
        double saldo) {

    public long noches() {
        return ChronoUnit.DAYS.between(entrada, salida);
    }

    public boolean activa() {
        return estado.equals("PENDIENTE") || estado.equals("CONFIRMADA") || estado.equals("EN_CURSO");
    }
}
