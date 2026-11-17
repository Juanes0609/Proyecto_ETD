package modelo;

import estructuras.Cola;
import estructuras.ListaDoblementeEnlazada;
import excepciones.CapacidadExcedidaException;
import excepciones.FechasInvalidasException;
import excepciones.ReservaSolapadaException;

import java.time.LocalDateTime;

public class GestorCanalExterno {
    private final SistemaGestionAlojamiento sistema;
    private final Cola<SolicitudExterna> cola;
    private final ListaDoblementeEnlazada<Reserva> aceptadas;
    private final ListaDoblementeEnlazada<ConflictoCanal> conflictos;
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
     * Procesa UNA solicitud: la primera que llegó. La reserva se crea con
     * {@code ServicioReservas} (las mismas reglas que los demás canales: fechas,
     * capacidad, solapamiento, cargo inicial en el folio). Si alguna regla falla,
     * la solicitud se rechaza y queda registrada como conflicto de canal.
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
                    "El apartamento " + solicitud.getCodigoApartamentoSolicitado() + " no existe");
        }
        if (apartamento.getEstadoOperativo() == EstadoOperativo.FUERASERVICIO) {
            return registrarConflicto(solicitud,
                    "El apartamento " + apartamento.getCodigo() + " está fuera de servicio");
        }
        if (solicitud.getCantidadPersonas() <= 0) {
            return registrarConflicto(solicitud, "La solicitud no indica cuántas personas llegan");
        }

        Huesped titular = titularDe(solicitud);
        ListaDoblementeEnlazada<Ocupante> ocupantes = new ListaDoblementeEnlazada<>();
        // La plataforma no envía fechas de nacimiento: sin fecha se cobra como adulto.
        ocupantes.agregarFinal(new Ocupante(titular.getNombre(), titular.getDocumento(),
                titular.getFechaNacimiento()));
        for (int i = 2; i <= solicitud.getCantidadPersonas(); i++) {
            ocupantes.agregarFinal(new Ocupante("Acompañante " + i, null));
        }

        try {
            Reserva reserva = sistema.getReservas().crearReserva(Canal.EXTERNA, apartamento, titular,
                    solicitud.getFechaEntrada(), solicitud.getFechaSalida(), ocupantes, LocalDateTime.now());
            reserva.setSolicitudExterna(solicitud);
            solicitud.setReserva(reserva);
            aceptadas.agregarFinal(reserva);
            return null;
        } catch (ReservaSolapadaException | CapacidadExcedidaException | FechasInvalidasException e) {
            return registrarConflicto(solicitud, e.getMessage());
        }
    }

    /** El titular es el huésped con ese documento; si no existe, se registra con los datos de la solicitud. */
    private Huesped titularDe(SolicitudExterna solicitud) {
        String documento = solicitud.getDocumentoHuesped() == null || solicitud.getDocumentoHuesped().isBlank()
                ? "EXT-" + solicitud.getCodigo() : solicitud.getDocumentoHuesped();
        Huesped existente = sistema.buscarHuesped(documento);
        if (existente != null) {
            return existente;
        }
        String nombre = solicitud.getNombreHuesped() == null || solicitud.getNombreHuesped().isBlank()
                ? "Huésped de la plataforma externa" : solicitud.getNombreHuesped();
        Huesped nuevo = new Huesped(nombre, documento, null, null, null, null);
        sistema.registrarHuesped(nuevo);
        return nuevo;
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
