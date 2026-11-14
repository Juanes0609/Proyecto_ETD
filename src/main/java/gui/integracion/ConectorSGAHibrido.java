package gui.integracion;

import estructuras.Pila;
import excepciones.CapacidadExcedidaException;
import excepciones.EstructuraVaciaException;
import excepciones.FechasInvalidasException;
import excepciones.ReservaSolapadaException;
import excepciones.TransicionEstadoInvalidaException;
import gui.integracion.AlmacenSGA.Cuenta;
import gui.integracion.AlmacenSGA.Instantanea;
import gui.integracion.AlmacenSGA.Orden;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.ConexionDTO;
import gui.integracion.dto.CotizacionDTO;
import gui.integracion.dto.EmpleadoDTO;
import gui.integracion.dto.HuespedDTO;
import gui.integracion.dto.MovimientoFolioDTO;
import gui.integracion.dto.NocheDTO;
import gui.integracion.dto.OcupanteDTO;
import gui.integracion.dto.OrdenPreparacionDTO;
import gui.integracion.dto.ReporteDTO;
import gui.integracion.dto.ReservaDTO;
import gui.integracion.dto.Rol;
import gui.integracion.dto.SolicitudExternaDTO;
import gui.integracion.dto.TemporadaDTO;
import gui.integracion.dto.UsuarioDTO;
import modelo.Apartamento;
import modelo.EstadoOperativo;
import modelo.Huesped;
import modelo.OrdenPreparacion;
import modelo.SistemaGestionAlojamiento;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Conector que usa la aplicación.
 * <p>
 * Lo que el núcleo del equipo ya implementa (catálogo, tarifas, huéspedes,
 * favoritos, mapa, preparación y turnos) lo resuelve {@link ConectorSGAProveedor}
 * sobre {@code SistemaGestionAlojamiento}. Lo que aún no existe en el núcleo
 * (sesión, temporadas, reservas, folios, canal externo y reporte) se cubre aquí
 * en memoria, leyendo los aparta-suites y huéspedes del modelo real para que
 * todo cuadre. Cuando el equipo implemente una de esas funciones, basta con
 * borrar aquí el método para que se use la del núcleo.
 * <p>
 * Todo cambio se guarda en disco ({@link AlmacenSGA}); la primera vez, o al
 * pulsar «Cargar escenario», se parte de {@link EscenarioInicialSGA}.
 */
public class ConectorSGAHibrido extends ConectorSGAProveedor {

    private static final double PORCENTAJE_ANTICIPO = 0.30;

    /** Reserva simulada mientras el núcleo no gestiona reservas. */
    static final class Reserva implements Serializable {
        String codigo, documentoTitular, codigoSuite, canal, estado;
        LocalDate entrada, salida;
        LocalTime horaLlegada;
        final List<OcupanteDTO> ocupantes = new ArrayList<>();
        /** Negativo mientras no se haya calculado (reservas del escenario inicial). */
        double total = -1;
        final List<MovimientoFolioDTO> movimientos = new ArrayList<>();
        /** Copia guardable de la pila de deshacer (la {@link Pila} del proyecto no es serializable). */
        final List<MovimientoFolioDTO> deshacibles = new ArrayList<>();
        private transient Pila<MovimientoFolioDTO> deshacer;

        Pila<MovimientoFolioDTO> pilaDeshacer() {
            if (deshacer == null) {
                deshacer = new Pila<>();
                deshacibles.forEach(deshacer::push);
            }
            return deshacer;
        }
    }

    private final Path archivo;
    private final Map<String, Cuenta> cuentas = new LinkedHashMap<>();
    private final Map<String, String> descripciones = new HashMap<>();
    private final Map<String, Reserva> reservas = new LinkedHashMap<>();
    private final List<TemporadaDTO> temporadas = new ArrayList<>();
    private final List<EmpleadoDTO> empleados = new ArrayList<>();
    private final Deque<SolicitudExternaDTO> colaExterna = new ArrayDeque<>();
    private final List<String> conflictos = new ArrayList<>();
    private final List<Long> minutosPreparacion = new ArrayList<>();
    private int consecutivoReserva;

