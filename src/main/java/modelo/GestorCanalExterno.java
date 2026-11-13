package modelo;

import estructuras.Cola;
import estructuras.ListaDoblementeEnlazada;
import excepciones.FechasInvalidasException;

import java.time.LocalDateTime;

public class GestorCanalExterno {
    private final SistemaGestionAlojamiento sistema;
    private final Cola<SolicitudExterna> cola;
    private final ListaDoblementeEnlazada<Reserva> aceptadas;
    private final ListaDoblementeEnlazada<ConflictoCanal> conflictos;
    private int consecutivoReserva;
    private int consecutivoConflicto;

    public GestorCanalExterno(SistemaGestionAlojamiento sistema) {
        if (sistema == null) {
            throw new IllegalArgumentException(
                    "El sistema de gestión es obligatorio");
        }
        this.sistema = sistema;
        this.cola = new Cola<>();
        this.aceptadas = new ListaDoblementeEnlazada<>();
        this.conflictos = new ListaDoblementeEnlazada<>();
    }

    /** La plataforma externa entrega una solicitud: entra al final de la cola. */
    public void recibirSolicitud(SolicitudExterna solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException(
                    "La solicitud no puede ser null");
        }
        cola.encolar(solicitud);
    }

    /** Siguiente solicitud sin retirarla, o null si no hay. */
    public SolicitudExterna consultarSiguiente() {
        return cola.estaVacia() ? null : cola.consultarFrente();
    }

    /**
     * Procesa UNA solicitud: la primera que llegó.
     *
     * @return null si la solicitud fue aceptada; el {@link ConflictoCanal}
     *         registrado si fue rechazada.
     * @throws excepciones.EstructuraVaciaException si no hay solicitudes.
     */
    public ConflictoCanal procesarSiguiente() {
        SolicitudExterna solicitud = cola.desencolar();

        Apartamento apartamento = sistema.buscarApartamento(
                solicitud.getCodigoApartamentoSolicitado());

        if (apartamento == null) {
            return registrarConflicto(solicitud,
                    "El apartamento " + solicitud.getCodigoApartamentoSolicitado()
                            + " no existe");
        }

        if (solicitud.getCantidadPersonas() > apartamento.getCapacidadMaxima()) {
            return registrarConflicto(solicitud,
                    "El apartamento " + apartamento.getCodigo() + " admite máximo "
                            + apartamento.getCapacidadMaxima() + " personas");
        }

        Estancia estancia;
        try {
            estancia = new Estancia(solicitud.getFechaEntrada(),
                    solicitud.getFechaSalida(), null);
        } catch (FechasInvalidasException e) {
            return registrarConflicto(solicitud, e.getMessage());
        }

        if (apartamento.tieneSolapamiento(estancia)) {
            return registrarConflicto(solicitud,
                    "El apartamento " + apartamento.getCodigo()
                            + " ya tiene una reserva activa que comparte alguna "
                            + "noche con " + solicitud.getFechaEntrada()
                            + " a " + solicitud.getFechaSalida());
        }

        consecutivoReserva++;
        Reserva reserva = new Reserva(
                "EXT-" + consecutivoReserva,
                LocalDateTime.now(),
                null,
                EstadoReserva.CONFIRMADA,
                Canal.EXTERNA,
                apartamento,
                null,           // la plataforma externa no envía un perfil de huésped
                estancia);
        estancia.setReserva(reserva);
        reserva.setSolicitudExterna(solicitud);
        solicitud.setReserva(reserva);

        apartamento.registrarReserva(reserva);
        aceptadas.agregarFinal(reserva);
        return null;
    }

    /** Procesa toda la cola en orden de llegada. */
    public void procesarTodas() {
        while (!cola.estaVacia()) {
            procesarSiguiente();
        }
    }

    private ConflictoCanal registrarConflicto(SolicitudExterna solicitud, String motivo) {
        consecutivoConflicto++;
        ConflictoCanal conflicto = new ConflictoCanal(
                "CON-" + consecutivoConflicto, LocalDateTime.now(), motivo, solicitud);
        solicitud.setConflictoCanal(conflicto);
        conflictos.agregarFinal(conflicto);
        return conflicto;
    }

    public boolean estaVacia() {
        return cola.estaVacia();
    }

    public int cantidadPendientes() {
        return cola.getTam();
    }

    /** Solicitudes en espera, en orden de llegada, sin modificar la cola. */
    public ListaDoblementeEnlazada<SolicitudExterna> listarPendientes() {
        ListaDoblementeEnlazada<SolicitudExterna> lista =
                new ListaDoblementeEnlazada<>();
        for (SolicitudExterna s : cola) {
            lista.agregarFinal(s);
        }
        return lista;
    }

    public ListaDoblementeEnlazada<Reserva> getAceptadas() {
        return aceptadas;
    }

    public ListaDoblementeEnlazada<ConflictoCanal> getConflictos() {
        return conflictos;
    }
}
