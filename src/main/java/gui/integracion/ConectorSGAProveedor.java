package gui.integracion;

import estructuras.ListaDoblementeEnlazada;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.ConexionDTO;
import gui.integracion.dto.EmpleadoDTO;
import gui.integracion.dto.HuespedDTO;
import gui.integracion.dto.NodoEdificioDTO;
import gui.integracion.dto.OrdenPreparacionDTO;
import gui.integracion.dto.ReporteDTO;
import gui.integracion.dto.RutaDTO;
import gui.integracion.dto.SolicitudExternaDTO;
import gui.integracion.dto.TemporadaDTO;
import modelo.Apartamento;
import estructuras.Arista;
import modelo.AsignadorGrupos;
import modelo.Estancia;
import modelo.ConflictoCanal;
import modelo.ConstructorEdificio;
import modelo.EstadoOperativo;
import modelo.Huesped;
import modelo.NodoEdificio;
import modelo.OrdenPreparacion;
import modelo.Recepcionista;
import modelo.Temporada;
import modelo.TipoTemporada;
import modelo.ReporteEstadisticas;
import modelo.SistemaGestionAlojamiento;
import modelo.SolicitudExterna;
import estructuras.Vertice;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de {@link ConectorSGA} que delega en
 * {@code SistemaGestionAlojamiento}. Cada método se sobrescribe aquí a medida
 * que el núcleo (modelo, estructuras y servicio) quede implementado; mientras
 * tanto se heredan los métodos por defecto, que avisan "en construcción".
 * <p>
 * Ya implementado: catálogo de aparta-suites y tarifas, huéspedes y favoritos
 * (árbol binario y conjunto), mapa del edificio (grafo, BFS y Dijkstra),
 * preparación (cola de prioridad), canal externo (cola FIFO), turnos (lista
 * circular) y estadísticas.
 * <p>
 * La aplicación usa {@link ConectorSGAHibrido}, que extiende esta clase y cubre
 * lo que el núcleo todavía no ofrece.
 */
public class ConectorSGAProveedor implements ConectorSGA {
    private SistemaGestionAlojamiento sistema;
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

    /** Cambia el sistema completo (al cargar datos guardados); el grafo se vuelve a armar. */
    protected void reemplazarSistema(SistemaGestionAlojamiento nuevo) {
        if (nuevo == null) {
            throw new IllegalArgumentException("El sistema es obligatorio");
        }
        sistema = nuevo;
        edificioConstruido = false;
    }

    // ---------- Catálogo y tarifas ----------

    @Override
    public List<ApartaSuiteDTO> listarApartaSuites() {
        List<ApartaSuiteDTO> lista = new ArrayList<>();
        for (Apartamento a : sistema.listarApartamentos()) {
            lista.add(aDto(a));
        }
        return lista;
    }

    /**
     * Registra un aparta-suite nuevo o modifica el existente en su sitio (así
     * conserva sus reservas, órdenes de preparación y nodo del mapa).
     */
    @Override
    public void guardarApartaSuite(ApartaSuiteDTO d) {
        String caracteristicas = d.caracteristicas() == null ? "" : String.join(", ", d.caracteristicas());
        Apartamento a = sistema.buscarApartamento(d.codigo());
        if (a == null) {
            a = new Apartamento(d.codigo(), d.nombre(), d.bloque(), d.piso(), d.dormitorios(), d.capacidad(),
                    EstadoOperativo.PREPARADO, caracteristicas, "");
            a.actualizarTarifas(d.tarifaBaja(), d.tarifaMedia(), d.tarifaAlta());
            sistema.registrarApartamento(a);
            if ("FUERA_DE_SERVICIO".equals(d.estadoOperativo()) && d.motivoFueraServicio() != null
                    && !d.motivoFueraServicio().isBlank()) {
                cambiarEstadoOperativo(a, EstadoOperativo.FUERASERVICIO, d.motivoFueraServicio());
            }
            return;
        }
        if (d.capacidad() <= 0) {
            throw new IllegalArgumentException("La capacidad máxima debe ser mayor que cero");
        }
        a.actualizarTarifas(d.tarifaBaja(), d.tarifaMedia(), d.tarifaAlta());
        a.setNombre(d.nombre());
        a.setBloque(d.bloque());
        a.setPiso(d.piso());
        a.setNumeroDormitorios(d.dormitorios());
        a.setCapacidadMaxima(d.capacidad());
        a.setCaracteristicas(caracteristicas);
        if (d.estadoOperativo() != null) {
            cambiarEstadoOperativo(a, estadoModelo(d.estadoOperativo()), d.motivoFueraServicio());
        }
    }

