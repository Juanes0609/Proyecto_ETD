package modelo;

public enum EstadoReserva {
    PENDIENTE,
    CONFIRMADA,
    ENCURSO,
    FINALIZADA,
    CANCELADA,
    NOSHOW;

    public boolean puedeTransicionarA(EstadoReserva destino) {
        switch (this) {
            case PENDIENTE:
                return destino == CONFIRMADA || destino == CANCELADA;
            case CONFIRMADA:
                return destino == ENCURSO || destino == CANCELADA || destino == NOSHOW;
            case ENCURSO:
                return destino == FINALIZADA;
            default:
                return false; // FINALIZADA, CANCELADA y NOSHOW son estados finales
        }
    }

    // Solo las reservas activas ocupan noches y pueden generar solapamiento
    public boolean esActiva() {
        return this == PENDIENTE || this == CONFIRMADA || this == ENCURSO;
    }
}
