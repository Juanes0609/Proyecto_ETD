package modelo;

import java.util.Comparator;

/** Órdenes alternativos para apartamentos (el natural es por código, en Apartamento.compareTo). */
public final class ComparadoresApartamento {
    private ComparadoresApartamento() {}

    /** Por tarifa por persona/noche de la temporada dada. */
    public static Comparator<Apartamento> porPrecio(Temporada t) {
        return Comparator.comparingDouble((Apartamento a) -> a.getTarifa(t)).thenComparing(Comparator.naturalOrder());
    }

    /** Por tarifa de temporada Baja (base). */
    public static final Comparator<Apartamento> POR_PRECIO = porPrecio(Temporada.BAJA);

    public static final Comparator<Apartamento> POR_CAPACIDAD =
            Comparator.comparingInt(Apartamento::getCapacidadMaxima).thenComparing(Comparator.naturalOrder());

    public static final Comparator<Apartamento> POR_DORMITORIOS =
            Comparator.comparingInt(Apartamento::getDormitorios).thenComparing(Comparator.naturalOrder());

    public static final Comparator<Apartamento> POR_NUMERO_RESERVAS =
            Comparator.comparingInt(Apartamento::getNumeroReservas).thenComparing(Comparator.naturalOrder());
}
