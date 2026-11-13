package gui.integracion;

import estructuras.ListaCircular;

import estructuras.Pila;
import excepciones.CapacidadExcedidaException;
import excepciones.EstructuraVaciaException;
import excepciones.FechasInvalidasException;
import excepciones.ReservaSolapadaException;
import excepciones.TransicionEstadoInvalidaException;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.ConexionDTO;
import gui.integracion.dto.CotizacionDTO;
import gui.integracion.dto.EmpleadoDTO;
import gui.integracion.dto.HuespedDTO;
import gui.integracion.dto.MovimientoFolioDTO;
import gui.integracion.dto.NocheDTO;
import gui.integracion.dto.NodoEdificioDTO;
import gui.integracion.dto.OcupanteDTO;
import gui.integracion.dto.OrdenPreparacionDTO;
import gui.integracion.dto.ReporteDTO;
import gui.integracion.dto.ReservaDTO;
import gui.integracion.dto.Rol;
import gui.integracion.dto.RutaDTO;
import gui.integracion.dto.SolicitudExternaDTO;
import gui.integracion.dto.TemporadaDTO;
import gui.integracion.dto.UsuarioDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Conector con datos de ejemplo en memoria para diseñar y probar la GUI
 * mientras el núcleo del equipo está en construcción.
 * <p>
 * <b>No es lógica del proyecto</b>: simula de forma simplificada las reglas del
 * documento (solapamientos, capacidad, check-in solo si está Preparado, folio
 * con deshacer, cola de preparación con prioridad, Dijkstra y BFS) solo para que
 * la interfaz se pueda recorrer de punta a punta. Usa colecciones de Java
 * porque vive en {@code gui/}, fuera del núcleo.
 * <p>
 * Cuentas: {@code huesped@demo.co}, {@code recepcion@demo.co} y
 * {@code admin@demo.co}, todas con la clave {@code 1234}.
 */
public class ConectorSGADemo implements ConectorSGA {

    public static final String CLAVE_DEMO = "1234";
    private static final double PORCENTAJE_ANTICIPO = 0.30;
    private static final String NODO_RECEPCION = "REC";
    private static final String NODO_ASEO = "ASEO";

    // ---------- Estado interno ----------

    private static final class Suite {
        String codigo, nombre, bloque, descripcion, estado;
        int piso, dormitorios, capacidad;
        double baja, media, alta;
        List<String> caracteristicas;

        ApartaSuiteDTO dto() {
            return new ApartaSuiteDTO(codigo, nombre, bloque, piso, dormitorios, capacidad, baja, media, alta,
                    caracteristicas, descripcion, estado, null);
        }
    }

    private static final class Reserva {
        String codigo, documentoTitular, codigoSuite, canal, estado;
        LocalDate entrada, salida;
        LocalTime horaLlegada;
        List<OcupanteDTO> ocupantes = new ArrayList<>();
        double total;
        final List<MovimientoFolioDTO> movimientos = new ArrayList<>();
        final Pila<MovimientoFolioDTO> deshacer = new Pila<>();
    }

    private record Cuenta(String clave, UsuarioDTO usuario) {
    }

    private record Orden(String codigoSuite, LocalDateTime ingreso) {
    }

    private final Map<String, Suite> suites = new LinkedHashMap<>();
    private final Map<String, HuespedDTO> huespedes = new LinkedHashMap<>();
    private final Map<String, Cuenta> cuentas = new LinkedHashMap<>();
    private final Map<String, Set<String>> favoritos = new HashMap<>();
    private final Map<String, Reserva> reservas = new LinkedHashMap<>();
    private final List<EmpleadoDTO> empleados = new ArrayList<>();
    /** Rotación de recepción con la lista circular del proyecto (sin eliminar ni reordenar). */
    private ListaCircular<EmpleadoDTO> rotacion = new ListaCircular<>();
    private final List<TemporadaDTO> temporadas = new ArrayList<>();
    private final Deque<SolicitudExternaDTO> colaExterna = new ArrayDeque<>();
    private final List<String> conflictos = new ArrayList<>();
    private final List<Orden> colaPreparacion = new ArrayList<>();
    private final Map<String, LocalDateTime> enPreparacion = new LinkedHashMap<>();
    private final List<Long> minutosPreparacion = new ArrayList<>();
    private final Map<String, NodoEdificioDTO> nodos = new LinkedHashMap<>();
    private final List<ConexionDTO> conexiones = new ArrayList<>();
    private int consecutivoReserva;

    public ConectorSGADemo() {
        cargarEscenarioInicial();
    }

    @Override
    public boolean esDemo() {
        return true;
    }

    @Override
    public final void cargarEscenarioInicial() {
        suites.clear();
        huespedes.clear();
        cuentas.clear();
        favoritos.clear();
        reservas.clear();
        empleados.clear();
        rotacion = new ListaCircular<>();
        temporadas.clear();
        colaExterna.clear();
        conflictos.clear();
        colaPreparacion.clear();
        enPreparacion.clear();
        minutosPreparacion.clear();
        nodos.clear();
        conexiones.clear();
        consecutivoReserva = 1000;
        sembrarDatos();
    }

    // ---------- Sesión ----------

    @Override
    public Optional<Rol> detectarRol(String correo) {
        Cuenta cuenta = cuentas.get(normalizar(correo));
        return cuenta == null ? Optional.empty() : Optional.of(cuenta.usuario().rol());
    }

