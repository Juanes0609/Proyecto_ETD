package servicio;

import excepciones.TransicionEstadoInvalidaException;
import excepciones.CapacidadExcedidaException;
import excepciones.FechasInvalidasException;
import modelo.EstadoReserva;
import modelo.Reserva;
import modelo.Apartamento;
import modelo.Canal;
import modelo.Estancia;
import modelo.Huesped;
import modelo.Ocupante;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ServicioReservas {
    private static final int HORAS_VENCIMIENTO = 24;

    private final ServicioFacturacion facturacion;
    private int consecutivo;

    public ServicioReservas(ServicioFacturacion facturacion) {
        this.facturacion = facturacion;
    }

    public Reserva crearReserva(Canal canal, Apartamento apartamento, Huesped titular,
                                LocalDate entrada, LocalDate salida,
                                Iterable<Ocupante> ocupantes, LocalDateTime ahora) {

        // 1. Fechas: el constructor de Estancia valida que la salida sea posterior a la entrada
        Estancia estancia = new Estancia(entrada, salida, null);
        if (entrada.isBefore(ahora.toLocalDate())) {
            throw new FechasInvalidasException("La fecha de entrada no puede estar en el pasado");
        }

        // 2. Capacidad: los menores de 6 años también ocupan cupo
        int cantidad = 0;
        for (Ocupante o : ocupantes) {
            cantidad++;
        }
        if (cantidad == 0) {
            throw new IllegalArgumentException("La reserva necesita al menos un ocupante");
        }
        if (cantidad > apartamento.getCapacidadMaxima()) {
            throw new CapacidadExcedidaException("El apartamento admite máximo "
                    + apartamento.getCapacidadMaxima() + " personas y se pidieron " + cantidad);
        }

        // 3. Solapamiento: PENDIENTE, se conecta cuando exista el árbol de reservas del apartamento

        // 4. Estado inicial y vencimiento según el canal
        EstadoReserva estadoInicial;
        LocalDateTime vencimiento;
        if (canal == Canal.PORTAL) {
            estadoInicial = EstadoReserva.PENDIENTE;
            vencimiento = ahora.plusHours(HORAS_VENCIMIENTO);
        } else {
            estadoInicial = EstadoReserva.CONFIRMADA;
            vencimiento = null;
        }

        // 5. Armar la reserva
        consecutivo++;
        Reserva reserva = new Reserva("RES-" + consecutivo, ahora, vencimiento,
                estadoInicial, canal, apartamento, titular, estancia);
        estancia.setReserva(reserva);
        for (Ocupante o : ocupantes) {
            reserva.getListOcupantes().agregarFinal(o);
        }

        // 6. Cargo inicial en el folio: el valor total de la estancia
        double total = facturacion.liquidar(estancia, reserva.getListOcupantes());
        if (total > 0) {
            reserva.getFolio().registrarCargo("Estancia " + entrada + " a " + salida, total);
        }
        return reserva;
    }

    // Solo para reservas PENDIENTES del portal: paga el 30 % y la confirma
    public void pagarAnticipo(Reserva reserva) {
        if (reserva.getCanal() != Canal.PORTAL
                || reserva.getEstadoReserva() != EstadoReserva.PENDIENTE) {
            throw new TransicionEstadoInvalidaException(
                    "El anticipo solo aplica a reservas PENDIENTES del portal");
        }
        double total = facturacion.liquidar(reserva.getEstancia(), reserva.getListOcupantes());
        double anticipo = facturacion.calcularAnticipo(total);
        if (anticipo > 0) {
            reserva.getFolio().registrarPago("Anticipo 30%", anticipo);
        }
        confirmar(reserva);
    }

    public void cambiarEstado(Reserva reserva, EstadoReserva destino) {
        EstadoReserva actual = reserva.getEstadoReserva();
        if (!actual.puedeTransicionarA(destino)) {
            throw new TransicionEstadoInvalidaException(
                    "No se puede pasar de " + actual + " a " + destino);
        }
        reserva.setEstadoReserva(destino);
    }

    public void confirmar(Reserva reserva) {
        cambiarEstado(reserva, EstadoReserva.CONFIRMADA);
    }

    public void cancelar(Reserva reserva) {
        cambiarEstado(reserva, EstadoReserva.CANCELADA);
    }

    public void iniciarEstancia(Reserva reserva) {
        cambiarEstado(reserva, EstadoReserva.ENCURSO);
    }

    public void finalizar(Reserva reserva) {
        cambiarEstado(reserva, EstadoReserva.FINALIZADA);
    }

    public void marcarNoShow(Reserva reserva) {
        cambiarEstado(reserva, EstadoReserva.NOSHOW);
    }
}
