package modelo;

import java.util.Comparator;

/** Ordena por capacidad máxima, de mayor a menor. */
public class ComparadorApartamentoPorCapacidad implements Comparator<Apartamento> {
    @Override
    public int compare(Apartamento a, Apartamento b) {
        int resultado = Integer.compare(b.getCapacidadMaxima(), a.getCapacidadMaxima());
        return resultado != 0 ? resultado : a.compareTo(b);
    }
}