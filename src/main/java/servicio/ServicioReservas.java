package servicio;

import excepciones.ReservaSolapadaException;
import excepciones.TransicionEstadoInvalidaException;
import excepciones.CapacidadExcedidaException;
import excepciones.FechasInvalidasException;
import estructuras.ColaPrioridad;
import estructuras.ListaDoblementeEnlazada;
import modelo.EstadoReserva;
import modelo.Reserva;
import modelo.Apartamento;
import modelo.Canal;
import modelo.Estancia;
import modelo.Huesped;
import modelo.Ocupante;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;

public class ServicioReservas {
    private static final int HORAS_VENCIMIENTO = 24;
    private final ServicioFacturacion facturacion;
    private int consecutivo;
    // reservas pendientes del portal, ordenadas por hora de vencimiento
    private final ColaPrioridad<Reserva> vencimientos = new ColaPrioridad<>(Comparator.comparing(Reserva::getHoraVencimiento));
    // todas las reservas creadas, para poder buscarlas y listarlas
    private final ListaDoblementeEnlazada<Reserva> reservas = new ListaDoblementeEnlazada<>();


    public ServicioReservas(ServicioFacturacion facturacion) {
        this.facturacion = facturacion;
    }

    public Reserva crearReserva(Canal canal, Apartamento apartamento, Huesped titular,
                                LocalDate entrada, LocalDate salida,
                                Iterable<Ocupante> ocupantes, LocalDateTime ahora) {

        // Validaciones básicas antes de consultar las entidades.
        if (canal == null || apartamento == null || titular == null || ahora == null) {
            throw new IllegalArgumentException("Canal, apartamento, titular y fecha actual son obligatorios");
        }

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

        // 3. Evitar doble reserva del mismo apartamento en noches compartidas.
        if (apartamento.tieneSolapamiento(estancia)) {
            throw new ReservaSolapadaException("El apartamento " + apartamento.getCodigo()
                    + " ya tiene una reserva activa en ese rango de fechas");
        }

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
            if (o == null) throw new IllegalArgumentException("La lista de ocupantes no puede contener null");
            reserva.getListOcupantes().agregarFinal(o);
        }


        // 6. Cargo inicial en el folio: el valor total de la estancia
        double total = facturacion.paraApartamento(apartamento).liquidar(estancia, reserva.getListOcupantes());
        if (total > 0) {
            reserva.getFolio().registrarCargo("Estancia " + entrada + " a " + salida, total, false);
        }
        // 7. Registrar al final, cuando ya nada puede fallar
        apartamento.registrarReserva(reserva);
        titular.agregarReserva(reserva);
        reservas.agregarFinal(reserva);
        if (canal == Canal.PORTAL) {
            vencimientos.encolar(reserva);
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
        double total = facturacion.paraApartamento(reserva.getApartamento())
                .liquidar(reserva.getEstancia(), reserva.getListOcupantes());
        double anticipo = facturacion.calcularAnticipo(total);
        if (anticipo > 0) {
            reserva.getFolio().registrarPago("Anticipo 30%", anticipo, false);
        }
        confirmar(reserva);
    }

    // cancela las PENDIENTES cuyo plazo de 24 h ya pasó. Devuelve cuántas canceló.
    public int procesarVencimientos(LocalDateTime ahora) {
        int canceladas = 0;
        while (!vencimientos.estaVacia()) {
            Reserva r = vencimientos.consultarPrioridad();
            if (r.getHoraVencimiento().isAfter(ahora)) {
                break;   // la cola está ordenada: las demás vencen después
            }
            vencimientos.desencolar();
            if (r.getEstadoReserva() == EstadoReserva.PENDIENTE) {   // si ya pagó o canceló, se ignora
                cancelar(r);   // cancelar ya libera las noches
                canceladas++;
            }
        }
        return canceladas;
    }

    // NUEVO: buscar una reserva por su código (null si no existe)
    public Reserva buscarReserva(String codigo) {
        for (Reserva r : reservas) {
            if (r.getCodigo().equalsIgnoreCase(codigo)) {
                return r;
            }
        }
        return null;
    }

    // NUEVO: todas las reservas creadas por este servicio
    public ListaDoblementeEnlazada<Reserva> listarReservas() {
        return reservas;
    }

    // liberar noches significa sacar la reserva del árbol de reservas activas del apartamento
    private void liberarNoches(Reserva reserva) {
        if (reserva.getApartamento() != null) {
            reserva.getApartamento().retirarReservaActiva(reserva);
        }
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
        liberarNoches(reserva);
    }

    public void iniciarEstancia(Reserva reserva) {
        cambiarEstado(reserva, EstadoReserva.ENCURSO);
    }

    public void finalizar(Reserva reserva) {
        cambiarEstado(reserva, EstadoReserva.FINALIZADA);
        liberarNoches(reserva);   // una reserva terminada no debe ocupar su fecha en el árbol
    }

    public void marcarNoShow(Reserva reserva) {
        cambiarEstado(reserva, EstadoReserva.NOSHOW);
        liberarNoches(reserva);
    }
}
