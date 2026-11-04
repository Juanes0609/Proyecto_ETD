package gui.integracion.dto;

import java.util.Map;

/** Estadísticas generadas recorriendo las estructuras del sistema. */
public record ReporteDTO(
        int totalApartaSuites,
        int ocupados,
        double porcentajeOcupacion,
        double ingresos,
        Map<String, Integer> reservasPorCanal,
        Map<String, Integer> apartaSuitesPorEstado,
        int conflictosCanal,
        double minutosPromedioPreparacion) {
}
