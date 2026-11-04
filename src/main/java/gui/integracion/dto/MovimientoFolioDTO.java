package gui.integracion.dto;

import java.time.LocalDateTime;

/** Movimiento inalterable del folio: CARGO, PAGO, ANULACION_CARGO o ANULACION_PAGO. */
public record MovimientoFolioDTO(String tipo, String concepto, double monto, LocalDateTime fechaHora) {

    /** Efecto sobre el saldo pendiente: cargos suman y pagos restan. */
    public double efectoEnSaldo() {
        return switch (tipo) {
            case "CARGO", "ANULACION_PAGO" -> monto;
            default -> -monto;
        };
    }
}