    public ConectorSGAHibrido() {
        this(AlmacenSGA.ARCHIVO);
    }

    /** @param archivo dónde se guardan los datos (las pruebas usan un archivo temporal) */
    public ConectorSGAHibrido(Path archivo) {
        super(new SistemaGestionAlojamiento());
        this.archivo = archivo;
        Instantanea guardada = AlmacenSGA.cargar(archivo);
        if (guardada != null) {
            try {
                importar(guardada);
                return;
            } catch (RuntimeException e) {
                // datos inconsistentes con el modelo actual: se parte del escenario inicial
            }
        }
        cargarEscenarioInicial();
    }

    @Override
    public boolean tieneCuentasDePrueba() {
        return true;
    }

    /** Borra lo guardado y vuelve a los datos de prueba. */
    @Override
    public final void cargarEscenarioInicial() {
        importar(EscenarioInicialSGA.crear());
        guardar();
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
        if (cuentas.values().stream().anyMatch(c -> datos.documento().equals(c.usuario().documento()))) {
            throw new IllegalArgumentException("Ese documento ya tiene una cuenta registrada.");
        }
        if (getSistema().buscarHuesped(datos.documento()) == null) {
            super.registrarHuesped(datos);
        } else {
            super.actualizarHuesped(datos);
        }
        UsuarioDTO usuario = new UsuarioDTO(correo, datos.nombre(), Rol.HUESPED, datos.documento());
        cuentas.put(correo, new Cuenta(clave, usuario));
        guardar();
        return usuario;
    }

    // ---------- Catálogo y tarifas (modelo real + guardado) ----------

    @Override
    protected String descripcionDe(String codigoApartaSuite) {
        return descripciones.getOrDefault(codigoApartaSuite, "");
    }

    /** Tras guardar se rearma el sistema para que el mapa incluya el aparta-suite con sus datos nuevos. */
    @Override
    public void guardarApartaSuite(ApartaSuiteDTO apartaSuite) {
        super.guardarApartaSuite(apartaSuite);
        descripciones.put(apartaSuite.codigo(), apartaSuite.descripcion() == null ? "" : apartaSuite.descripcion());
        importar(exportar());
        guardar();
    }

    @Override
    public void actualizarTarifas(String codigoApartaSuite, double baja, double media, double alta) {
        if (baja <= 0 || media <= 0 || alta <= 0) {
            throw new IllegalArgumentException("Las tarifas deben ser mayores que cero.");
        }
        super.actualizarTarifas(codigoApartaSuite, baja, media, alta);
        guardar();
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
        guardar();
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
        ApartaSuiteDTO suite = aDto(apartamento(codigoApartaSuite));
        List<NocheDTO> noches = new ArrayList<>();
        double total = 0;
        for (LocalDate noche = entrada; noche.isBefore(salida); noche = noche.plusDays(1)) {
            String temporada = temporadaEn(noche);
            double tarifa = suite.tarifa(temporada);
            double subtotal = tarifa * personasQuePagan;
            noches.add(new NocheDTO(noche, temporada, personasQuePagan, tarifa, subtotal));
            total += subtotal;
        }
        return new CotizacionDTO(noches, personasQuePagan, total);
    }

    // ---------- Huéspedes y favoritos (modelo real + guardado) ----------

    @Override
    public void registrarHuesped(HuespedDTO huesped) {
        super.registrarHuesped(huesped);
        guardar();
    }

