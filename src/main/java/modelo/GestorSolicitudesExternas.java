package modelo;

import estructuras.Cola;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class GestorSolicitudesExternas {
    private Cola<SolicitudExterna> solicitudes;

    public GestorSolicitudesExternas() {
        solicitudes = new Cola<>();
    }

    // Recibir una solicitud y agregarla al final
    public void recibirSolicitud(SolicitudExterna solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException(
                    "La solicitud no puede ser null"
            );
        }

        solicitudes.encolar(solicitud);
    }

    // Consultar la siguiente solicitud sin retirarla
    public SolicitudExterna consultarSiguiente() {
        if (solicitudes.estaVacia()) {
            return null;
        }

        return solicitudes.consultarFrente();
    }

    // Consultar cuántas solicitudes están pendientes
    public int getCantidadSolicitudes() {
        return solicitudes.getTam();
    }

    // Procesar la solicitud más antigua y revisar conflictos
    public ConflictoCanal procesarSiguiente(List<Reserva> reservas) {
        if (solicitudes.estaVacia()) {
            return null;
        }

        if (reservas == null) {
            throw new IllegalArgumentException(
                    "La lista de reservas no puede ser null"
            );
        }

        SolicitudExterna solicitud = solicitudes.consultarFrente();

        ConflictoCanal conflicto = buscarConflicto(
                solicitud, reservas
        );

        solicitudes.desencolar();

        if (conflicto != null) {
            solicitud.setConflictoCanal(conflicto);
        }

        return conflicto;
    }

    // Buscar una reserva activa que se solape con la solicitud
    private ConflictoCanal buscarConflicto(
            SolicitudExterna solicitud,
            List<Reserva> reservas) {

        for (Reserva reserva : reservas) {

            if (reserva == null) {
                continue;
            }

            // Solo revisar reservas del apartamento solicitado
            if (reserva.getApartamento() == null
                    || !solicitud.getCodigoApartamentoSolicitado()
                    .equals(reserva.getApartamento().getCodigo())) {
                continue;
            }

            // Ignorar reservas que no ocupan noches
            if (reserva.getEstadoReserva() == null
                    || !reserva.getEstadoReserva().esActiva()) {
                continue;
            }

            Estancia estancia = reserva.getEstancia();

            if (estancia == null
                    || estancia.getFechaEntrada() == null
                    || estancia.getFechaSalida() == null) {
                continue;
            }

            LocalDate entradaExistente = estancia.getFechaEntrada();
            LocalDate salidaExistente = estancia.getFechaSalida();

            boolean fechasSeSolapan =
                    solicitud.getFechaEntrada().isBefore(salidaExistente)
                            && solicitud.getFechaSalida().isAfter(entradaExistente);

            if (fechasSeSolapan) {
                ConflictoCanal nuevoConflicto = new ConflictoCanal(
                        UUID.randomUUID().toString(),
                        LocalDateTime.now(),
                        "El apartamento "
                                + solicitud.getCodigoApartamentoSolicitado()
                                + " ya tiene una reserva activa para fechas "
                                + "que se solapan con la solicitud.",
                        solicitud
                );

                return nuevoConflicto;
            }
        }

        return null;
    }
}
