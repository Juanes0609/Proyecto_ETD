package servicio;

import modelo.*;

import java.time.LocalDate;
import java.time.Period;

public class ServicioFacturacion {

    private static final int EDAD_MINIMA_PAGO = 6;
    private static final double PORCENTAJE_ANTICIPO = 0.30;
    private final Iterable<Temporada> temporadas;
    private final double tarifaBaja;
    private final double tarifaMedia;
    private final double tarifaAlta;


    public ServicioFacturacion(Iterable<Temporada> temporadas, double tarifaBaja, double tarifaMedia, double tarifaAlta) {
        this.temporadas = temporadas;
        this.tarifaBaja = tarifaBaja;
        this.tarifaMedia = tarifaMedia;
        this.tarifaAlta = tarifaAlta;
    }

    // La temporada base es BAJA: si ninguna temporada definida contiene la noche, es BAJA
    public TipoTemporada temporadaDe(LocalDate noche){
        for (Temporada t : temporadas){
            boolean dentro = !noche.isBefore(t.getFechaInicio()) && !noche.isAfter(t.getFechaFin());
            if (dentro) {
                return t.getTipoTemporada();
            }
        }
        return TipoTemporada.BAJA;
    }


    public double tarifaDe (TipoTemporada tipo)  {
        switch (tipo) {
            case MEDIA: return tarifaMedia;
            case ALTA: return tarifaAlta;
            default: return tarifaBaja;
        }
    }

    // Menores a 6 años no pagan pero ocupan cupo, se mide la edad en la noche cobrada
    public int personasQuePagan(Iterable<Ocupante> ocupantes, LocalDate noche) {
        int pagan = 0;
        for (Ocupante o : ocupantes) {
            int edad = Period.between(o.getFechaNacimiento(), noche).getYears();
        if (edad >= EDAD_MINIMA_PAGO) {
            pagan ++;

            }
        }
        return pagan;
    }

    public double calcularValorNoche (LocalDate noche, Iterable<Ocupante> ocupantes) {
        return personasQuePagan(ocupantes, noche) * tarifaDe(temporadaDe(noche));
    }

    // Liquidación recursiva: valor de la noche actual + valor de las noches restantes
    public double liquidar(Estancia estancia, Iterable<Ocupante> ocupantes){
        return liquidarDesde(estancia.getFechaEntrada(), estancia.getFechaSalida(), ocupantes);
    }

    private double liquidarDesde(LocalDate noche, LocalDate salida, Iterable<Ocupante> ocupantes) {
        if (!noche.isBefore(salida)) {
            return 0;  // caso base: ya no quedan noches
        }
        return calcularValorNoche(noche, ocupantes) + liquidarDesde(noche.plusDays(1), salida, ocupantes);
    }

    public double calcularAnticipo(double totalEstancia) {
        return totalEstancia * PORCENTAJE_ANTICIPO;
    }
}