    @Override
    public void actualizarHuesped(HuespedDTO huesped) {
        super.actualizarHuesped(huesped);
        cuentas.replaceAll((correo, cuenta) -> huesped.documento().equals(cuenta.usuario().documento())
                ? new Cuenta(cuenta.clave(), new UsuarioDTO(correo, huesped.nombre(), cuenta.usuario().rol(),
                huesped.documento()))
                : cuenta);
        guardar();
    }

    @Override
    public boolean alternarFavorito(String documentoHuesped, String codigoApartaSuite) {
        boolean favorito = super.alternarFavorito(documentoHuesped, codigoApartaSuite);
        guardar();
        return favorito;
    }

    // ---------- Reservas ----------

    @Override
    public List<ReservaDTO> reservasDeHuesped(String documentoHuesped) {
        return reservas.values().stream().filter(r -> r.documentoTitular.equals(documentoHuesped))
                .sorted(Comparator.comparing((Reserva r) -> r.entrada).reversed()).map(this::dto).toList();
    }

    @Override
    public ReservaDTO crearReserva(String codigoApartaSuite, LocalDate entrada, LocalDate salida,
                                   HuespedDTO titular, List<OcupanteDTO> acompanantes,
                                   String canal, LocalTime horaLlegada) {
        Reserva reserva = nuevaReserva(codigoApartaSuite, entrada, salida, titular, acompanantes, canal, horaLlegada);
        guardar();
        return dto(reserva);
    }

    private Reserva nuevaReserva(String codigoApartaSuite, LocalDate entrada, LocalDate salida,
                                 HuespedDTO titular, List<OcupanteDTO> acompanantes,
                                 String canal, LocalTime horaLlegada) {
        validarFechas(entrada, salida, true);
        ApartaSuiteDTO suite = aDto(apartamento(codigoApartaSuite));
        if (!suite.reservable()) {
            throw new TransicionEstadoInvalidaException(suite.nombre() + " está fuera de servicio.");
        }
        int personas = 1 + acompanantes.size();
        if (personas > suite.capacidad()) {
            throw new CapacidadExcedidaException(suite.nombre() + " admite hasta " + suite.capacidad()
                    + " personas y la reserva tiene " + personas + ".");
        }
        if (!estaDisponible(codigoApartaSuite, entrada, salida)) {
            throw new ReservaSolapadaException(suite.nombre() + " ya está reservado en parte de esas fechas.");
        }
        if (getSistema().buscarHuesped(titular.documento()) == null) {
            super.registrarHuesped(titular);
        }

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
        return reserva;
    }

    @Override
    public void pagarAnticipo(String codigoReserva) {
        Reserva reserva = reserva(codigoReserva);
        if (!"PENDIENTE".equals(reserva.estado)) {
            throw new TransicionEstadoInvalidaException("La reserva " + codigoReserva + " no está pendiente de anticipo.");
        }
        movimiento(reserva, "PAGO", "Anticipo por el portal", Math.round(reserva.total * PORCENTAJE_ANTICIPO), false);
        reserva.estado = "CONFIRMADA";
        guardar();
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
        guardar();
    }

    @Override
    public List<ReservaDTO> reservasActivas() {
        return reservas.values().stream().map(this::dto).filter(ReservaDTO::activa)
                .sorted(Comparator.comparing(ReservaDTO::entrada)).toList();
    }

    /** El aparta-suite debe estar Preparado en el modelo y pasa a Ocupado. */
    @Override
    public void registrarCheckIn(String codigoReserva) {
        Reserva reserva = reserva(codigoReserva);
        Apartamento apartamento = apartamento(reserva.codigoSuite);
        if (!"CONFIRMADA".equals(reserva.estado)) {
            throw new TransicionEstadoInvalidaException("Solo se puede hacer check-in de una reserva confirmada.");
        }
        if (reserva.entrada.isAfter(LocalDate.now())) {
            throw new TransicionEstadoInvalidaException("La llegada de " + codigoReserva + " es el "
                    + reserva.entrada + ".");
        }
        if (apartamento.getEstadoOperativo() != EstadoOperativo.PREPARADO) {
            throw new TransicionEstadoInvalidaException(apartamento.getNombre() + " aún no está preparado.");
        }
        reserva.estado = "EN_CURSO";
        apartamento.setEstadoOperativo(EstadoOperativo.OCUPADO);
        guardar();
    }

