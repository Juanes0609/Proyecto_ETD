package gui.integracion.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

/** Orden de la cola de preparación (aseo) de un aparta-suite. */
public record OrdenPreparacionDTO(String codigoApartaSuite, String nombreApartaSuite, boolean entradaHoy,
                                  LocalTime horaLlegadaSiguiente, LocalDateTime ingresoCola) {
}