    @Override
    public UsuarioDTO iniciarSesion(String correo, String clave) {
        Cuenta cuenta = cuentas.get(normalizar(correo));
        if (cuenta == null || !cuenta.clave().equals(clave)) {
            throw new IllegalArgumentException("Correo o contraseña incorrectos.");
        }
        return cuenta.usuario();
    }

    @Override
    public UsuarioDTO registrarCuentaHuesped(HuespedDTO datos, String clave) {
        String correo = normalizar(datos.correo());
        if (cuentas.containsKey(correo)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo.");
        }
        if (huespedes.containsKey(datos.documento()) && cuentas.values().stream()
                .anyMatch(c -> datos.documento().equals(c.usuario().documento()))) {
            throw new IllegalArgumentException("Ese documento ya tiene una cuenta registrada.");
        }
        huespedes.put(datos.documento(), datos);
        UsuarioDTO usuario = new UsuarioDTO(correo, datos.nombre(), Rol.HUESPED, datos.documento());
        cuentas.put(correo, new Cuenta(clave, usuario));
        return usuario;
    }

    // ---------- Catálogo y cotización ----------

    @Override
    public List<ApartaSuiteDTO> listarApartaSuites() {
        return suites.values().stream().map(Suite::dto).toList();
    }

    @Override
    public String temporadaEn(LocalDate fecha) {
        return temporadas.stream().filter(t -> t.incluye(fecha)).map(TemporadaDTO::tipo).findFirst().orElse("BAJA");
    }

    @Override
    public boolean estaDisponible(String codigoApartaSuite, LocalDate entrada, LocalDate salida) {
        return reservas.values().stream().noneMatch(r -> r.codigoSuite.equals(codigoApartaSuite)
                && bloquea(r) && seSolapan(r.entrada, r.salida, entrada, salida));
    }

    @Override
    public CotizacionDTO cotizar(String codigoApartaSuite, LocalDate entrada, LocalDate salida, int personasQuePagan) {
        validarFechas(entrada, salida, false);
        Suite suite = suite(codigoApartaSuite);
        List<NocheDTO> noches = new ArrayList<>();
        double total = 0;
        for (LocalDate noche = entrada; noche.isBefore(salida); noche = noche.plusDays(1)) {
            String temporada = temporadaEn(noche);
            double tarifa = suite.dto().tarifa(temporada);
            double subtotal = tarifa * personasQuePagan;
            noches.add(new NocheDTO(noche, temporada, personasQuePagan, tarifa, subtotal));
            total += subtotal;
        }
        return new CotizacionDTO(noches, personasQuePagan, total);
    }

    // ---------- Huéspedes ----------

    @Override
    public HuespedDTO obtenerHuesped(String documento) {
        HuespedDTO huesped = huespedes.get(documento);
        if (huesped == null) {
            throw new IllegalArgumentException("No hay un huésped con el documento " + documento + ".");
        }
        return huesped;
    }

    @Override
    public void actualizarHuesped(HuespedDTO huesped) {
        obtenerHuesped(huesped.documento());
        huespedes.put(huesped.documento(), huesped);
        cuentas.replaceAll((correo, cuenta) -> huesped.documento().equals(cuenta.usuario().documento())
                ? new Cuenta(cuenta.clave(), new UsuarioDTO(correo, huesped.nombre(), cuenta.usuario().rol(),
                huesped.documento()))
                : cuenta);
    }

    @Override
    public List<HuespedDTO> listarHuespedes() {
        return huespedes.values().stream().sorted(Comparator.comparing(HuespedDTO::documento)).toList();
    }

    @Override
    public void registrarHuesped(HuespedDTO huesped) {
        if (huespedes.containsKey(huesped.documento())) {
            throw new IllegalArgumentException("Ya existe un huésped con el documento " + huesped.documento() + ".");
        }
        huespedes.put(huesped.documento(), huesped);
    }

    @Override
    public boolean alternarFavorito(String documentoHuesped, String codigoApartaSuite) {
        Set<String> conjunto = favoritos.computeIfAbsent(documentoHuesped, d -> new LinkedHashSet<>());
        if (!conjunto.remove(codigoApartaSuite)) {
            conjunto.add(codigoApartaSuite);
            return true;
        }
        return false;
    }

    @Override
    public List<ApartaSuiteDTO> favoritos(String documentoHuesped) {
        return favoritos.getOrDefault(documentoHuesped, Set.of()).stream().map(c -> suites.get(c).dto()).toList();
    }

    @Override
    public List<ReservaDTO> reservasDeHuesped(String documentoHuesped) {
        return reservas.values().stream().filter(r -> r.documentoTitular.equals(documentoHuesped))
                .sorted(Comparator.comparing((Reserva r) -> r.entrada).reversed()).map(this::dto).toList();
    }

    // ---------- Reservas ----------