    /** Al salir el huésped, el aparta-suite entra a la cola de prioridad de preparación del modelo. */
    @Override
    public void registrarCheckOut(String codigoReserva) {
        Reserva reserva = reserva(codigoReserva);
        if (!"EN_CURSO".equals(reserva.estado)) {
            throw new TransicionEstadoInvalidaException("Solo se puede hacer check-out de una estancia en curso.");
        }
        reserva.estado = "FINALIZADA";
        encolarPreparacion(reserva.codigoSuite);
        guardar();
    }

    // ---------- Canal externo ----------

    @Override
    public List<SolicitudExternaDTO> colaCanalExterno() {
        return List.copyOf(colaExterna);
    }

    @Override
    public String procesarSiguienteSolicitudExterna() {
        SolicitudExternaDTO solicitud = colaExterna.peekFirst();
        if (solicitud == null) {
            throw new EstructuraVaciaException("No hay solicitudes externas en espera.");
        }
        String nombreSuite = apartamento(solicitud.codigoApartaSuite()).getNombre();
        String mensaje;
        if (!estaDisponible(solicitud.codigoApartaSuite(), solicitud.entrada(), solicitud.salida())) {
            conflictos.add(solicitud.id());
            mensaje = "Conflicto de canal: " + nombreSuite + " ya estaba reservado. La solicitud "
                    + solicitud.id() + " quedó registrada como conflicto.";
        } else {
            List<OcupanteDTO> acompanantes = new ArrayList<>();
            for (int i = 2; i <= solicitud.personas(); i++) {
                acompanantes.add(new OcupanteDTO("Acompañante " + i, null, null));
            }
            Huesped existente = getSistema().buscarHuesped(solicitud.documentoHuesped());
            HuespedDTO titular = existente != null ? aDto(existente)
                    : new HuespedDTO(solicitud.documentoHuesped(), solicitud.nombreHuesped(), null, null, null);
            Reserva reserva = nuevaReserva(solicitud.codigoApartaSuite(), solicitud.entrada(), solicitud.salida(),
                    titular, acompanantes, "EXTERNA", null);
            mensaje = "Solicitud " + solicitud.id() + " convertida en la reserva " + reserva.codigo + ".";
        }
        colaExterna.pollFirst(); // sale de la cola solo si se procesó sin errores
        guardar();
        return mensaje;
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
        guardar();
    }

    @Override
    public void registrarPago(String codigoReserva, String concepto, double monto) {
        validarMonto(monto);
        movimiento(reservaConFolioAbierto(codigoReserva), "PAGO", concepto, monto, true);
        guardar();
    }

    /** Deshace con la {@link Pila} del proyecto. */
    @Override
    public MovimientoFolioDTO deshacerUltimaOperacion(String codigoReserva) {
        Reserva reserva = reservaConFolioAbierto(codigoReserva);
        MovimientoFolioDTO ultimo = reserva.pilaDeshacer().pop(); // EstructuraVaciaException si no hay qué deshacer
        reserva.deshacibles.removeLast();
        String tipo = "CARGO".equals(ultimo.tipo()) ? "ANULACION_CARGO" : "ANULACION_PAGO";
        MovimientoFolioDTO contrario = new MovimientoFolioDTO(tipo, "Anula: " + ultimo.concepto(),
                ultimo.monto(), LocalDateTime.now());
        reserva.movimientos.add(contrario);
        guardar();
        return contrario;
    }

    // ---------- Preparación (modelo real + guardado) ----------

