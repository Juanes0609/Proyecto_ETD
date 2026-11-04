package gui.integracion.dto;

import java.time.LocalDate;

/** Una noche del desglose de la cotización. */
public record NocheDTO(LocalDate fecha, String temporada, int personasCobradas,
                       double tarifaUnitaria, double subtotal) {
}
