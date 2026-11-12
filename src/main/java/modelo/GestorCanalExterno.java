package modelo;

import estructuras.Cola;
import estructuras.ListaDoblementeEnlazada;

public class GestorCanalExterno {
    private final Cola<Reserva> cola;
    private final ListaDoblementeEnlazada<Reserva> aceptadas;
    private final ListaDoblementeEnlazada<Reserva> conflictos;

    public GestorCanalExterno() {
        this.cola = new Cola<>();
        this.aceptadas = new ListaDoblementeEnlazada<>();
        this.conflictos = new ListaDoblementeEnlazada<>();
    }

    public void recibirSolicitud(Reserva reserva) {
        if (reserva == null || reserva.getApartamento() == null
                || reserva.getEstancia() == null) {
            throw new IllegalArgumentException(
                    "La reserva externa debe tener apartamento y estancia");
        }
        cola.encolar(reserva);
    }

    /**
     * Procesa UNA solicitud, la primera que llegó.
     * @return true si se aceptó, false si fue conflicto
     */
    public boolean procesarSiguiente() {
        Reserva reserva = cola.desencolar();
        Apartamento apartamento = reserva.getApartamento();

        // Aquí está la detección de conflicto: usa el ABB del apartamento
        boolean choca = apartamento.tieneSolapamiento(reserva.getEstancia());

        if (choca) {
            conflictos.agregarFinal(reserva);
            return false;
        }

        apartamento.registrarReserva(reserva);
        aceptadas.agregarFinal(reserva);
        return true;
    }

    /** Procesa toda la cola en orden. */
    public void procesarTodas() {
        while (!cola.estaVacia()) {
            procesarSiguiente();
        }
    }

    public boolean estaVacia() {
        return cola.estaVacia();
    }

    public int cantidadPendientes() {
        return cola.getTam();
    }

    public ListaDoblementeEnlazada<Reserva> getAceptadas() {
        return aceptadas;
    }

    public ListaDoblementeEnlazada<Reserva> getConflictos() {
        return conflictos;
    }
}