    @Override
    public OrdenPreparacionDTO despacharSiguientePreparacion() {
        OrdenPreparacionDTO orden = super.despacharSiguientePreparacion();
        guardar();
        return orden;
    }

    @Override
    public void terminarPreparacion(String codigoApartaSuite) {
        LocalDateTime ingreso = null;
        for (OrdenPreparacion o : getSistema().getPreparacion().listarEnPreparacion()) {
            if (o.getCodigo().equals(codigoApartaSuite)) {
                ingreso = o.getFechaCreacion();
            }
        }
        super.terminarPreparacion(codigoApartaSuite);
        if (ingreso != null) {
            minutosPreparacion.add(Math.max(1, ChronoUnit.MINUTES.between(ingreso, LocalDateTime.now())));
        }
        guardar();
    }

    /** Encola el aparta-suite en la cola de prioridad del modelo (entrada hoy primero). */
    private OrdenPreparacion encolarPreparacion(String codigoApartaSuite) {
        Optional<Reserva> llegada = llegadaHoy(codigoApartaSuite);
        LocalDateTime hora = llegada.map(r -> r.horaLlegada).map(h -> LocalDate.now().atTime(h)).orElse(null);
        return getSistema().getPreparacion().encolar(apartamento(codigoApartaSuite), llegada.isPresent(), hora);
    }

    private Optional<Reserva> llegadaHoy(String codigoApartaSuite) {
        LocalDate hoy = LocalDate.now();
        return reservas.values().stream()
                .filter(r -> r.codigoSuite.equals(codigoApartaSuite) && r.entrada.equals(hoy)
                        && ("CONFIRMADA".equals(r.estado) || "PENDIENTE".equals(r.estado)))
                .findFirst();
    }

    // ---------- Turnos (lista circular del modelo + datos del empleado) ----------

    @Override
    public EmpleadoDTO turnoActual() {
        return empleado(getSistema().getTurnos().turnoActual());
    }

    @Override
    public List<EmpleadoDTO> ordenTurnos() {
        List<EmpleadoDTO> orden = new ArrayList<>();
        for (String nombre : getSistema().getTurnos().listarOrdenTurnos()) {
            orden.add(empleado(nombre));
        }
        return orden;
    }

    @Override
    public EmpleadoDTO avanzarTurno() {
        EmpleadoDTO siguiente = empleado(getSistema().getTurnos().avanzarTurno());
        guardar();
        return siguiente;
    }

    @Override
    public void reiniciarTurnos() {
        super.reiniciarTurnos();
        guardar();
    }

    @Override
    public List<EmpleadoDTO> listarEmpleados() {
        return empleados.stream().sorted(Comparator.comparing(EmpleadoDTO::documento)).toList();
    }

    /** Todo recepcionista nuevo entra al final de la rotación. */
    @Override
    public void registrarEmpleado(EmpleadoDTO empleado) {
        if (empleados.stream().anyMatch(e -> e.documento().equals(empleado.documento()))) {
            throw new IllegalArgumentException("Ya existe un empleado con el documento " + empleado.documento() + ".");
        }
        empleados.add(empleado);
        super.registrarEmpleado(empleado);
        guardar();
    }

    /** El modelo guarda solo el nombre; documento y horario salen de la lista de empleados. */
    private EmpleadoDTO empleado(String nombre) {
        return empleados.stream().filter(e -> e.nombre().equals(nombre)).findFirst()
                .orElse(new EmpleadoDTO("-", nombre, "-"));
    }

    // ---------- Mapa y reporte ----------

    @Override
    public void alternarPasillo(String origen, String destino) {
        super.alternarPasillo(origen, destino);
        guardar();
    }

