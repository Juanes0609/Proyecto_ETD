package gui.integracion;

import estructuras.ListaDoblementeEnlazada;
import gui.integracion.dto.ConexionDTO;
import gui.integracion.dto.EmpleadoDTO;
import gui.integracion.dto.NodoEdificioDTO;
import gui.integracion.dto.OrdenPreparacionDTO;
import gui.integracion.dto.ReporteDTO;
import gui.integracion.dto.RutaDTO;
import gui.integracion.dto.SolicitudExternaDTO;
import modelo.Apartamento;
import modelo.Arista;
import modelo.ConflictoCanal;
import modelo.ConstructorEdificio;
import modelo.EstadoOperativo;
import modelo.NodoEdificio;
import modelo.OrdenPreparacion;
import modelo.ReporteEstadisticas;
import modelo.SistemaGestionAlojamiento;
import modelo.SolicitudExterna;
import modelo.Vertice;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de {@link ConectorSGA} que delega en
 * {@code SistemaGestionAlojamiento}. Cada método se sobrescribe aquí a medida
 * que el núcleo (modelo, estructuras y servicio) quede implementado; mientras
 * tanto se heredan los métodos por defecto, que avisan "en construcción".
 * <p>
 * Ya implementado: mapa del edificio, preparación (cola de prioridad), canal
 * externo (cola FIFO), turnos (lista circular) y estadísticas de recorridos.
 * <p>
 * Para usarlo, cambiar {@code new ConectorSGADemo()} por
 * {@code new ConectorSGAProveedor(sistema)} en {@code AplicacionSGA}.
 */
public class ConectorSGAProveedor implements ConectorSGA {
    private final SistemaGestionAlojamiento sistema;
    private boolean edificioConstruido;

    public ConectorSGAProveedor() {
        this(new SistemaGestionAlojamiento());
    }

    public ConectorSGAProveedor(SistemaGestionAlojamiento sistema) {
        if (sistema == null) {
            throw new IllegalArgumentException("El sistema es obligatorio");
        }
        this.sistema = sistema;
    }

    public SistemaGestionAlojamiento getSistema() {
        return sistema;
    }

    /** El grafo se arma con los apartamentos que existan al primer uso. */
    private void asegurarEdificio() {
        if (!edificioConstruido) {
            ConstructorEdificio.construir(sistema.getMapa(), sistema);
            edificioConstruido = true;
        }
    }

    /** Fuerza a reconstruir el grafo (por ejemplo, tras cargar un escenario nuevo). */
    public void reconstruirEdificio() {
        edificioConstruido = false;
    }

    // ---------- Mapa del edificio ----------

    @Override
    public List<NodoEdificioDTO> nodosEdificio() {
        asegurarEdificio();
        List<NodoEdificioDTO> lista = new ArrayList<>();
        for (Vertice<NodoEdificio> v : sistema.getMapa().getGrafo().getVertices()) {
            NodoEdificio n = v.getDato();
            String bloque = bloqueDe(n);
            lista.add(new NodoEdificioDTO(n.getCodigo(), n.getNombre(), tipoDto(n),
                    bloque, n.getX(), n.getY()));
        }
        return lista;
    }

    /** Apartamentos: su bloque. Pasillo y escalera: el sufijo de su código (P-A, ESC-A). */
    private String bloqueDe(NodoEdificio n) {
        if (n.getApartamento() != null) {
            return n.getApartamento().getBloque();
        }
        String codigo = n.getCodigo();
        boolean deBloque = codigo.startsWith("P-") || codigo.startsWith("ESC-");
        return deBloque ? codigo.substring(codigo.lastIndexOf('-') + 1) : "-";
    }

    private String tipoDto(NodoEdificio n) {
        return switch (n.getTipoNodoEdificio()) {
            case APARTAMENTO -> "APARTAMENTO";
            case RECEPCION -> "RECEPCION";
            case ESCALERAS -> "ESCALERA";
            case CUARTOLIMPIEZA -> "CUARTO_ASEO";
            case PASILLO -> "PASILLO";
        };
    }

    /** Un pasillo se muestra una sola vez, aunque en el grafo sean dos aristas. */
    @Override
    public List<ConexionDTO> conexionesEdificio() {
        asegurarEdificio();
        List<ConexionDTO> lista = new ArrayList<>();
        ListaDoblementeEnlazada<String> vistos = new ListaDoblementeEnlazada<>();
        for (Arista<NodoEdificio> a : sistema.getMapa().getGrafo().getAristas()) {
            String o = a.getOrigen().getCodigo();
            String d = a.getDestino().getCodigo();
            String clave = o.compareTo(d) < 0 ? o + "|" + d : d + "|" + o;
            if (vistos.contiene(clave)) {
                continue;
            }
            vistos.agregarFinal(clave);
            lista.add(new ConexionDTO(o, d, a.getPeso(),
                    sistema.getMapa().pasilloAbierto(o, d)));
        }
        return lista;
    }

    @Override
    public void alternarPasillo(String origen, String destino) {
        asegurarEdificio();
        if (sistema.getMapa().pasilloAbierto(origen, destino)) {
            sistema.getMapa().cerrarPasillo(origen, destino);
        } else {
            sistema.getMapa().abrirPasillo(origen, destino);
        }
    }

    @Override
    public List<String> apartaSuitesInalcanzables() {
        asegurarEdificio();
        List<String> codigos = new ArrayList<>();
        for (NodoEdificio n : sistema.getMapa().apartamentosInalcanzables(ConstructorEdificio.RECEPCION)) {
            if (n.getApartamento() != null) {
                codigos.add(n.getCodigo());
            }
        }
        return codigos;
    }

    // ---------- Preparación ----------

