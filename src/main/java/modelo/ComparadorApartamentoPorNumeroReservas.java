package modelo;

import java.util.Comparator;

/** Ordena primero los apartamentos con más reservas registradas. */
public class ComparadorApartamentoPorNumeroReservas implements Comparator<Apartamento> {
    @Override
    public int compare(Apartamento a, Apartamento b) {
        int resultado = Integer.compare(b.getCantidadReservas(), a.getCantidadReservas());
        return resultado != 0 ? resultado : a.compareTo(b);
    }
}