    @Override
    public ReservaDTO crearReserva(String codigoApartaSuite, LocalDate entrada, LocalDate salida,
                                   HuespedDTO titular, List<OcupanteDTO> acompanantes,
                                   String canal, LocalTime horaLlegada) {
        validarFechas(entrada, salida, true);
        Suite suite = suite(codigoApartaSuite);
        if (!suite.dto().reservable()) {
            throw new TransicionEstadoInvalidaException(suite.nombre + " está fuera de servicio.");
        }
        int personas = 1 + acompanantes.size();
        if (personas > suite.capacidad) {
            throw new CapacidadExcedidaException(suite.nombre + " admite hasta " + suite.capacidad
                    + " personas y la reserva tiene " + personas + ".");
        }
        if (!estaDisponible(codigoApartaSuite, entrada, salida)) {
            throw new ReservaSolapadaException(suite.nombre + " ya está reservado en parte de esas fechas.");
        }
        huespedes.putIfAbsent(titular.documento(), titular);

        Reserva reserva = new Reserva();
        reserva.codigo = "R-" + (++consecutivoReserva);
        reserva.documentoTitular = titular.documento();
        reserva.codigoSuite = codigoApartaSuite;
        reserva.entrada = entrada;
        reserva.salida = salida;
        reserva.canal = canal;
        reserva.horaLlegada = horaLlegada;
        reserva.estado = "PORTAL".equals(canal) ? "PENDIENTE" : "CONFIRMADA";
        reserva.ocupantes.add(new OcupanteDTO(titular.nombre(), titular.documento(), titular.fechaNacimiento()));
        reserva.ocupantes.addAll(acompanantes);
        int quePagan = (int) reserva.ocupantes.stream().filter(o -> o.paga(entrada)).count();
        reserva.total = cotizar(codigoApartaSuite, entrada, salida, quePagan).total();
        movimiento(reserva, "CARGO", "Alojamiento · " + reserva.ocupantes.size() + " personas", reserva.total, false);
        reservas.put(reserva.codigo, reserva);
        return dto(reserva);
    }

    @Override
    public void pagarAnticipo(String codigoReserva) {
        Reserva reserva = reserva(codigoReserva);
        if (!"PENDIENTE".equals(reserva.estado)) {
            throw new TransicionEstadoInvalidaException("La reserva " + codigoReserva + " no está pendiente de anticipo.");
        }
        movimiento(reserva, "PAGO", "Anticipo por el portal", Math.round(reserva.total * PORCENTAJE_ANTICIPO), false);
        reserva.estado = "CONFIRMADA";
    }