    @Override
    public List<OrdenPreparacionDTO> colaPreparacion() {
        List<OrdenPreparacionDTO> lista = new ArrayList<>();
        for (OrdenPreparacion o : sistema.getPreparacion().listarPendientes()) {
            lista.add(aDto(o));
        }
        return lista;
    }

    @Override
    public List<OrdenPreparacionDTO> enPreparacion() {
        List<OrdenPreparacionDTO> lista = new ArrayList<>();
        for (OrdenPreparacion o : sistema.getPreparacion().listarEnPreparacion()) {
            lista.add(aDto(o));
        }
        return lista;
    }

    @Override
    public OrdenPreparacionDTO despacharSiguientePreparacion() {
        return aDto(sistema.getPreparacion().despacharSiguiente());
    }

    @Override
    public void terminarPreparacion(String codigoApartaSuite) {
        sistema.getPreparacion().marcarPreparado(codigoApartaSuite);
    }

    private OrdenPreparacionDTO aDto(OrdenPreparacion o) {
        Apartamento a = sistema.buscarApartamento(o.getCodigo());
        return new OrdenPreparacionDTO(o.getCodigo(), a != null ? a.getNombre() : o.getCodigo(),
                o.getPrioridad() == 1,
                o.getHoraLlegadaEstimada() != null ? o.getHoraLlegadaEstimada().toLocalTime() : null,
                o.getFechaCreacion());
    }

    /** Ruta más corta (Dijkstra) desde el cuarto de aseo hasta un aparta-suite. */
    @Override
    public RutaDTO rutaPreparacion(String codigoApartaSuite) {
        asegurarEdificio();
        ListaDoblementeEnlazada<NodoEdificio> ruta = sistema.getMapa()
                .rutaMasCorta(ConstructorEdificio.ASEO, codigoApartaSuite);
        if (ruta.estaVacio()) {
            throw new IllegalStateException("No hay ruta hasta " + codigoApartaSuite
                    + " con los pasillos abiertos.");
        }
        return aRuta(ruta);
    }

    /** Recorrido completo para atender toda la cola en orden de prioridad. */
    @Override
    public RutaDTO rutaRecorridoPreparacion() {
        asegurarEdificio();
        ListaDoblementeEnlazada<NodoEdificio> ruta = sistema.getMapa().rutaParaPreparacion(
                ConstructorEdificio.ASEO, sistema.getPreparacion().listarPendientes());
        return aRuta(ruta);
    }

    private RutaDTO aRuta(ListaDoblementeEnlazada<NodoEdificio> ruta) {
        List<String> pasos = new ArrayList<>();
        for (NodoEdificio n : ruta) {
            pasos.add(n.getNombre());
        }
        return new RutaDTO(pasos, sistema.getMapa().distanciaDeRuta(ruta));
    }

    // ---------- Canal externo ----------

    @Override
    public List<SolicitudExternaDTO> colaCanalExterno() {
        List<SolicitudExternaDTO> lista = new ArrayList<>();
        for (SolicitudExterna s : sistema.getCanalExterno().listarPendientes()) {
            lista.add(new SolicitudExternaDTO(s.getCodigo(), "Plataforma externa", "-",
                    s.getCodigoApartamentoSolicitado(), s.getFechaEntrada(), s.getFechaSalida(),
                    s.getCantidadPersonas(), s.getFechaSolicitud()));
        }
        return lista;
    }

    @Override
    public String procesarSiguienteSolicitudExterna() {
        SolicitudExterna solicitud = sistema.getCanalExterno().consultarSiguiente();
        ConflictoCanal conflicto = sistema.getCanalExterno().procesarSiguiente();
        String codigo = solicitud != null ? solicitud.getCodigo() : "";
        return conflicto == null
                ? "Solicitud " + codigo + " aceptada: reserva creada."
                : "Solicitud " + codigo + " rechazada. " + conflicto.getMotivo();
    }

    // ---------- Turnos ----------

    @Override
    public EmpleadoDTO turnoActual() {
        return new EmpleadoDTO("-", sistema.getTurnos().turnoActual(), "Turno actual");
    }

    // ---------- Estadísticas ----------

    @Override
    public ReporteDTO generarReporte() {
        asegurarEdificio();
        LocalDate hoy = LocalDate.now();
        ReporteEstadisticas r = sistema.getEstadisticas().generar(
                hoy.withDayOfMonth(1), hoy.withDayOfMonth(1).plusMonths(1), null,
                ConstructorEdificio.RECEPCION);

        Map<String, Integer> porCanal = new LinkedHashMap<>();
        for (modelo.ConteoEstadistica c : r.getReservasPorCanal()) {
            porCanal.put(c.etiqueta(), (int) c.valor());
        }
        Map<String, Integer> porEstado = new LinkedHashMap<>();
        for (EstadoOperativo e : EstadoOperativo.values()) {
            porEstado.put(estadoDto(e),
                    (int) ReporteEstadisticas.valorDe(r.getApartamentosPorEstado(), e.name()));
        }
        return new ReporteDTO(r.getTotalApartamentos(), r.getOcupadosAhora(),
                r.getPorcentajeOcupacionActual(), r.getIngresos(), porCanal, porEstado,
                r.getConflictosDeCanal(), r.getTiempoPromedioPreparacion());
    }

    private String estadoDto(EstadoOperativo e) {
        return switch (e) {
            case PREPARADO -> "PREPARADO";
            case OCUPADO -> "OCUPADO";
            case PENDIENTEPREPARACION -> "PENDIENTE_PREPARACION";
            case ENPREPARACION -> "EN_PREPARACION";
            case FUERASERVICIO -> "FUERA_DE_SERVICIO";
        };
    }
}
