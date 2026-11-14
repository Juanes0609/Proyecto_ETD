package gui.integracion;

import estructuras.ArbolBinarioBusqueda;
import estructuras.Cola;
import estructuras.ColaPrioridad;
import estructuras.ListaDoblementeEnlazada;
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
import modelo.Canal;
import modelo.ComparadoresApartamento;
import modelo.ConstructorEdificio;
import modelo.ConteoEstadistica;
import modelo.DetalleNoche;
import modelo.EstadoOperativo;
import modelo.EstadoReserva;
import modelo.Estancia;
import modelo.Huesped;
import modelo.Ocupante;
import modelo.OrdenPreparacion;
import modelo.ReporteEstadisticas;
import modelo.SistemaGestionAlojamiento;
import modelo.Temporada;
import modelo.TipoTemporada;
import servicio.ServicioFacturacion;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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
 * sobre {@code SistemaGestionAlojamiento}. Aquí se cubren la sesión, las
 * reservas, los folios, el canal externo y el reporte, siempre apoyados en las
 * estructuras y servicios del núcleo:
 * <ul>
 *   <li>cada reserva se refleja en el modelo ({@code modelo.Reserva}) y queda en el
 *       árbol por fecha de entrada de su apartamento: el solapamiento lo decide
 *       {@code Apartamento.tieneSolapamiento};</li>
 *   <li>los cambios de estado respetan {@code EstadoReserva.puedeTransicionarA};</li>
 *   <li>el valor se liquida con {@code ServicioFacturacion} (recursivo, noche por
 *       noche, recorriendo la {@code Estancia} con su iterador);</li>
 *   <li>el canal externo usa la {@code Cola} propia, los vencimientos la
 *       {@code ColaPrioridad}, deshacer la {@code Pila} y los empleados el
 *       {@code ArbolBinarioBusqueda}.</li>
 * </ul>
 * Lo que se guarda en disco es una copia plana de esos datos (las estructuras
 * propias no son serializables).
 * <p>
 * Todo cambio se guarda en disco ({@link AlmacenSGA}); la primera vez, o al
 * pulsar «Cargar escenario», se parte de {@link EscenarioInicialSGA}.
 */
public class ConectorSGAHibrido extends ConectorSGAProveedor {

    private static final double PORCENTAJE_ANTICIPO = 0.30;

    /** Horas que una reserva del portal espera el anticipo antes de cancelarse sola. */
    private static final int HORAS_VENCIMIENTO = 24;
    /** Fecha de nacimiento que se asume cuando no se conoce: un adulto, que paga. */
    private static final LocalDate NACIMIENTO_ADULTO = LocalDate.of(1990, 1, 1);

    /** Datos guardables de una reserva; su par en el modelo se arma con {@link #reflejarEnModelo}. */
    static final class Reserva implements Serializable {
        String codigo, documentoTitular, codigoSuite, canal, estado;
        LocalDate entrada, salida;
        LocalTime horaLlegada;
        /** Momento en que se creó: de aquí sale el vencimiento de las pendientes. */
        LocalDateTime creada;
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

        LocalDateTime vence() {
            return creada.plusHours(HORAS_VENCIMIENTO);
        }
    }