    @Override
    public ReporteDTO generarReporte() {
        List<ApartaSuiteDTO> suites = listarApartaSuites();
        Map<String, Integer> porCanal = new LinkedHashMap<>();
        for (String canal : List.of("PORTAL", "DIRECTA", "EXTERNA")) {
            porCanal.put(canal, (int) reservas.values().stream().filter(r -> r.canal.equals(canal)).count());
        }
        Map<String, Integer> porEstado = new LinkedHashMap<>();
        for (String estado : List.of("PREPARADO", "OCUPADO", "PENDIENTE_PREPARACION", "EN_PREPARACION",
                "FUERA_DE_SERVICIO")) {
            porEstado.put(estado, (int) suites.stream().filter(s -> s.estadoOperativo().equals(estado)).count());
        }
        int ocupados = porEstado.get("OCUPADO");
        int operativos = suites.size() - porEstado.get("FUERA_DE_SERVICIO");
        double ingresos = reservas.values().stream().mapToDouble(this::pagado).sum();
        double promedio = minutosPreparacion.stream().mapToLong(Long::longValue).average().orElse(0);
        return new ReporteDTO(suites.size(), ocupados, operativos == 0 ? 0 : ocupados * 100.0 / operativos,
                ingresos, porCanal, porEstado, conflictos.size(), promedio);
    }

    // ---------- Guardado ----------

    /** Escribe el estado actual en disco; se llama tras cada cambio. */
    private void guardar() {
        try {
            AlmacenSGA.guardar(archivo, exportar());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudieron guardar los cambios en " + archivo + ".", e);
        }
    }

    private Instantanea exportar() {
        SistemaGestionAlojamiento sistema = getSistema();
        Map<String, List<String>> favoritos = new LinkedHashMap<>();
        for (Huesped h : sistema.listarHuespedes()) {
            List<String> codigos = new ArrayList<>();
            for (Apartamento a : h.getFavoritos()) {
                codigos.add(a.getCodigo());
            }
            if (!codigos.isEmpty()) {
                favoritos.put(h.getDocumento(), codigos);
            }
        }
        List<Orden> pendientes = new ArrayList<>();
        for (OrdenPreparacion o : sistema.getPreparacion().listarPendientes()) {
            pendientes.add(new Orden(o.getCodigo(), o.getFechaCreacion()));
        }
        List<Orden> enCurso = new ArrayList<>();
        for (OrdenPreparacion o : sistema.getPreparacion().listarEnPreparacion()) {
            enCurso.add(new Orden(o.getCodigo(), o.getFechaCreacion()));
        }
        List<ConexionDTO> cerrados = conexionesEdificio().stream().filter(c -> !c.habilitada()).toList();
        String turno = sistema.getTurnos().estaVacio() ? null : sistema.getTurnos().turnoActual();
        return new Instantanea(new ArrayList<>(listarApartaSuites()), new ArrayList<>(listarHuespedes()), favoritos,
                new LinkedHashMap<>(cuentas), new ArrayList<>(reservas.values()), new ArrayList<>(temporadas),
                new ArrayList<>(empleados), turno, new ArrayList<>(colaExterna), new ArrayList<>(conflictos),
                pendientes, enCurso, new ArrayList<>(minutosPreparacion), new ArrayList<>(cerrados),
                consecutivoReserva);
    }

