package servicio;

import estructuras.ListaDoblementeEnlazada;
import modelo.*;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;


public class ServicioFacturacion {

    private static final int EDAD_MINIMA_PAGO = 6;
    private static final double PORCENTAJE_ANTICIPO = 0.30;
    private static final int DIAS_DEVOLUCION_TOTAL = 7;
    private static final int DIAS_DEVOLUCION_PARCIAL = 2;
    private static final double PORCENTAJE_DEVOLUCION_PARCIAL = 0.50;
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

    // devuelve un servicio con las tarifas propias de ese apartamento
    public ServicioFacturacion paraApartamento(Apartamento apartamento) {
        return new ServicioFacturacion(temporadas,
                apartamento.getTarifaBaja(),
                apartamento.getTarifaMedia(),
                apartamento.getTarifaAlta());
    }

    // búsqueda de la temporada que cubre la noche (null si ninguna)
    private Temporada buscarTemporada(LocalDate noche) {
        for (Temporada t : temporadas) {
            if (!noche.isBefore(t.getFechaInicio()) && !noche.isAfter(t.getFechaFin())) {
                return t;
            }
        }
        return null;   // ninguna la cubre: es BAJA (temporada base)
    }


    // La temporada base es BAJA: si ninguna temporada definida contiene la noche, es BAJA
    public TipoTemporada temporadaDe(LocalDate noche) {
        Temporada t = buscarTemporada(noche);
        return (t == null) ? TipoTemporada.BAJA : t.getTipoTemporada();
    }

    public double tarifaDe(TipoTemporada tipo) {
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

    // desglose noche por noche para la cotización
    public ListaDoblementeEnlazada<DetalleNoche> desglosar(Estancia estancia, Iterable<Ocupante> ocupantes) {
        ListaDoblementeEnlazada<DetalleNoche> detalle = new ListaDoblementeEnlazada<>();
        for (LocalDate noche : estancia) {
            int pagan = personasQuePagan(ocupantes, noche);
            TipoTemporada tipo = temporadaDe(noche);                 // CAMBIO: ahora se guarda el tipo
            double tarifa = tarifaDe(tipo);
            detalle.agregarFinal(new DetalleNoche(noche, pagan, tarifa, pagan * tarifa, tipo));   // CAMBIO: tipo en vez de buscarTemporada(noche)
        }
        return detalle;
    }

    // política de devolución al cancelar (100 % / 50 % / 0 %)
    public double porcentajeDevolucion(LocalDate hoy, LocalDate fechaEntrada) {
        long diasFaltantes = ChronoUnit.DAYS.between(hoy, fechaEntrada);
        if (diasFaltantes >= DIAS_DEVOLUCION_TOTAL) {
            return 1.0;
        }
        if (diasFaltantes >= DIAS_DEVOLUCION_PARCIAL) {
            return PORCENTAJE_DEVOLUCION_PARCIAL;
        }
        return 0.0;
    }

    //
    public double calcularDevolucion(double valorPagado, LocalDate hoy, LocalDate fechaEntrada) {
        return valorPagado * porcentajeDevolucion(hoy, fechaEntrada);
    }

}
