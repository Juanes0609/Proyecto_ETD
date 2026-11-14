package gui.integracion.dto;

import java.time.LocalDate;
import java.util.Map;

/**
 * Estadísticas generadas recorriendo las estructuras del sistema.
 * Las noches ocupadas y los ingresos corresponden al periodo que va de
 * {@code desde} (incluido) a {@code hasta} (excluido); el resto es el estado actual.
 */
public record ReporteDTO(
        LocalDate desde,
        LocalDate hasta,
        int totalApartaSuites,
        int ocupados,
        double porcentajeOcupacion,
        double ingresos,
        Map<String, Integer> reservasPorCanal,
        /** Todas las reservas por estado, incluidas canceladas y no-show. */
        Map<String, Integer> reservasPorEstado,
        Map<String, Integer> apartaSuitesPorEstado,
        Map<String, Integer> nochesPorApartaSuite,
        Map<String, Integer> nochesPorTemporada,
        int conflictosCanal,
        double minutosPromedioPreparacion) {
}
