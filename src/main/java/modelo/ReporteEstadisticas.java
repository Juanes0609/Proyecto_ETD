package modelo;

import estructuras.ListaDoblementeEnlazada;

public class ReporteEstadisticas {
    // ----- ocupación -----
    private int totalApartamentos;
    private int ocupadosAhora;
    private double porcentajeOcupacionActual;
    private ListaDoblementeEnlazada<ConteoEstadistica> apartamentosPorEstado = new ListaDoblementeEnlazada<>();
    /** Noches ocupadas de cada apartamento dentro del periodo consultado. */
    private ListaDoblementeEnlazada<ConteoEstadistica> ocupacionPorApartamento = new ListaDoblementeEnlazada<>();
    /** Noches ocupadas por tipo de temporada dentro del periodo consultado. */
    private ListaDoblementeEnlazada<ConteoEstadistica> ocupacionPorTemporada = new ListaDoblementeEnlazada<>();

    // ----- reservas / dinero -----
    private ListaDoblementeEnlazada<ConteoEstadistica> reservasPorCanal = new ListaDoblementeEnlazada<>();
    /** Incluye canceladas y no-show. */
    private ListaDoblementeEnlazada<ConteoEstadistica> reservasPorEstado = new ListaDoblementeEnlazada<>();
    private double ingresos;
    private int conflictosDeCanal;

    // ----- preparación -----
    private double tiempoPromedioPreparacion;
    private int ordenesPendientes;

    // ----- recorridos del grafo -----
    private int ubicaciones;
    private int pasillos;
    private int pasillosCerrados;
    private int alcanzablesPorBFS;
    private int visitadosPorDFS;
    private int inalcanzables;

    public ReporteEstadisticas() {}

    public int getTotalApartamentos() { return totalApartamentos; }
    public void setTotalApartamentos(int v) { this.totalApartamentos = v; }

    public int getOcupadosAhora() { return ocupadosAhora; }
    public void setOcupadosAhora(int v) { this.ocupadosAhora = v; }

    public double getPorcentajeOcupacionActual() { return porcentajeOcupacionActual; }
    public void setPorcentajeOcupacionActual(double v) { this.porcentajeOcupacionActual = v; }

    public ListaDoblementeEnlazada<ConteoEstadistica> getApartamentosPorEstado() { return apartamentosPorEstado; }
    public ListaDoblementeEnlazada<ConteoEstadistica> getOcupacionPorApartamento() { return ocupacionPorApartamento; }
    public ListaDoblementeEnlazada<ConteoEstadistica> getOcupacionPorTemporada() { return ocupacionPorTemporada; }
    public ListaDoblementeEnlazada<ConteoEstadistica> getReservasPorCanal() { return reservasPorCanal; }
    public ListaDoblementeEnlazada<ConteoEstadistica> getReservasPorEstado() { return reservasPorEstado; }

    public double getIngresos() { return ingresos; }
    public void setIngresos(double v) { this.ingresos = v; }

    public int getConflictosDeCanal() { return conflictosDeCanal; }
    public void setConflictosDeCanal(int v) { this.conflictosDeCanal = v; }

    public double getTiempoPromedioPreparacion() { return tiempoPromedioPreparacion; }
    public void setTiempoPromedioPreparacion(double v) { this.tiempoPromedioPreparacion = v; }

    public int getOrdenesPendientes() { return ordenesPendientes; }
    public void setOrdenesPendientes(int v) { this.ordenesPendientes = v; }

    public int getUbicaciones() { return ubicaciones; }
    public void setUbicaciones(int v) { this.ubicaciones = v; }

    public int getPasillos() { return pasillos; }
    public void setPasillos(int v) { this.pasillos = v; }

    public int getPasillosCerrados() { return pasillosCerrados; }
    public void setPasillosCerrados(int v) { this.pasillosCerrados = v; }

    public int getAlcanzablesPorBFS() { return alcanzablesPorBFS; }
    public void setAlcanzablesPorBFS(int v) { this.alcanzablesPorBFS = v; }

    public int getVisitadosPorDFS() { return visitadosPorDFS; }
    public void setVisitadosPorDFS(int v) { this.visitadosPorDFS = v; }

    public int getInalcanzables() { return inalcanzables; }
    public void setInalcanzables(int v) { this.inalcanzables = v; }

    /** Valor de una lista de conteos por etiqueta, o 0 si no existe. */
    public static double valorDe(ListaDoblementeEnlazada<ConteoEstadistica> conteos, String etiqueta) {
        for (ConteoEstadistica c : conteos) {
            if (c.etiqueta().equals(etiqueta)) {
                return c.valor();
            }
        }
        return 0;
    }
}
