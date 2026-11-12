package modelo;

import java.util.Comparator;

/** Ordena por tarifa media de las tres temporadas, de menor a mayor. */
public class ComparadorApartamentoPorPrecio implements Comparator<Apartamento> {
    private double tarifaMedia(Apartamento a) {
        return (a.getTarifaBaja() + a.getTarifaMedia() + a.getTarifaAlta()) / 3.0;
    }

    @Override
    public int compare(Apartamento a, Apartamento b) {
        int resultado = Double.compare(tarifaMedia(a), tarifaMedia(b));
        return resultado != 0 ? resultado : a.compareTo(b);
    }
}

