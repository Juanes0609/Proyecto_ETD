package gui.integracion.dto;

import java.time.LocalDate;

/** Rango de fechas con su tipo de temporada (BAJA, MEDIA o ALTA). */
public record TemporadaDTO(String tipo, LocalDate inicio, LocalDate fin) {

    public boolean incluye(LocalDate fecha) {
        return !fecha.isBefore(inicio) && !fecha.isAfter(fin);
    }
}
