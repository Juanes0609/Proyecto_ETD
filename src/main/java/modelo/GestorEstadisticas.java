package modelo;

import java.time.LocalDate;

public class GestorEstadisticas {
    private final SistemaGestionAlojamiento sistema;
    private final GestorPreparacion preparacion;
    private final GestorCanalExterno canalExterno;
    private final GestorMapaEdificio mapa;

    public GestorEstadisticas(SistemaGestionAlojamiento sistema,
                              GestorPreparacion preparacion,
                              GestorCanalExterno canalExterno,
                              GestorMapaEdificio mapa) {
        if (sistema == null || preparacion == null || canalExterno == null || mapa == null) {
            throw new IllegalArgumentException("Todos los gestores son obligatorios");
        }
        this.sistema = sistema;
        this.preparacion = preparacion;
        this.canalExterno = canalExterno;
        this.mapa = mapa;
    }

    /**
     * @param desde             primera noche del periodo (inclusive)
     * @param hasta             fin del periodo (exclusivo)
     * @param temporadas        temporadas definidas; las noches que no caigan
     *                          en ninguna se cuentan como BAJA (temporada base). Puede ser null.
     * @param codigoRecepcion   nodo desde el que se hacen los recorridos BFS y DFS
     */
    public ReporteEstadisticas generar(LocalDate desde, LocalDate hasta,
                                       Iterable<Temporada> temporadas,
                                       String codigoRecepcion) {
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            throw new IllegalArgumentException("El periodo del reporte no es válido");
        }

        ReporteEstadisticas reporte = new ReporteEstadisticas();
        calcularApartamentosYOcupacion(reporte, desde, hasta, temporadas);
        calcularReservasEIngresos(reporte);

        reporte.setConflictosDeCanal(canalExterno.getConflictos().getTam());
        reporte.setTiempoPromedioPreparacion(preparacion.minutosPromedioPreparacion());
        reporte.setOrdenesPendientes(preparacion.cantidadPendientes());