    /**
     * Fuera de servicio exige registrar el motivo y solo se permite si el aparta-suite
     * no está ocupado ni en preparación. Esas reglas viven en el modelo ({@code Apartamento}).
     */
    private void cambiarEstadoOperativo(Apartamento a, EstadoOperativo nuevo, String motivo) {
        if (nuevo == EstadoOperativo.FUERASERVICIO) {
            a.ponerFueraDeServicio(motivo);
        } else if (a.getEstadoOperativo() == EstadoOperativo.FUERASERVICIO) {
            a.volverAServicio();
            a.setEstadoOperativo(nuevo);
        } else {
            a.setEstadoOperativo(nuevo);
        }
    }

    @Override
    public void actualizarTarifas(String codigoApartaSuite, double baja, double media, double alta) {
        apartamento(codigoApartaSuite).actualizarTarifas(baja, media, alta);
    }

    /**
     * Grupo grande con el backtracking de {@code AsignadorGrupos}. Los candidatos se
     * filtran aquí y no con {@code SistemaGestionAlojamiento.asignarGrupoGrande},
     * que hoy solo acepta aparta-suites Preparados (un aparta-suite ocupado hoy
     * puede estar libre en las fechas del grupo; ver PLAN_NUCLEO 2.1).
     */
    @Override
    public List<ApartaSuiteDTO> asignarGrupo(LocalDate entrada, LocalDate salida, int personas) {
        Estancia estancia = new Estancia(entrada, salida, null); // valida el rango de fechas
        ListaDoblementeEnlazada<Apartamento> libres = new ListaDoblementeEnlazada<>();
        for (Apartamento a : sistema.listarApartamentos()) {
            if (a.getEstadoOperativo() != EstadoOperativo.FUERASERVICIO && !a.tieneSolapamiento(estancia)) {
                libres.agregarFinal(a);
            }
        }
        List<ApartaSuiteDTO> combinacion = new ArrayList<>();
        for (Apartamento a : AsignadorGrupos.asignar(libres, personas)) {
            combinacion.add(aDto(a));
        }
        return combinacion;
    }

    protected ApartaSuiteDTO aDto(Apartamento a) {
        return new ApartaSuiteDTO(a.getCodigo(), a.getNombre(), a.getBloque(), a.getPiso(),
                a.getNumeroDormitorios(), a.getCapacidadMaxima(), a.getTarifaBaja(), a.getTarifaMedia(),
                a.getTarifaAlta(), caracteristicasDe(a.getCaracteristicas()), descripcionDe(a.getCodigo()),
                estadoDto(a.getEstadoOperativo()), null, a.getMotivoFueraServicio());
    }

