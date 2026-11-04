package gui.integracion.dto;

import java.time.LocalDate;
import java.time.Period;

/** Persona que se aloja en la reserva (titular o acompañante). */
public record OcupanteDTO(String nombre, String documento, LocalDate fechaNacimiento) {

    /** Los menores de 6 años ocupan cupo pero no pagan. */
    public boolean paga(LocalDate fechaReferencia) {
        return fechaNacimiento == null || Period.between(fechaNacimiento, fechaReferencia).getYears() >= 6;
    }
}