    private final Path archivo;
    private final Map<String, Cuenta> cuentas = new LinkedHashMap<>();
    private final Map<String, String> descripciones = new HashMap<>();
    private final Map<String, Reserva> reservas = new LinkedHashMap<>();
    /** Par de cada reserva en el modelo real, por código. */
    private final Map<String, modelo.Reserva> enModelo = new HashMap<>();
    private final List<TemporadaDTO> temporadas = new ArrayList<>();
    /** Las mismas temporadas como las entiende {@link ServicioFacturacion}. */
    private ListaDoblementeEnlazada<Temporada> temporadasModelo = new ListaDoblementeEnlazada<>();
    /** Empleados por documento, en el árbol binario de búsqueda del proyecto. */
    private ArbolBinarioBusqueda<EmpleadoDTO> empleados = new ArbolBinarioBusqueda<>();
    /** Solicitudes del canal externo en orden de llegada (cola FIFO del proyecto). */
    private Cola<SolicitudExternaDTO> colaExterna = new Cola<>();
    /** Reservas pendientes de anticipo; primero la que vence antes. */
    private ColaPrioridad<Reserva> vencimientos = nuevaColaVencimientos();
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
        temporadasModelo.agregarFinal(aModelo(temporada));
        guardar();
    }

    private static Temporada aModelo(TemporadaDTO t) {
        return new Temporada(t.inicio(), t.fin(), TipoTemporada.valueOf(t.tipo()));
    }

    /** Facturación con las tarifas propias del aparta-suite y las temporadas vigentes. */
    private ServicioFacturacion facturacion(Apartamento a) {
        return new ServicioFacturacion(temporadasModelo, a.getTarifaBaja(), a.getTarifaMedia(), a.getTarifaAlta());
    }

    @Override
    public String temporadaEn(LocalDate fecha) {
        return new ServicioFacturacion(temporadasModelo, 0, 0, 0).temporadaDe(fecha).name();
    }

    /**
     * El solapamiento lo resuelve el árbol de reservas (por fecha de entrada) del
     * apartamento, recorriéndolo de forma recursiva.
     */
    @Override
    public boolean estaDisponible(String codigoApartaSuite, LocalDate entrada, LocalDate salida) {
        if (entrada == null || salida == null || !salida.isAfter(entrada)) {
            return false;
        }
        return !apartamento(codigoApartaSuite).tieneSolapamiento(new Estancia(entrada, salida, null));
    }

    /**
     * Aparta-suites libres en el rango (si se indica) con cupo para el grupo, en el
     * orden elegido. Se ordenan pasando por la {@link ColaPrioridad} propia con los
     * comparadores del modelo: salen de la cola ya ordenados.
     */
    @Override
    public List<ApartaSuiteDTO> buscarDisponibles(LocalDate entrada, LocalDate salida, int personas, String orden) {
        boolean conFechas = entrada != null && salida != null && salida.isAfter(entrada);
        TipoTemporada temporada = TipoTemporada.valueOf(temporadaEn(conFechas ? entrada : LocalDate.now()));
        Comparator<Apartamento> comparador = switch (orden == null ? "" : orden) {
            case "PRECIO" -> ComparadoresApartamento.porPrecio(temporada);
            case "CAPACIDAD" -> ComparadoresApartamento.POR_CAPACIDAD;
            case "DORMITORIOS" -> ComparadoresApartamento.POR_DORMITORIOS;
            default -> Comparator.naturalOrder();
        };
        ColaPrioridad<Apartamento> ordenados = new ColaPrioridad<>(comparador);
        for (Apartamento a : getSistema().listarApartamentos()) {
            if (a.getEstadoOperativo() != EstadoOperativo.FUERASERVICIO
                    && a.getCapacidadMaxima() >= personas
                    && (!conFechas || !a.tieneSolapamiento(new Estancia(entrada, salida, null)))) {
                ordenados.encolar(a);
            }
        }
        List<ApartaSuiteDTO> lista = new ArrayList<>();
        while (!ordenados.estaVacia()) {
            lista.add(aDto(ordenados.desencolar()));
        }
        return lista;
    }

    @Override
    public CotizacionDTO cotizar(String codigoApartaSuite, LocalDate entrada, LocalDate salida, int personasQuePagan) {
        validarFechas(entrada, salida, false);
        ServicioFacturacion facturacion = facturacion(apartamento(codigoApartaSuite));
        Estancia estancia = new Estancia(entrada, salida, null);
        ListaDoblementeEnlazada<Ocupante> pagan = new ListaDoblementeEnlazada<>();
        for (int i = 0; i < personasQuePagan; i++) {
            pagan.agregarFinal(new Ocupante("Ocupante " + (i + 1), NACIMIENTO_ADULTO));
        }
        // El desglose recorre la estancia con su iterador; el total es la liquidación recursiva.
        List<NocheDTO> noches = new ArrayList<>();
        for (DetalleNoche d : facturacion.desglosar(estancia, pagan)) {
            noches.add(new NocheDTO(d.getFecha(), d.getTemporada().name(), d.getCantidadPersonasPagas(),
                    d.getTarifaPorPersona(), d.getSubtotal()));
        }
        return new CotizacionDTO(noches, personasQuePagan, facturacion.liquidar(estancia, pagan));
    }

    /** Ocupantes como los espera el modelo; sin fecha de nacimiento se cobra como adulto. */
    private static ListaDoblementeEnlazada<Ocupante> ocupantesModelo(List<OcupanteDTO> ocupantes) {
        ListaDoblementeEnlazada<Ocupante> lista = new ListaDoblementeEnlazada<>();
        for (OcupanteDTO o : ocupantes) {
            lista.agregarFinal(new Ocupante(o.nombre(),
                    o.fechaNacimiento() == null ? NACIMIENTO_ADULTO : o.fechaNacimiento()));
        }
        return lista;
    }

    /** Valor de la estancia: menores de 6 años ocupan cupo pero no pagan (edad en cada noche). */
    private double liquidar(Reserva r) {
        return facturacion(apartamento(r.codigoSuite))
                .liquidar(new Estancia(r.entrada, r.salida, null), ocupantesModelo(r.ocupantes));
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
        reserva.creada = LocalDateTime.now();
        reserva.estado = "PORTAL".equals(canal) ? "PENDIENTE" : "CONFIRMADA";
        reserva.ocupantes.add(new OcupanteDTO(titular.nombre(), titular.documento(), titular.fechaNacimiento()));
        reserva.ocupantes.addAll(acompanantes);
        reserva.total = liquidar(reserva);
        movimiento(reserva, "CARGO", "Alojamiento · " + reserva.ocupantes.size() + " personas", reserva.total, false);
        reservas.put(reserva.codigo, reserva);
        reflejarEnModelo(reserva);
        if ("PENDIENTE".equals(reserva.estado)) {
            vencimientos.encolar(reserva);
        }
        return reserva;
    }

    // ---------- Reservas en el modelo real ----------

    /**
     * Crea el par de la reserva en el modelo: queda en el historial del apartamento
     * y del huésped y, si está activa, en el árbol por fecha de entrada.
     */
    private void reflejarEnModelo(Reserva r) {
        Apartamento apartamento = getSistema().buscarApartamento(r.codigoSuite);
        if (apartamento == null) {
            return; // el aparta-suite ya no existe: la reserva solo queda en el historial guardado
        }
        Huesped titular = getSistema().buscarHuesped(r.documentoTitular);
        Estancia estancia = new Estancia(r.entrada, r.salida, null);
        modelo.Reserva par = new modelo.Reserva(r.codigo, r.creada,
                "PENDIENTE".equals(r.estado) ? r.vence() : null, estadoReserva(r.estado),
                Canal.valueOf(r.canal), apartamento, titular, estancia);
        estancia.setReserva(par);
        for (Ocupante o : ocupantesModelo(r.ocupantes)) {
            par.getListOcupantes().agregarFinal(o);
        }
        apartamento.registrarReserva(par);
        if (titular != null) {
            titular.agregarReserva(par);
        }
        enModelo.put(r.codigo, par);
    }

    /**
     * Único punto donde cambia el estado de una reserva: valida la transición con
     * las reglas del modelo y, si deja de estar activa, libera sus noches en el árbol.
     */
    private void cambiarEstado(Reserva r, String destino) {
        EstadoReserva actual = estadoReserva(r.estado);
        EstadoReserva nuevo = estadoReserva(destino);
        if (!actual.puedeTransicionarA(nuevo)) {
            throw new TransicionEstadoInvalidaException("La reserva " + r.codigo + " está "
                    + nombreEstado(r.estado) + " y no puede pasar a " + nombreEstado(destino) + ".");
        }
        r.estado = destino;
        modelo.Reserva par = enModelo.get(r.codigo);
        if (par != null) {
            par.setEstadoReserva(nuevo);
            if (actual.esActiva() && !nuevo.esActiva()) {
                par.getApartamento().retirarReservaActiva(par);
            }
        }
    }

    private static EstadoReserva estadoReserva(String estado) {
        return switch (estado) {
            case "EN_CURSO" -> EstadoReserva.ENCURSO;
            case "NO_SHOW" -> EstadoReserva.NOSHOW;
            default -> EstadoReserva.valueOf(estado);
        };
    }

    private static String estadoDto(EstadoReserva estado) {
        return switch (estado) {
            case ENCURSO -> "EN_CURSO";
            case NOSHOW -> "NO_SHOW";
            default -> estado.name();
        };
    }

    private static String nombreEstado(String estado) {
        return estado.replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static ColaPrioridad<Reserva> nuevaColaVencimientos() {
        return new ColaPrioridad<>(Comparator.comparing(Reserva::vence));
    }

    /**
     * Cancela las reservas pendientes cuyo plazo de anticipo ya pasó. La cola de
     * prioridad está ordenada por vencimiento: se detiene en la primera que aún no vence.
     */
    private int vencerPendientes() {
        LocalDateTime ahora = LocalDateTime.now();
        int canceladas = 0;
        while (!vencimientos.estaVacia() && !vencimientos.consultarPrioridad().vence().isAfter(ahora)) {
            Reserva r = vencimientos.desencolar();
            if ("PENDIENTE".equals(r.estado)) { // si ya pagó o canceló, se descarta
                cambiarEstado(r, "CANCELADA");
                canceladas++;
            }
        }
        return canceladas;
    }

    @Override
    public int procesarVencimientos() {
        int canceladas = vencerPendientes();
        if (canceladas > 0) {
            guardar();
        }
        return canceladas;
    }

    @Override
    public void pagarAnticipo(String codigoReserva) {
        Reserva reserva = reserva(codigoReserva);
        if (!"PENDIENTE".equals(reserva.estado)) {
            throw new TransicionEstadoInvalidaException("La reserva " + codigoReserva + " no está pendiente de anticipo.");
        }
        cambiarEstado(reserva, "CONFIRMADA");
        movimiento(reserva, "PAGO", "Anticipo por el portal", Math.round(reserva.total * PORCENTAJE_ANTICIPO), false);
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
        cambiarEstado(reserva, "CANCELADA");
        if (devolucion > 0) {
            movimiento(reserva, "ANULACION_PAGO", "Devolución por cancelación", devolucion, false);
        }
        guardar();
    }

    /** El huésped no llegó: la reserva termina como No-show y sus noches quedan libres. */
    @Override
    public void marcarNoShow(String codigoReserva) {
        Reserva reserva = reserva(codigoReserva);
        if (!"CONFIRMADA".equals(reserva.estado)) {
            throw new TransicionEstadoInvalidaException("Solo una reserva confirmada puede marcarse como no-show.");
        }
        if (reserva.entrada.isAfter(LocalDate.now())) {
            throw new TransicionEstadoInvalidaException("La llegada de " + codigoReserva + " es el "
                    + reserva.entrada + ": aún no se puede marcar como no-show.");
        }
        cambiarEstado(reserva, "NO_SHOW");
        guardar();
    }

    /** Reservas que ocupan o retienen alguna noche entre {@code desde} (incluida) y {@code hasta} (excluida). */
    @Override
    public List<ReservaDTO> reservasEntre(LocalDate desde, LocalDate hasta) {
        return reservas.values().stream()
                .filter(r -> !"CANCELADA".equals(r.estado) && !"NO_SHOW".equals(r.estado))
                .filter(r -> seSolapan(r.entrada, r.salida, desde, hasta))
                .sorted(Comparator.comparing((Reserva r) -> r.entrada)).map(this::dto).toList();
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
        cambiarEstado(reserva, "EN_CURSO");
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
        cambiarEstado(reserva, "FINALIZADA");
        encolarPreparacion(reserva.codigoSuite);
        guardar();
    }

    // ---------- Canal externo ----------

    @Override
    public List<SolicitudExternaDTO> colaCanalExterno() {
        List<SolicitudExternaDTO> lista = new ArrayList<>();
        for (SolicitudExternaDTO s : colaExterna) {
            lista.add(s);
        }
        return lista;
    }

    /**
     * Atiende la solicitud más antigua. Si no se puede convertir en reserva (choca
     * con otra, no cabe el grupo, fechas inválidas, aparta-suite inexistente o fuera
     * de servicio) se rechaza y queda como conflicto de canal: la cola nunca se bloquea.
     */
    @Override
    public String procesarSiguienteSolicitudExterna() {
        if (colaExterna.estaVacia()) {
            throw new EstructuraVaciaException("No hay solicitudes externas en espera.");
        }
        SolicitudExternaDTO solicitud = colaExterna.desencolar();
        String mensaje;
        try {
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
        } catch (ReservaSolapadaException | CapacidadExcedidaException | FechasInvalidasException
                 | TransicionEstadoInvalidaException | IllegalArgumentException e) {
            conflictos.add(solicitud.id());
            mensaje = "Conflicto de canal: la solicitud " + solicitud.id() + " fue rechazada. " + e.getMessage();
        }
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
        // El tiempo de preparación corre desde que se despacha la orden, no desde que entra a la cola.
        LocalDateTime inicio = null;
        for (OrdenPreparacion o : getSistema().getPreparacion().listarEnPreparacion()) {
            if (o.getCodigo().equals(codigoApartaSuite)) {
                inicio = o.getHoraInicioPreparacion() != null ? o.getHoraInicioPreparacion() : o.getFechaCreacion();
            }
        }
        super.terminarPreparacion(codigoApartaSuite);
        if (inicio != null) {
            minutosPreparacion.add(Math.max(1, ChronoUnit.MINUTES.between(inicio, LocalDateTime.now())));
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
        List<EmpleadoDTO> lista = new ArrayList<>();
        for (EmpleadoDTO e : empleados) { // el árbol se recorre en orden de documento
            lista.add(e);
        }
        return lista;
    }

    /** Todo recepcionista nuevo entra al final de la rotación. */
    @Override
    public void registrarEmpleado(EmpleadoDTO empleado) {
        if (!empleados.insertar(empleado)) { // el árbol no admite dos empleados con el mismo documento
            throw new IllegalArgumentException("Ya existe un empleado con el documento " + empleado.documento() + ".");
        }
        super.registrarEmpleado(empleado);
        guardar();
    }

    /** El modelo guarda solo el nombre; documento y horario salen de la lista de empleados. */
    private EmpleadoDTO empleado(String nombre) {
        EmpleadoDTO encontrado = empleados.existeSi(e -> e.nombre().equals(nombre));
        return encontrado != null ? encontrado : new EmpleadoDTO("-", nombre, "-");
    }

    // ---------- Mapa y reporte ----------

    @Override
    public void alternarPasillo(String origen, String destino) {
        super.alternarPasillo(origen, destino);
        guardar();
    }

    /**
     * Ocupación, reservas por canal y por estado salen de {@code GestorEstadisticas},
     * que recorre los apartamentos del árbol y sus reservas. Ingresos, conflictos y
     * tiempos de preparación se toman de los folios y registros que lleva este conector.
     */
    @Override
    public ReporteDTO generarReporte(LocalDate desde, LocalDate hasta) {
        ReporteEstadisticas r = getSistema().getEstadisticas().generar(desde, hasta, temporadasModelo,
                ConstructorEdificio.RECEPCION);
        double ingresos = 0;
        for (Reserva reserva : reservas.values()) {
            for (MovimientoFolioDTO m : reserva.movimientos) {
                LocalDate dia = m.fechaHora().toLocalDate();
                if (!dia.isBefore(desde) && dia.isBefore(hasta)) {
                    ingresos += switch (m.tipo()) {
                        case "PAGO" -> m.monto();
                        case "ANULACION_PAGO" -> -m.monto();
                        default -> 0;
                    };
                }
            }
        }
        double promedio = minutosPreparacion.stream().mapToLong(Long::longValue).average().orElse(0);
        return aReporte(r, desde, hasta, ingresos, conflictos.size(), promedio);
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
        // Los empleados se guardan en el orden de la rotación de turnos (lista circular).
        List<EmpleadoDTO> enRotacion = new ArrayList<>();
        for (String nombre : sistema.getTurnos().listarOrdenTurnos()) {
            enRotacion.add(empleado(nombre));
        }
        return new Instantanea(new ArrayList<>(listarApartaSuites()), new ArrayList<>(listarHuespedes()), favoritos,
                new LinkedHashMap<>(cuentas), new ArrayList<>(reservas.values()), new ArrayList<>(temporadas),
                enRotacion, turno, new ArrayList<>(colaCanalExterno()), new ArrayList<>(conflictos),
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
        enModelo.clear();
        temporadas.clear();
        temporadasModelo = new ListaDoblementeEnlazada<>();
        empleados = new ArbolBinarioBusqueda<>();
        colaExterna = new Cola<>();
        vencimientos = nuevaColaVencimientos();
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
        for (TemporadaDTO t : d.temporadas()) {
            temporadas.add(t);
            temporadasModelo.agregarFinal(aModelo(t));
        }
        for (Reserva r : reservas.values()) {
            if (r.creada == null) {
                r.creada = LocalDateTime.now();
            }
            completarReservaInicial(r);
            reflejarEnModelo(r);
            if ("PENDIENTE".equals(r.estado)) {
                vencimientos.encolar(r);
            }
        }

        for (EmpleadoDTO e : d.empleados()) {
            empleados.insertar(e);
            super.registrarEmpleado(e);
        }
        if (d.turnoActual() != null) {
            for (int i = 0; i < empleados.size() && !d.turnoActual().equals(sistema.getTurnos().turnoActual()); i++) {
                sistema.getTurnos().avanzarTurno();
            }
        }
        for (SolicitudExternaDTO solicitud : d.colaExterna()) {
            colaExterna.encolar(solicitud);
        }
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
        vencerPendientes(); // lo que venció mientras la aplicación estaba cerrada
    }

    /** Las reservas del escenario inicial llegan sin total: se cotizan y se cargan al folio. */
    private void completarReservaInicial(Reserva r) {
        if (r.total >= 0) {
            return;
        }
        r.total = liquidar(r);
        r.movimientos.addFirst(new MovimientoFolioDTO("CARGO", "Alojamiento · " + r.ocupantes.size() + " personas",
                r.total, r.creada));
        if ("FINALIZADA".equals(r.estado)) {
            // Se pagó al salir: así los ingresos caen en el periodo de la estancia.
            r.movimientos.add(new MovimientoFolioDTO("PAGO", "Pago total", r.total, r.salida.atTime(11, 0)));
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
                Math.round(r.total * PORCENTAJE_ANTICIPO), pagado(r), "CANCELADA".equals(r.estado) ? 0 : saldo,
                "PENDIENTE".equals(r.estado) ? r.vence() : null);
    }
}
