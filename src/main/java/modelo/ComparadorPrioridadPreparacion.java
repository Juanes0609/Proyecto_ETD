package modelo;

import java.util.Comparator;

public class ComparadorPrioridadPreparacion implements Comparator<OrdenPreparacion> {
    @Override
    public int compare(OrdenPreparacion o1, OrdenPreparacion o2) {
        int resultado = Integer.compare(
                o1.getPrioridad(),
                o2.getPrioridad()
        );

        if (resultado != 0) {
            return resultado;
        }

        return o1.getHoraLlegadaEstimada().compareTo(
                o2.getHoraLlegadaEstimada()
        );

    }
}