    /** El modelo guarda las características en un solo texto separado por comas. */
    private static List<String> caracteristicasDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return Arrays.stream(texto.split(",")).map(String::trim).filter(c -> !c.isEmpty()).toList();
    }

    /** El modelo no guarda descripción; quien extienda el conector puede aportarla. */
    protected String descripcionDe(String codigoApartaSuite) {
        return "";
    }

    protected Apartamento apartamento(String codigo) {
        Apartamento a = sistema.buscarApartamento(codigo);
        if (a == null) {
            throw new IllegalArgumentException("No existe el aparta-suite " + codigo + ".");
        }
        return a;
    }

    // ---------- Huéspedes y favoritos ----------

    @Override
    public List<HuespedDTO> listarHuespedes() {
        List<HuespedDTO> lista = new ArrayList<>();
        for (Huesped h : sistema.listarHuespedes()) {
            lista.add(aDto(h));
        }
        return lista;
    }

    @Override
    public HuespedDTO obtenerHuesped(String documento) {
        return aDto(huesped(documento));
    }

    @Override
    public void registrarHuesped(HuespedDTO d) {
        sistema.registrarHuesped(new Huesped(d.nombre(), d.documento(), d.telefono(), d.fechaNacimiento(),
                d.correo(), null));
    }

    /** Modifica el huésped en su sitio para no perder sus favoritos ni su historial. */
    @Override
    public void actualizarHuesped(HuespedDTO d) {
        if (d.nombre() == null || d.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        Huesped h = huesped(d.documento());
        h.setNombre(d.nombre());
        h.setTelefono(d.telefono());
        h.setFechaNacimiento(d.fechaNacimiento());
        h.setCorreo(d.correo());
    }

    /** Agrega o quita el favorito en el {@code Conjunto} del huésped. */
    @Override
    public boolean alternarFavorito(String documentoHuesped, String codigoApartaSuite) {
        Huesped h = huesped(documentoHuesped);
        Apartamento a = apartamento(codigoApartaSuite);
        if (h.esFavorito(a)) {
            h.quitarFavorito(a);
            return false;
        }
        h.agregarFavorito(a);
        return true;
    }

    @Override
    public List<ApartaSuiteDTO> favoritos(String documentoHuesped) {
        List<ApartaSuiteDTO> lista = new ArrayList<>();
        for (Apartamento a : huesped(documentoHuesped).getFavoritos()) {
            lista.add(aDto(a));
        }
        return lista;
    }

    protected HuespedDTO aDto(Huesped h) {
        return new HuespedDTO(h.getDocumento(), h.getNombre(), h.getFechaNacimiento(), h.getTelefono(),
                h.getCorreo());
    }

    protected Huesped huesped(String documento) {
        Huesped h = sistema.buscarHuesped(documento);
        if (h == null) {
            throw new IllegalArgumentException("No hay un huésped con el documento " + documento + ".");
        }
        return h;
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

    /** Grupos por bloque del recorrido DFS del grafo ({@code GestorMapaEdificio.agruparPorBloque}). */
    @Override
    public Map<String, List<String>> apartaSuitesPorBloque() {
        asegurarEdificio();
        Map<String, List<String>> grupos = new LinkedHashMap<>();
        for (ListaDoblementeEnlazada<NodoEdificio> grupo
                : sistema.getMapa().agruparPorBloque(ConstructorEdificio.RECEPCION)) {
            List<String> codigos = new ArrayList<>();
            for (NodoEdificio n : grupo) {
                codigos.add(n.getCodigo());
            }
            grupos.put(grupo.primero().getApartamento().getBloque(), codigos);
        }
        return grupos;
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
            lista.add(new SolicitudExternaDTO(s.getCodigo(),
                    s.getNombreHuesped() == null ? "Plataforma externa" : s.getNombreHuesped(),
                    s.getDocumentoHuesped() == null ? "-" : s.getDocumentoHuesped(),
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

    // ---------- Temporadas ----------

    @Override
    public List<TemporadaDTO> listarTemporadas() {
        List<TemporadaDTO> lista = new ArrayList<>();
        for (Temporada t : sistema.listarTemporadas()) {
            lista.add(new TemporadaDTO(t.getTipoTemporada().name(), t.getFechaInicio(), t.getFechaFin()));
        }
        lista.sort(java.util.Comparator.comparing(TemporadaDTO::inicio));
        return lista;
    }

    /** El sistema valida que no se cruce con otra temporada. */
    @Override
    public void agregarTemporada(TemporadaDTO temporada) {
        sistema.agregarTemporada(new Temporada(temporada.inicio(), temporada.fin(),
                TipoTemporada.valueOf(temporada.tipo())));
    }

    /** Las noches que no caen en ninguna temporada definida son BAJA. */
    @Override
    public String temporadaEn(LocalDate fecha) {
        return sistema.temporadaDe(fecha).name();
    }

    // ---------- Empleados y turnos ----------

    /** Turno vigente: el recepcionista donde está el cursor de la lista circular. */
    @Override
    public EmpleadoDTO turnoActual() {
        return aDto(sistema.getTurnos().turnoActual());
    }

    /** Una vuelta de la rotación, empezando por el primero. */
    @Override
    public List<EmpleadoDTO> ordenTurnos() {
        List<EmpleadoDTO> orden = new ArrayList<>();
        for (Recepcionista r : sistema.getTurnos().listarOrdenTurnos()) {
            orden.add(aDto(r));
        }
        return orden;
    }

    @Override
    public EmpleadoDTO avanzarTurno() {
        return aDto(sistema.getTurnos().avanzarTurno());
    }

    @Override
    public void reiniciarTurnos() {
        sistema.getTurnos().reiniciarTurno();
    }

    /** Empleados en orden de documento (recorrido in-orden del árbol de empleados). */
    @Override
    public List<EmpleadoDTO> listarEmpleados() {
        List<EmpleadoDTO> lista = new ArrayList<>();
        for (Recepcionista r : sistema.listarEmpleados()) {
            lista.add(aDto(r));
        }
        return lista;
    }

    /** Lo guarda en el árbol de empleados y lo pone al final de la rotación. */
    @Override
    public void registrarEmpleado(EmpleadoDTO empleado) {
        sistema.registrarEmpleado(new Recepcionista(empleado.nombre(), empleado.documento(), null, null,
                empleado.turno()));
    }

    protected EmpleadoDTO aDto(Recepcionista r) {
        return new EmpleadoDTO(r.getDocumento(), r.getNombre(), r.getHorario() == null ? "-" : r.getHorario());
    }

    // ---------- Estadísticas ----------

    @Override
    public ReporteDTO generarReporte(LocalDate desde, LocalDate hasta) {
        asegurarEdificio();
        ReporteEstadisticas r = sistema.getEstadisticas().generar(desde, hasta, null, ConstructorEdificio.RECEPCION);
        return aReporte(r, desde, hasta, r.getIngresos(), r.getConflictosDeCanal(), r.getTiempoPromedioPreparacion());
    }

    /** Pasa a la GUI lo que {@code GestorEstadisticas} calculó recorriendo las estructuras. */
    protected ReporteDTO aReporte(ReporteEstadisticas r, LocalDate desde, LocalDate hasta, double ingresos,
                                  int conflictos, double minutosPreparacion) {
        Map<String, Integer> reservasPorEstado = new LinkedHashMap<>();
        for (modelo.EstadoReserva e : modelo.EstadoReserva.values()) {
            String nombre = switch (e) {
                case ENCURSO -> "EN_CURSO";
                case NOSHOW -> "NO_SHOW";
                default -> e.name();
            };
            reservasPorEstado.put(nombre, (int) ReporteEstadisticas.valorDe(r.getReservasPorEstado(), e.name()));
        }
        Map<String, Integer> suitesPorEstado = new LinkedHashMap<>();
        for (EstadoOperativo e : EstadoOperativo.values()) {
            suitesPorEstado.put(estadoDto(e),
                    (int) ReporteEstadisticas.valorDe(r.getApartamentosPorEstado(), e.name()));
        }
        return new ReporteDTO(desde, hasta, r.getTotalApartamentos(), r.getOcupadosAhora(),
                r.getPorcentajeOcupacionActual(), ingresos, conteos(r.getReservasPorCanal()), reservasPorEstado,
                suitesPorEstado, conteos(r.getOcupacionPorApartamento()), conteos(r.getOcupacionPorTemporada()),
                conflictos, minutosPreparacion);
    }

    private static Map<String, Integer> conteos(ListaDoblementeEnlazada<modelo.ConteoEstadistica> lista) {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        for (modelo.ConteoEstadistica c : lista) {
            mapa.put(c.etiqueta(), (int) c.valor());
        }
        return mapa;
    }

    protected static String estadoDto(EstadoOperativo e) {
        return switch (e) {
            case PREPARADO -> "PREPARADO";
            case OCUPADO -> "OCUPADO";
            case PENDIENTEPREPARACION -> "PENDIENTE_PREPARACION";
            case ENPREPARACION -> "EN_PREPARACION";
            case FUERASERVICIO -> "FUERA_DE_SERVICIO";
        };
    }

    protected static EstadoOperativo estadoModelo(String estado) {
        return switch (estado) {
            case "OCUPADO" -> EstadoOperativo.OCUPADO;
            case "PENDIENTE_PREPARACION" -> EstadoOperativo.PENDIENTEPREPARACION;
            case "EN_PREPARACION" -> EstadoOperativo.ENPREPARACION;
            case "FUERA_DE_SERVICIO" -> EstadoOperativo.FUERASERVICIO;
            default -> EstadoOperativo.PREPARADO;
        };
    }
}
