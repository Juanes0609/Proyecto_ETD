package modelo;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import excepciones.FechasInvalidasException;

public class Estancia implements Iterable<LocalDate> {
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private Reserva reserva;
    private List<DetalleNoche> listDetalleNoche;

    public Estancia(LocalDate fechaEntrada, LocalDate fechaSalida, Reserva reserva) {
        if (fechaEntrada == null || fechaSalida == null) {
            throw new FechasInvalidasException("Las fechas de entrada y salida son obligatorias");
        }
        if (!fechaSalida.isAfter(fechaEntrada)) {
            throw new FechasInvalidasException("La fecha de salida debe ser posterior a la de entrada");
        }
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.reserva = reserva;
    }

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }

    public void setFechaEntrada(LocalDate fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public List<DetalleNoche> getListDetalleNoche() {
        return listDetalleNoche;
    }

    public void setListDetalleNoche(List<DetalleNoche> listDetalleNoche) {
        this.listDetalleNoche = listDetalleNoche;
    }

    /** Cantidad de noches a cobrar: la noche de salida no cuenta. */
    public long cantidadNoches() {
        return java.time.temporal.ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);
    }

    /** Permite {@code for (LocalDate noche : estancia)}: recorre las noches a cobrar. */
    @Override
    public Iterator<LocalDate> iterator() {
        return new Iterator<LocalDate>() {
            private LocalDate siguiente = fechaEntrada;

            @Override
            public boolean hasNext() {
                return siguiente.isBefore(fechaSalida);
            }

            @Override
            public LocalDate next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                LocalDate noche = siguiente;
                siguiente = siguiente.plusDays(1);
                return noche;
            }
        };
    }
}