        calcularRecorridosDelGrafo(reporte, codigoRecepcion);
        return reporte;
    }

    // ---------- apartamentos y ocupación ----------

    private void calcularApartamentosYOcupacion(ReporteEstadisticas reporte,
                                                LocalDate desde, LocalDate hasta,
                                                Iterable<Temporada> temporadas) {
        int total = 0;
        int ocupados = 0;
        int fueraDeServicio = 0;
        int[] porEstado = new int[EstadoOperativo.values().length];
        double[] nochesPorTemporada = new double[TipoTemporada.values().length];

        for (Apartamento apartamento : sistema.listarApartamentos()) {
            total++;
            porEstado[apartamento.getEstadoOperativo().ordinal()]++;
            if (apartamento.getEstadoOperativo() == EstadoOperativo.OCUPADO) {
                ocupados++;
            }
            if (apartamento.getEstadoOperativo() == EstadoOperativo.FUERASERVICIO) {
                fueraDeServicio++;
            }

            int nochesApartamento = 0;
            for (Reserva reserva : apartamento.getReservas()) {
                if (!ocupaNoches(reserva)) {
                    continue;
                }
                for (LocalDate noche : reserva.getEstancia()) {
                    if (!noche.isBefore(desde) && noche.isBefore(hasta)) {
                        nochesApartamento++;
                        nochesPorTemporada[tipoDeTemporada(noche, temporadas).ordinal()]++;
                    }
                }
            }
            reporte.getOcupacionPorApartamento().agregarFinal(
                    new ConteoEstadistica(apartamento.getCodigo(), nochesApartamento));
        }

        for (EstadoOperativo estado : EstadoOperativo.values()) {
            reporte.getApartamentosPorEstado().agregarFinal(
                    new ConteoEstadistica(estado.name(), porEstado[estado.ordinal()]));
        }
        for (TipoTemporada tipo : TipoTemporada.values()) {
            reporte.getOcupacionPorTemporada().agregarFinal(
                    new ConteoEstadistica(tipo.name(), nochesPorTemporada[tipo.ordinal()]));
        }

        int operativos = total - fueraDeServicio;
        reporte.setTotalApartamentos(total);
        reporte.setOcupadosAhora(ocupados);
        reporte.setPorcentajeOcupacionActual(
                operativos == 0 ? 0 : ocupados * 100.0 / operativos);
    }

    /** Las reservas canceladas y no-show no ocupan noches. */
    private boolean ocupaNoches(Reserva reserva) {
        EstadoReserva estado = reserva.getEstadoReserva();
        return reserva.getEstancia() != null
                && estado != null
                && estado != EstadoReserva.CANCELADA
                && estado != EstadoReserva.NOSHOW;
    }

    private TipoTemporada tipoDeTemporada(LocalDate noche, Iterable<Temporada> temporadas) {
        if (temporadas != null) {
            for (Temporada t : temporadas) {
                if (!noche.isBefore(t.getFechaInicio()) && !noche.isAfter(t.getFechaFin())) {
                    return t.getTipoTemporada();
                }
            }
        }
        return TipoTemporada.BAJA;
    }

    // ---------- reservas por canal / estado e ingresos ----------

    private void calcularReservasEIngresos(ReporteEstadisticas reporte) {
        int[] porCanal = new int[Canal.values().length];
        int[] porEstado = new int[EstadoReserva.values().length];
        double ingresos = 0;

        for (Apartamento apartamento : sistema.listarApartamentos()) {
            for (Reserva reserva : apartamento.getReservas()) {
                // Cuenta TODAS las reservas, también canceladas y no-show.
                if (reserva.getCanal() != null) {
                    porCanal[reserva.getCanal().ordinal()]++;
                }
                if (reserva.getEstadoReserva() != null) {
                    porEstado[reserva.getEstadoReserva().ordinal()]++;
                }
                ingresos += ingresosDe(reserva);
            }
        }

        for (Canal canal : Canal.values()) {
            reporte.getReservasPorCanal().agregarFinal(
                    new ConteoEstadistica(canal.name(), porCanal[canal.ordinal()]));
        }
        for (EstadoReserva estado : EstadoReserva.values()) {
            reporte.getReservasPorEstado().agregarFinal(
                    new ConteoEstadistica(estado.name(), porEstado[estado.ordinal()]));
        }
        reporte.setIngresos(ingresos);
    }

    /**
     * Dinero realmente cobrado en el folio: pagos menos reembolsos.
     * Deshacer un pago crea un CARGO "Reverso de ..." (se resta); deshacer un
     * cargo crea un PAGO "Reverso de ..." (no es ingreso, se ignora).
     */
    private double ingresosDe(Reserva reserva) {
        if (reserva.getFolio() == null) {
            return 0;
        }
        double neto = 0;
        for (MovimientoFolio m : reserva.getFolio().getListMovimientosFolio()) {
            boolean esReverso = m.getConcepto() != null && m.getConcepto().startsWith("Reverso");
            if (m.getTipoMovimiento() == TipoMovimientoFolio.PAGO && !esReverso) {
                neto += m.getValor();
            } else if (m.getTipoMovimiento() == TipoMovimientoFolio.CARGO && esReverso) {
                neto -= m.getValor();
            }
        }
        return neto;
    }

    // ---------- recorridos del grafo ----------

    private void calcularRecorridosDelGrafo(ReporteEstadisticas reporte, String codigoRecepcion) {
        reporte.setUbicaciones(mapa.contarUbicaciones());
        reporte.setPasillos(mapa.contarPasillos());
        reporte.setPasillosCerrados(mapa.contarPasillosCerrados());

        if (codigoRecepcion != null && mapa.getGrafo().buscarVertice(codigoRecepcion) != null) {
            reporte.setAlcanzablesPorBFS(
                    mapa.getGrafo().recorridoBFS(codigoRecepcion).getTam());
            reporte.setVisitadosPorDFS(
                    mapa.getGrafo().recorridoDFS(codigoRecepcion).getTam());
            reporte.setInalcanzables(
                    mapa.apartamentosInalcanzables(codigoRecepcion).getTam());
        }
    }
}