    /** Arma un sistema nuevo con los datos guardados y reemplaza el actual. */
    private void importar(Instantanea d) {
        reemplazarSistema(new SistemaGestionAlojamiento());
        SistemaGestionAlojamiento sistema = getSistema();
        cuentas.clear();
        descripciones.clear();
        reservas.clear();
        temporadas.clear();
        empleados.clear();
        colaExterna.clear();
        conflictos.clear();
        minutosPreparacion.clear();

        // Los que estaban en preparación se registran Preparados: la cola los vuelve a pasar a su estado.
        for (ApartaSuiteDTO s : d.apartaSuites()) {
            super.guardarApartaSuite(s);
            descripciones.put(s.codigo(), s.descripcion() == null ? "" : s.descripcion());
            boolean enProceso = "PENDIENTE_PREPARACION".equals(s.estadoOperativo())
                    || "EN_PREPARACION".equals(s.estadoOperativo());
            apartamento(s.codigo()).setEstadoOperativo(enProceso ? EstadoOperativo.PREPARADO
                    : estadoModelo(s.estadoOperativo()));
        }
        for (HuespedDTO h : d.huespedes()) {
            super.registrarHuesped(h);
        }
        d.favoritos().forEach((documento, codigos) -> {
            Huesped h = sistema.buscarHuesped(documento);
            for (String codigo : codigos) {
                Apartamento a = sistema.buscarApartamento(codigo);
                if (h != null && a != null) {
                    h.agregarFavorito(a);
                }
            }
        });
        cuentas.putAll(d.cuentas());
        d.reservas().forEach(r -> reservas.put(r.codigo, r));
        consecutivoReserva = d.consecutivoReserva();
        temporadas.addAll(d.temporadas());
        reservas.values().forEach(this::completarReservaInicial);

        for (EmpleadoDTO e : d.empleados()) {
            empleados.add(e);
            super.registrarEmpleado(e);
        }
        if (d.turnoActual() != null) {
            for (int i = 0; i < empleados.size() && !d.turnoActual().equals(sistema.getTurnos().turnoActual()); i++) {
                sistema.getTurnos().avanzarTurno();
            }
        }
        colaExterna.addAll(d.colaExterna());
        conflictos.addAll(d.conflictos());
        minutosPreparacion.addAll(d.minutosPreparacion());

        // Primero los que ya estaban en preparación (cola vacía: el despacho los saca a ellos),
        // luego la cola en el orden en que entraron.
        for (Orden o : d.enPreparacion()) {
            encolarPreparacion(o.codigoApartaSuite()).setFechaCreacion(o.ingreso());
            sistema.getPreparacion().despacharSiguiente();
        }
        d.pendientesPreparacion().stream().sorted(Comparator.comparing(Orden::ingreso))
                .forEach(o -> encolarPreparacion(o.codigoApartaSuite()).setFechaCreacion(o.ingreso()));

        for (ConexionDTO c : d.pasillosCerrados()) {
            super.alternarPasillo(c.origen(), c.destino());
        }
    }

    /** Las reservas del escenario inicial llegan sin total: se cotizan y se cargan al folio. */
    private void completarReservaInicial(Reserva r) {
        if (r.total >= 0) {
            return;
        }
        r.total = cotizar(r.codigoSuite, r.entrada, r.salida, r.ocupantes.size()).total();
        r.movimientos.addFirst(new MovimientoFolioDTO("CARGO", "Alojamiento · " + r.ocupantes.size() + " personas",
                r.total, LocalDateTime.now()));
        if ("FINALIZADA".equals(r.estado)) {
            movimiento(r, "PAGO", "Pago total", r.total, false);
        }
    }

    // ---------- Apoyo ----------

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

    private static void movimiento(Reserva reserva, String tipo, String concepto, double monto, boolean deshacible) {
        MovimientoFolioDTO m = new MovimientoFolioDTO(tipo, concepto, monto, LocalDateTime.now());
        reserva.movimientos.add(m);
        if (deshacible) {
            reserva.pilaDeshacer().push(m);
            reserva.deshacibles.add(m);
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
        Huesped titular = getSistema().buscarHuesped(r.documentoTitular);
        Apartamento suite = getSistema().buscarApartamento(r.codigoSuite);
        return new ReservaDTO(r.codigo, r.documentoTitular, titular == null ? r.documentoTitular : titular.getNombre(),
                r.codigoSuite, suite == null ? r.codigoSuite : suite.getNombre(), r.entrada, r.salida,
                r.ocupantes.size(), r.canal, r.estado, r.horaLlegada, r.total,
                Math.round(r.total * PORCENTAJE_ANTICIPO), pagado(r), "CANCELADA".equals(r.estado) ? 0 : saldo);
    }
}
