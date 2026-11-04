package gui.integracion.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Solicitud de reserva que llega por el canal externo (cola FIFO). */
public record SolicitudExternaDTO(String id, String nombreHuesped, String documentoHuesped,
                                  String codigoApartaSuite, LocalDate entrada, LocalDate salida,
                                  int personas, LocalDateTime recepcion) {
}