    @Override
    public double calcularDevolucion(String codigoReserva) {
        Reserva reserva = reserva(codigoReserva);
        double pagado = pagado(reserva);
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), reserva.entrada);
        if (dias >= 7) {
            return pagado;
        }
        return dias >= 2 ? Math.round(pagado * 0.5) : 0;
    }

    @Override
    public void cancelarReserva(String codigoReserva) {
        Reserva reserva = reserva(codigoReserva);
        if (!"PENDIENTE".equals(reserva.estado) && !"CONFIRMADA".equals(reserva.estado)) {
            throw new TransicionEstadoInvalidaException("Solo se pueden cancelar reservas pendientes o confirmadas.");
        }
        double devolucion = calcularDevolucion(codigoReserva);
        if (devolucion > 0) {
            movimiento(reserva, "ANULACION_PAGO", "Devolución por cancelación", devolucion, false);
        }
        reserva.estado = "CANCELADA";
    }

    @Override
    public List<ReservaDTO> reservasActivas() {
        return reservas.values().stream().filter(r -> dto(r).activa())
                .sorted(Comparator.comparing((Reserva r) -> r.entrada)).map(this::dto).toList();
    }

    @Override
    public void registrarCheckIn(String codigoReserva) {
        Reserva reserva = reserva(codigoReserva);
        Suite suite = suite(reserva.codigoSuite);
        if (!"CONFIRMADA".equals(reserva.estado)) {
            throw new TransicionEstadoInvalidaException("Solo se puede hacer check-in de una reserva confirmada.");
        }
        if (reserva.entrada.isAfter(LocalDate.now())) {
            throw new TransicionEstadoInvalidaException("La llegada de " + codigoReserva + " es el "
                    + reserva.entrada + ".");
        }
        if (!"PREPARADO".equals(suite.estado)) {
            throw new TransicionEstadoInvalidaException(suite.nombre + " aún no está preparado.");
        }
        reserva.estado = "EN_CURSO";
        suite.estado = "OCUPADO";
    }

    @Override
    public void registrarCheckOut(String codigoReserva) {
        Reserva reserva = reserva(codigoReserva);
        if (!"EN_CURSO".equals(reserva.estado)) {
            throw new TransicionEstadoInvalidaException("Solo se puede hacer check-out de una estancia en curso.");
        }
        reserva.estado = "FINALIZADA";
        suite(reserva.codigoSuite).estado = "PENDIENTE_PREPARACION";
        colaPreparacion.add(new Orden(reserva.codigoSuite, LocalDateTime.now()));
    }

    // ---------- Canal externo ----------

    @Override
    public List<SolicitudExternaDTO> colaCanalExterno() {
        return List.copyOf(colaExterna);
    }

    @Override
    public String procesarSiguienteSolicitudExterna() {
        SolicitudExternaDTO solicitud = colaExterna.pollFirst();
        if (solicitud == null) {
            throw new EstructuraVaciaException("No hay solicitudes externas en espera.");
        }
        Suite suite = suite(solicitud.codigoApartaSuite());
        if (!estaDisponible(suite.codigo, solicitud.entrada(), solicitud.salida())) {
            conflictos.add(solicitud.id());
            return "Conflicto de canal: " + suite.nombre + " ya estaba reservado. La solicitud "
                    + solicitud.id() + " quedó registrada como conflicto.";
        }
        List<OcupanteDTO> acompanantes = new ArrayList<>();
        for (int i = 2; i <= solicitud.personas(); i++) {
            acompanantes.add(new OcupanteDTO("Acompañante " + i, null, null));
        }
        HuespedDTO titular = huespedes.getOrDefault(solicitud.documentoHuesped(),
                new HuespedDTO(solicitud.documentoHuesped(), solicitud.nombreHuesped(), null, null, null));
        ReservaDTO reserva = crearReserva(suite.codigo, solicitud.entrada(), solicitud.salida(), titular,
                acompanantes, "EXTERNA", null);
        return "Solicitud " + solicitud.id() + " convertida en la reserva " + reserva.codigo() + ".";
    }

    // ---------- Folio ----------

    @Override
    public List<MovimientoFolioDTO> movimientosFolio(String codigoReserva) {
        return List.copyOf(reserva(codigoReserva).movimientos);
    }

    @Override
    public void registrarCargo(String codigoReserva, String concepto, double monto) {
        validarMonto(monto);
        movimiento(reservaConFolioAbierto(codigoReserva), "CARGO", concepto, monto, true);
    }

    @Override
    public void registrarPago(String codigoReserva, String concepto, double monto) {
        validarMonto(monto);
        movimiento(reservaConFolioAbierto(codigoReserva), "PAGO", concepto, monto, true);
    }

    @Override
    public MovimientoFolioDTO deshacerUltimaOperacion(String codigoReserva) {
        Reserva reserva = reservaConFolioAbierto(codigoReserva);
        MovimientoFolioDTO ultimo = reserva.deshacer.pop(); // EstructuraVaciaException si no hay qué deshacer
        String tipo = "CARGO".equals(ultimo.tipo()) ? "ANULACION_CARGO" : "ANULACION_PAGO";
        MovimientoFolioDTO contrario = new MovimientoFolioDTO(tipo, "Anula: " + ultimo.concepto(),
                ultimo.monto(), LocalDateTime.now());
        reserva.movimientos.add(contrario);
        return contrario;
    }

    // ---------- Preparación y turnos ----------

    @Override
    public List<OrdenPreparacionDTO> colaPreparacion() {
        PriorityQueue<OrdenPreparacionDTO> cola = new PriorityQueue<>(prioridadPreparacion());
        colaPreparacion.forEach(o -> cola.add(dto(o)));
        List<OrdenPreparacionDTO> ordenada = new ArrayList<>();
        while (!cola.isEmpty()) {
            ordenada.add(cola.poll());
        }
        return ordenada;
    }

    @Override
    public List<OrdenPreparacionDTO> enPreparacion() {
        return enPreparacion.entrySet().stream().map(e -> dto(new Orden(e.getKey(), e.getValue()))).toList();
    }

    @Override
    public OrdenPreparacionDTO despacharSiguientePreparacion() {
        List<OrdenPreparacionDTO> cola = colaPreparacion();
        if (cola.isEmpty()) {
            throw new EstructuraVaciaException("No hay aparta-suites esperando preparación.");
        }
        OrdenPreparacionDTO siguiente = cola.getFirst();
        colaPreparacion.removeIf(o -> o.codigoSuite().equals(siguiente.codigoApartaSuite()));
        enPreparacion.put(siguiente.codigoApartaSuite(), siguiente.ingresoCola());
        suite(siguiente.codigoApartaSuite()).estado = "EN_PREPARACION";
        return siguiente;
    }

    @Override
    public void terminarPreparacion(String codigoApartaSuite) {
        LocalDateTime ingreso = enPreparacion.remove(codigoApartaSuite);
        if (ingreso == null) {
            throw new TransicionEstadoInvalidaException(codigoApartaSuite + " no está en preparación.");
        }
        minutosPreparacion.add(Math.max(1, ChronoUnit.MINUTES.between(ingreso, LocalDateTime.now())));
        suite(codigoApartaSuite).estado = "PREPARADO";
    }

    @Override
    public RutaDTO rutaPreparacion(String codigoApartaSuite) {
        return dijkstra(NODO_ASEO, codigoApartaSuite);
    }

    @Override
    public EmpleadoDTO turnoActual() {
        return rotacion.getActual();
    }

    @Override
    public List<EmpleadoDTO> ordenTurnos() {
        List<EmpleadoDTO> orden = new ArrayList<>();
        rotacion.forEach(orden::add); // el iterador recorre la lista circular una vuelta desde el inicio
        return orden;
    }

    @Override
    public EmpleadoDTO avanzarTurno() {
        rotacion.avanzarTurno();
        return rotacion.getActual();
    }

    @Override
    public void reiniciarTurnos() {
        rotacion.reiniciarTurno();
    }

    /** Todo recepcionista nuevo entra al final de la rotación. */
    private void agregarEmpleado(EmpleadoDTO empleado) {
        empleados.add(empleado);
        rotacion.agregar(empleado);
    }

    // ---------- Administración ----------

    @Override
    public ReporteDTO generarReporte() {
        Map<String, Integer> porCanal = new LinkedHashMap<>();
        for (String canal : List.of("PORTAL", "DIRECTA", "EXTERNA")) {
            porCanal.put(canal, (int) reservas.values().stream().filter(r -> r.canal.equals(canal)).count());
        }
        Map<String, Integer> porEstado = new LinkedHashMap<>();
        for (String estado : List.of("PREPARADO", "OCUPADO", "PENDIENTE_PREPARACION", "EN_PREPARACION",
                "FUERA_DE_SERVICIO")) {
            porEstado.put(estado, (int) suites.values().stream().filter(s -> s.estado.equals(estado)).count());
        }
        int ocupados = porEstado.get("OCUPADO");
        int operativos = suites.size() - porEstado.get("FUERA_DE_SERVICIO");
        double ingresos = reservas.values().stream().mapToDouble(this::pagado).sum();
        double promedio = minutosPreparacion.stream().mapToLong(Long::longValue).average().orElse(0);
        return new ReporteDTO(suites.size(), ocupados, operativos == 0 ? 0 : ocupados * 100.0 / operativos,
                ingresos, porCanal, porEstado, conflictos.size(), promedio);
    }

    @Override
    public List<NodoEdificioDTO> nodosEdificio() {
        return List.copyOf(nodos.values());
    }

    @Override
    public List<ConexionDTO> conexionesEdificio() {
        return List.copyOf(conexiones);
    }

    @Override
    public void alternarPasillo(String origen, String destino) {
        for (int i = 0; i < conexiones.size(); i++) {
            ConexionDTO c = conexiones.get(i);
            if (c.origen().equals(origen) && c.destino().equals(destino)) {
                conexiones.set(i, new ConexionDTO(origen, destino, c.metros(), !c.habilitada()));
                return;
            }
        }
        throw new IllegalArgumentException("No existe el pasillo " + origen + " – " + destino + ".");
    }

    @Override
    public List<String> apartaSuitesInalcanzables() {
        Set<String> visitados = new HashSet<>();
        Deque<String> cola = new ArrayDeque<>(List.of(NODO_RECEPCION));
        visitados.add(NODO_RECEPCION);
        while (!cola.isEmpty()) {
            String actual = cola.poll();
            for (String vecino : vecinos(actual).keySet()) {
                if (visitados.add(vecino)) {
                    cola.add(vecino);
                }
            }
        }
        return nodos.values().stream().filter(n -> "APARTAMENTO".equals(n.tipo()) && !visitados.contains(n.codigo()))
                .map(NodoEdificioDTO::codigo).toList();
    }

    @Override
    public void guardarApartaSuite(ApartaSuiteDTO a) {
        Suite suite = suites.computeIfAbsent(a.codigo(), c -> new Suite());
        boolean nuevo = suite.codigo == null;
        suite.codigo = a.codigo();
        suite.nombre = a.nombre();
        suite.bloque = a.bloque();
        suite.piso = a.piso();
        suite.dormitorios = a.dormitorios();
        suite.capacidad = a.capacidad();
        suite.baja = a.tarifaBaja();
        suite.media = a.tarifaMedia();
        suite.alta = a.tarifaAlta();
        suite.caracteristicas = a.caracteristicas();
        suite.descripcion = a.descripcion();
        suite.estado = nuevo ? "PREPARADO" : a.estadoOperativo();
    }

    @Override
    public List<EmpleadoDTO> listarEmpleados() {
        return empleados.stream().sorted(Comparator.comparing(EmpleadoDTO::documento)).toList();
    }

    @Override
    public void registrarEmpleado(EmpleadoDTO empleado) {
        if (empleados.stream().anyMatch(e -> e.documento().equals(empleado.documento()))) {
            throw new IllegalArgumentException("Ya existe un empleado con el documento " + empleado.documento() + ".");
        }
        agregarEmpleado(empleado);
    }

    @Override
    public List<TemporadaDTO> listarTemporadas() {
        return temporadas.stream().sorted(Comparator.comparing(TemporadaDTO::inicio)).toList();
    }

    @Override
    public void agregarTemporada(TemporadaDTO temporada) {
        if (!temporada.fin().isAfter(temporada.inicio())) {
            throw new FechasInvalidasException("La fecha final debe ser posterior a la inicial.");
        }
        boolean cruza = temporadas.stream().anyMatch(t -> !temporada.fin().isBefore(t.inicio())
                && !temporada.inicio().isAfter(t.fin()));
        if (cruza) {
            throw new FechasInvalidasException("La temporada se cruza con otra ya definida.");
        }
        temporadas.add(temporada);
    }

    @Override
    public void actualizarTarifas(String codigoApartaSuite, double baja, double media, double alta) {
        if (baja <= 0 || media <= 0 || alta <= 0) {
            throw new IllegalArgumentException("Las tarifas deben ser mayores que cero.");
        }
        Suite suite = suite(codigoApartaSuite);
        suite.baja = baja;
        suite.media = media;
        suite.alta = alta;
    }

    // ---------- Apoyo ----------

    private Suite suite(String codigo) {
        Suite suite = suites.get(codigo);
        if (suite == null) {
            throw new IllegalArgumentException("No existe el aparta-suite " + codigo + ".");
        }
        return suite;
    }

    private Reserva reserva(String codigo) {
        Reserva reserva = reservas.get(codigo);
        if (reserva == null) {
            throw new IllegalArgumentException("No existe la reserva " + codigo + ".");
        }
        return reserva;
    }

    private Reserva reservaConFolioAbierto(String codigo) {
        Reserva reserva = reserva(codigo);
        if (!"CONFIRMADA".equals(reserva.estado) && !"EN_CURSO".equals(reserva.estado)) {
            throw new TransicionEstadoInvalidaException("El folio de " + codigo + " está cerrado.");
        }
        return reserva;
    }

    private static void validarFechas(LocalDate entrada, LocalDate salida, boolean exigirFuturo) {
        if (entrada == null || salida == null) {
            throw new FechasInvalidasException("Indica la fecha de entrada y la de salida.");
        }
        if (!salida.isAfter(entrada)) {
            throw new FechasInvalidasException("La salida debe ser posterior a la entrada.");
        }
        if (exigirFuturo && entrada.isBefore(LocalDate.now())) {
            throw new FechasInvalidasException("La fecha de entrada no puede estar en el pasado.");
        }
    }

    private static void validarMonto(double monto) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero.");
        }
    }

    private static boolean bloquea(Reserva r) {
        return r.estado.equals("PENDIENTE") || r.estado.equals("CONFIRMADA") || r.estado.equals("EN_CURSO");
    }

    private static boolean seSolapan(LocalDate e1, LocalDate s1, LocalDate e2, LocalDate s2) {
        return e1.isBefore(s2) && e2.isBefore(s1);
    }

    private static String normalizar(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private void movimiento(Reserva reserva, String tipo, String concepto, double monto, boolean deshacible) {
        MovimientoFolioDTO m = new MovimientoFolioDTO(tipo, concepto, monto, LocalDateTime.now());
        reserva.movimientos.add(m);
        if (deshacible) {
            reserva.deshacer.push(m);
        }
    }

    private double pagado(Reserva reserva) {
        return reserva.movimientos.stream().mapToDouble(m -> switch (m.tipo()) {
            case "PAGO" -> m.monto();
            case "ANULACION_PAGO" -> -m.monto();
            default -> 0;
        }).sum();
    }

    private ReservaDTO dto(Reserva r) {
        double saldo = r.movimientos.stream().mapToDouble(MovimientoFolioDTO::efectoEnSaldo).sum();
        HuespedDTO titular = huespedes.get(r.documentoTitular);
        return new ReservaDTO(r.codigo, r.documentoTitular, titular == null ? r.documentoTitular : titular.nombre(),
                r.codigoSuite, suites.get(r.codigoSuite).nombre, r.entrada, r.salida, r.ocupantes.size(), r.canal,
                r.estado, r.horaLlegada, r.total, Math.round(r.total * PORCENTAJE_ANTICIPO), pagado(r),
                "CANCELADA".equals(r.estado) ? 0 : saldo);
    }

    private OrdenPreparacionDTO dto(Orden orden) {
        LocalDate hoy = LocalDate.now();
        Optional<Reserva> llegada = reservas.values().stream()
                .filter(r -> r.codigoSuite.equals(orden.codigoSuite()) && r.entrada.equals(hoy)
                        && ("CONFIRMADA".equals(r.estado) || "PENDIENTE".equals(r.estado)))
                .findFirst();
        return new OrdenPreparacionDTO(orden.codigoSuite(), suites.get(orden.codigoSuite()).nombre,
                llegada.isPresent(), llegada.map(r -> r.horaLlegada).orElse(null), orden.ingreso());
    }

    private static Comparator<OrdenPreparacionDTO> prioridadPreparacion() {
        return Comparator.comparing((OrdenPreparacionDTO o) -> !o.entradaHoy())
                .thenComparing(OrdenPreparacionDTO::horaLlegadaSiguiente, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(OrdenPreparacionDTO::ingresoCola);
    }

    @Override
    public RutaDTO rutaRecorridoPreparacion() {
        List<String> pasos = new ArrayList<>();
        double metros = 0;
        String actual = NODO_ASEO;
        for (OrdenPreparacionDTO orden : colaPreparacion()) {
            RutaDTO tramo = dijkstra(actual, orden.codigoApartaSuite());
            // el primer paso del tramo es el nodo donde ya estamos
            pasos.addAll(pasos.isEmpty() ? tramo.pasos() : tramo.pasos().subList(1, tramo.pasos().size()));
            metros += tramo.metros();
            actual = orden.codigoApartaSuite();
        }
        if (pasos.isEmpty()) {
            pasos.add(nodos.get(NODO_ASEO).nombre());
        }
        return new RutaDTO(pasos, metros);
    }

    private Map<String, Double> vecinos(String nodo) {
        Map<String, Double> vecinos = new HashMap<>();
        for (ConexionDTO c : conexiones) {
            if (!c.habilitada()) {
                continue;
            }
            if (c.origen().equals(nodo)) {
                vecinos.put(c.destino(), c.metros());
            } else if (c.destino().equals(nodo)) {
                vecinos.put(c.origen(), c.metros());
            }
        }
        return vecinos;
    }

    private RutaDTO dijkstra(String origen, String destino) {
        Map<String, Double> distancia = new HashMap<>();
        Map<String, String> previo = new HashMap<>();
        PriorityQueue<Map.Entry<String, Double>> cola = new PriorityQueue<>(Map.Entry.comparingByValue());
        distancia.put(origen, 0.0);
        cola.add(Map.entry(origen, 0.0));
        while (!cola.isEmpty()) {
            Map.Entry<String, Double> actual = cola.poll();
            if (actual.getValue() > distancia.getOrDefault(actual.getKey(), Double.MAX_VALUE)) {
                continue;
            }
            for (Map.Entry<String, Double> vecino : vecinos(actual.getKey()).entrySet()) {
                double nueva = actual.getValue() + vecino.getValue();
                if (nueva < distancia.getOrDefault(vecino.getKey(), Double.MAX_VALUE)) {
                    distancia.put(vecino.getKey(), nueva);
                    previo.put(vecino.getKey(), actual.getKey());
                    cola.add(Map.entry(vecino.getKey(), nueva));
                }
            }
        }
        if (!distancia.containsKey(destino)) {
            throw new IllegalStateException("No hay ruta hasta " + destino + " con los pasillos abiertos.");
        }
        List<String> pasos = new ArrayList<>();
        for (String n = destino; n != null; n = previo.get(n)) {
            pasos.addFirst(nodos.get(n).nombre());
        }
        return new RutaDTO(pasos, distancia.get(destino));
    }

    // ---------- Datos de ejemplo ----------

    private void sembrarDatos() {
        suite("A-101", "Suite Cocora", "A", 1, 1, 2, 85_000, 98_000, 125_000,
                "Vista al valle de Cocora, cama queen y cocineta equipada.",
                "Vista al valle", "Cama queen", "Cocineta", "Wi-Fi", "Agua caliente");
        suite("A-102", "Suite Palma de Cera", "A", 1, 2, 4, 78_000, 90_000, 115_000,
                "Dos habitaciones con baño privado y sala con chimenea.",
                "Chimenea", "2 baños", "Cocina completa", "Wi-Fi");
        suite("A-201", "Suite Barbas", "A", 2, 2, 5, 72_000, 84_000, 108_000,
                "Ideal para familias: litera para niños y balcón amplio.",
                "Balcón", "Litera", "Cocina completa", "Wi-Fi", "Parqueadero");
        suite("A-202", "Suite Mirador", "A", 2, 1, 3, 90_000, 104_000, 132_000,
                "Ventanal panorámico hacia las montañas y bañera.",
                "Ventanal panorámico", "Bañera", "Cama king", "Wi-Fi");
        suite("B-101", "Suite Bosque de Niebla", "B", 1, 2, 4, 75_000, 88_000, 110_000,
                "Rodeada de jardín, con terraza privada y hamaca.",
                "Terraza privada", "Hamaca", "Cocineta", "Wi-Fi");
        suite("B-102", "Suite Quindío", "B", 1, 3, 6, 68_000, 80_000, 102_000,
                "La más amplia del bloque B, pensada para grupos.",
                "3 habitaciones", "2 baños", "Comedor para 6", "Parqueadero", "Wi-Fi");
        suite("B-201", "Suite Cafetal", "B", 2, 1, 2, 82_000, 95_000, 120_000,
                "Decoración con maderas locales y vista a los cafetales.",
                "Vista a cafetales", "Cama queen", "Cafetera", "Wi-Fi");
        suite("B-202", "Suite Arriero", "B", 2, 2, 4, 76_000, 89_000, 112_000,
                "Estilo tradicional paisa con balcón de madera.",
                "Balcón", "Cocina completa", "Wi-Fi");
        suite("C-101", "Suite Guadua", "C", 1, 3, 7, 65_000, 78_000, 99_000,
                "Construida en guadua, con zona de BBQ y jardín propio.",
                "Zona BBQ", "Jardín", "3 habitaciones", "Parqueadero", "Wi-Fi");
        suite("C-201", "Suite Colibrí", "C", 2, 1, 2, 88_000, 101_000, 128_000,
                "Pequeña y luminosa, perfecta para parejas.",
                "Jacuzzi", "Cama queen", "Desayuno incluido", "Wi-Fi");
        estado("A-202", "PENDIENTE_PREPARACION");
        estado("B-101", "OCUPADO");
        estado("B-202", "FUERA_DE_SERVICIO");
        estado("C-101", "PENDIENTE_PREPARACION");

        LocalDate hoy = LocalDate.now();
        temporadas.add(new TemporadaDTO("MEDIA", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30)));
        temporadas.add(new TemporadaDTO("ALTA", LocalDate.of(2026, 12, 15), LocalDate.of(2027, 1, 15)));
        temporadas.add(new TemporadaDTO("MEDIA", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 15)));
        temporadas.add(new TemporadaDTO("ALTA", LocalDate.of(2027, 3, 20), LocalDate.of(2027, 4, 5)));
        temporadas.add(new TemporadaDTO("ALTA", LocalDate.of(2027, 6, 15), LocalDate.of(2027, 7, 31)));

        agregarEmpleado(new EmpleadoDTO("41900001", "Laura Ríos", "06:00 – 14:00"));
        agregarEmpleado(new EmpleadoDTO("41900002", "Andrés Castaño", "14:00 – 22:00"));
        agregarEmpleado(new EmpleadoDTO("41900003", "Sofía Marín", "22:00 – 06:00"));

        HuespedDTO ana = huesped("1094000001", "Ana María Gómez", LocalDate.of(1995, 3, 12),
                "300 123 4567", "huesped@demo.co");
        huesped("1094000002", "Juan Pablo Pérez", LocalDate.of(1988, 7, 2), "311 222 3344", "juan.perez@correo.com");
        huesped("1094000003", "María José López", LocalDate.of(1992, 11, 20), "320 555 1212", "mjlopez@correo.com");
        huesped("1094000004", "Pedro Antonio Ruiz", LocalDate.of(1979, 1, 5), "315 777 8899", "pedro.ruiz@correo.com");

        cuentas.put("huesped@demo.co", new Cuenta(CLAVE_DEMO,
                new UsuarioDTO("huesped@demo.co", ana.nombre(), Rol.HUESPED, ana.documento())));
        cuentas.put("recepcion@demo.co", new Cuenta(CLAVE_DEMO,
                new UsuarioDTO("recepcion@demo.co", "Laura Ríos", Rol.RECEPCIONISTA, "41900001")));
        cuentas.put("admin@demo.co", new Cuenta(CLAVE_DEMO,
                new UsuarioDTO("admin@demo.co", "Carlos Mejía", Rol.ADMINISTRADOR, "41900099")));
        favoritos.put(ana.documento(), new LinkedHashSet<>(List.of("A-202", "C-201")));

        reservaSemilla("1094000001", "A-101", hoy.plusDays(20), hoy.plusDays(23), "PORTAL", "PENDIENTE", 1, null);
        reservaSemilla("1094000001", "B-201", hoy.minusDays(30), hoy.minusDays(27), "PORTAL", "FINALIZADA", 1, null);
        reservaSemilla("1094000002", "A-201", hoy, hoy.plusDays(3), "DIRECTA", "CONFIRMADA", 3, LocalTime.of(15, 0));
        Reserva enCurso = reservaSemilla("1094000003", "B-101", hoy.minusDays(2), hoy.plusDays(1), "DIRECTA",
                "EN_CURSO", 2, null);
        movimiento(enCurso, "PAGO", "Pago en recepción", 200_000, true);
        movimiento(enCurso, "CARGO", "Restaurante · cena", 86_000, true);
        reservaSemilla("1094000004", "C-101", hoy, hoy.plusDays(2), "EXTERNA", "CONFIRMADA", 5, LocalTime.of(16, 30));

        colaPreparacion.add(new Orden("A-202", LocalDateTime.now().minusHours(3)));
        colaPreparacion.add(new Orden("C-101", LocalDateTime.now().minusHours(1)));
        minutosPreparacion.addAll(List.of(42L, 55L, 38L, 47L));

        colaExterna.add(new SolicitudExternaDTO("EXT-01", "Lucía Fernández", "52800011", "A-101",
                hoy.plusDays(21), hoy.plusDays(24), 2, LocalDateTime.now().minusMinutes(50)));
        colaExterna.add(new SolicitudExternaDTO("EXT-02", "Tomás Herrera", "79100022", "B-102",
                hoy.plusDays(5), hoy.plusDays(8), 4, LocalDateTime.now().minusMinutes(20)));

        sembrarEdificio();
    }

    private void suite(String codigo, String nombre, String bloque, int piso, int dormitorios, int capacidad,
                       double baja, double media, double alta, String descripcion, String... caracteristicas) {
        Suite s = new Suite();
        s.codigo = codigo;
        s.nombre = nombre;
        s.bloque = bloque;
        s.piso = piso;
        s.dormitorios = dormitorios;
        s.capacidad = capacidad;
        s.baja = baja;
        s.media = media;
        s.alta = alta;
        s.descripcion = descripcion;
        s.caracteristicas = List.of(caracteristicas);
        s.estado = "PREPARADO";
        suites.put(codigo, s);
    }

    private void estado(String codigo, String estado) {
        suites.get(codigo).estado = estado;
    }

    private HuespedDTO huesped(String documento, String nombre, LocalDate nacimiento, String telefono, String correo) {
        HuespedDTO h = new HuespedDTO(documento, nombre, nacimiento, telefono, correo);
        huespedes.put(documento, h);
        return h;
    }

    private Reserva reservaSemilla(String documento, String suite, LocalDate entrada, LocalDate salida, String canal,
                                   String estado, int personas, LocalTime hora) {
        Reserva r = new Reserva();
        r.codigo = "R-" + (++consecutivoReserva);
        r.documentoTitular = documento;
        r.codigoSuite = suite;
        r.entrada = entrada;
        r.salida = salida;
        r.canal = canal;
        r.estado = estado;
        r.horaLlegada = hora;
        HuespedDTO titular = huespedes.get(documento);
        r.ocupantes.add(new OcupanteDTO(titular.nombre(), documento, titular.fechaNacimiento()));
        for (int i = 2; i <= personas; i++) {
            r.ocupantes.add(new OcupanteDTO("Acompañante " + i, null, null));
        }
        r.total = cotizar(suite, entrada, salida, personas).total();
        movimiento(r, "CARGO", "Alojamiento · " + personas + " personas", r.total, false);
        if ("FINALIZADA".equals(estado)) {
            movimiento(r, "PAGO", "Pago total", r.total, false);
        }
        reservas.put(r.codigo, r);
        return r;
    }

    private void sembrarEdificio() {
        nodo(NODO_RECEPCION, "Recepción", "RECEPCION", "-", 70, 210);
        nodo(NODO_ASEO, "Cuarto de aseo", "CUARTO_ASEO", "-", 70, 340);
        String[] bloques = {"A", "B", "C"};
        double[] y = {80, 210, 340};
        for (int i = 0; i < bloques.length; i++) {
            String b = bloques[i];
            nodo("P-" + b, "Pasillo " + b, "PASILLO", b, 230, y[i]);
            nodo("ESC-" + b, "Escalera " + b, "ESCALERA", b, 470, y[i]);
            for (Suite s : suites.values()) {
                if (s.bloque.equals(b)) {
                    boolean primero = s.codigo.endsWith("01");
                    double x = s.piso == 1 ? 350 : 590;
                    nodo(s.codigo, s.codigo + " · " + s.nombre, "APARTAMENTO", b, x, y[i] + (primero ? -38 : 38));
                    conexion(s.piso == 1 ? "P-" + b : "ESC-" + b, s.codigo, primero ? 10 : 14);
                }
            }
            conexion(NODO_RECEPCION, "P-" + b, i == 1 ? 15 : 25);
            conexion("P-" + b, "ESC-" + b, 18);
        }
        conexion(NODO_RECEPCION, NODO_ASEO, 12);
        conexion("P-A", "P-B", 20);
        conexion("P-B", "P-C", 20);
    }

    private void nodo(String codigo, String nombre, String tipo, String bloque, double x, double y) {
        nodos.put(codigo, new NodoEdificioDTO(codigo, nombre, tipo, bloque, x, y));
    }

    private void conexion(String origen, String destino, double metros) {
        conexiones.add(new ConexionDTO(origen, destino, metros, true));
    }
}
