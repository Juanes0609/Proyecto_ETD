package gui.integracion;

import gui.integracion.AlmacenSGA.Cuenta;
import gui.integracion.AlmacenSGA.Instantanea;
import gui.integracion.AlmacenSGA.Orden;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.EmpleadoDTO;
import gui.integracion.dto.HuespedDTO;
import gui.integracion.dto.MovimientoFolioDTO;
import gui.integracion.dto.OcupanteDTO;
import gui.integracion.dto.Rol;
import gui.integracion.dto.SolicitudExternaDTO;
import gui.integracion.dto.TemporadaDTO;
import gui.integracion.dto.UsuarioDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Datos de prueba con los que arranca la aplicación la primera vez (o al pulsar
 * «Cargar escenario»): 10 aparta-suites en 3 bloques, huéspedes, reservas en
 * distintos estados, temporadas, recepcionistas, cola de preparación y
 * solicitudes del canal externo.
 * <p>
 * Cuentas de prueba, todas con la clave {@value #CLAVE_PRUEBA}:
 * {@code huesped@demo.co}, {@code recepcion@demo.co} y {@code admin@demo.co}.
 */
public final class EscenarioInicialSGA {

    public static final String CLAVE_PRUEBA = "1234";

    private EscenarioInicialSGA() {
    }

    static Instantanea crear() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();

        List<ApartaSuiteDTO> suites = List.of(
                suite("A-101", "Suite Cocora", "A", 1, 1, 2, 85_000, 98_000, 125_000, "PREPARADO",
                        "Vista al valle de Cocora, cama queen y cocineta equipada.",
                        "Vista al valle", "Cama queen", "Cocineta", "Wi-Fi", "Agua caliente"),
                suite("A-102", "Suite Palma de Cera", "A", 1, 2, 4, 78_000, 90_000, 115_000, "PREPARADO",
                        "Dos habitaciones con baño privado y sala con chimenea.",
                        "Chimenea", "2 baños", "Cocina completa", "Wi-Fi"),
                suite("A-201", "Suite Barbas", "A", 2, 2, 5, 72_000, 84_000, 108_000, "PREPARADO",
                        "Ideal para familias: litera para niños y balcón amplio.",
                        "Balcón", "Litera", "Cocina completa", "Wi-Fi", "Parqueadero"),
                suite("A-202", "Suite Mirador", "A", 2, 1, 3, 90_000, 104_000, 132_000, "PENDIENTE_PREPARACION",
                        "Ventanal panorámico hacia las montañas y bañera.",
                        "Ventanal panorámico", "Bañera", "Cama king", "Wi-Fi"),
                suite("B-101", "Suite Bosque de Niebla", "B", 1, 2, 4, 75_000, 88_000, 110_000, "OCUPADO",
                        "Rodeada de jardín, con terraza privada y hamaca.",
                        "Terraza privada", "Hamaca", "Cocineta", "Wi-Fi"),
                suite("B-102", "Suite Quindío", "B", 1, 3, 6, 68_000, 80_000, 102_000, "PREPARADO",
                        "La más amplia del bloque B, pensada para grupos.",
                        "3 habitaciones", "2 baños", "Comedor para 6", "Parqueadero", "Wi-Fi"),
                suite("B-201", "Suite Cafetal", "B", 2, 1, 2, 82_000, 95_000, 120_000, "PREPARADO",
                        "Decoración con maderas locales y vista a los cafetales.",
                        "Vista a cafetales", "Cama queen", "Cafetera", "Wi-Fi"),
                suite("B-202", "Suite Arriero", "B", 2, 2, 4, 76_000, 89_000, 112_000, "FUERA_DE_SERVICIO",
                        "Estilo tradicional paisa con balcón de madera.",
                        "Balcón", "Cocina completa", "Wi-Fi"),
                suite("C-101", "Suite Guadua", "C", 1, 3, 7, 65_000, 78_000, 99_000, "PENDIENTE_PREPARACION",
                        "Construida en guadua, con zona de BBQ y jardín propio.",
                        "Zona BBQ", "Jardín", "3 habitaciones", "Parqueadero", "Wi-Fi"),
                suite("C-201", "Suite Colibrí", "C", 2, 1, 2, 88_000, 101_000, 128_000, "PREPARADO",
                        "Pequeña y luminosa, perfecta para parejas.",
                        "Jacuzzi", "Cama queen", "Desayuno incluido", "Wi-Fi"));

        HuespedDTO ana = new HuespedDTO("1094000001", "Ana María Gómez", LocalDate.of(1995, 3, 12),
                "300 123 4567", "huesped@demo.co");
        List<HuespedDTO> huespedes = List.of(ana,
                new HuespedDTO("1094000002", "Juan Pablo Pérez", LocalDate.of(1988, 7, 2), "311 222 3344",
                        "juan.perez@correo.com"),
                new HuespedDTO("1094000003", "María José López", LocalDate.of(1992, 11, 20), "320 555 1212",
                        "mjlopez@correo.com"),
                new HuespedDTO("1094000004", "Pedro Antonio Ruiz", LocalDate.of(1979, 1, 5), "315 777 8899",
                        "pedro.ruiz@correo.com"));

        Map<String, Cuenta> cuentas = new LinkedHashMap<>();
        cuentas.put("huesped@demo.co", new Cuenta(CLAVE_PRUEBA,
                new UsuarioDTO("huesped@demo.co", ana.nombre(), Rol.HUESPED, ana.documento())));
        cuentas.put("recepcion@demo.co", new Cuenta(CLAVE_PRUEBA,
                new UsuarioDTO("recepcion@demo.co", "Laura Ríos", Rol.RECEPCIONISTA, "41900001")));
        cuentas.put("admin@demo.co", new Cuenta(CLAVE_PRUEBA,
                new UsuarioDTO("admin@demo.co", "Carlos Mejía", Rol.ADMINISTRADOR, "41900099")));

        int[] consecutivo = {1000};
        List<ConectorSGAHibrido.Reserva> reservas = new ArrayList<>();
        reservas.add(reserva(consecutivo, ana, "A-101", hoy.plusDays(20), hoy.plusDays(23), "PORTAL", "PENDIENTE",
                1, null));
        reservas.add(reserva(consecutivo, ana, "B-201", hoy.minusDays(30), hoy.minusDays(27), "PORTAL", "FINALIZADA",
                1, null));
        reservas.add(reserva(consecutivo, huespedes.get(1), "A-201", hoy, hoy.plusDays(3), "DIRECTA", "CONFIRMADA",
                3, LocalTime.of(15, 0)));
        ConectorSGAHibrido.Reserva enCurso = reserva(consecutivo, huespedes.get(2), "B-101", hoy.minusDays(2),
                hoy.plusDays(1), "DIRECTA", "EN_CURSO", 2, null);
        movimientoDeshacible(enCurso, "PAGO", "Pago en recepción", 200_000, ahora);
        movimientoDeshacible(enCurso, "CARGO", "Restaurante · cena", 86_000, ahora);
        reservas.add(enCurso);
        reservas.add(reserva(consecutivo, huespedes.get(3), "C-101", hoy, hoy.plusDays(2), "EXTERNA", "CONFIRMADA",
                5, LocalTime.of(16, 30)));

        return new Instantanea(
                suites,
                huespedes,
                Map.of(ana.documento(), List.of("A-202", "C-201")),
                cuentas,
                reservas,
                List.of(new TemporadaDTO("MEDIA", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30)),
                        new TemporadaDTO("ALTA", LocalDate.of(2026, 12, 15), LocalDate.of(2027, 1, 15)),
                        new TemporadaDTO("MEDIA", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 15)),
                        new TemporadaDTO("ALTA", LocalDate.of(2027, 3, 20), LocalDate.of(2027, 4, 5)),
                        new TemporadaDTO("ALTA", LocalDate.of(2027, 6, 15), LocalDate.of(2027, 7, 31))),
                List.of(new EmpleadoDTO("41900001", "Laura Ríos", "06:00 – 14:00"),
                        new EmpleadoDTO("41900002", "Andrés Castaño", "14:00 – 22:00"),
                        new EmpleadoDTO("41900003", "Sofía Marín", "22:00 – 06:00")),
                null,
                List.of(new SolicitudExternaDTO("EXT-01", "Lucía Fernández", "52800011", "A-101",
                                hoy.plusDays(21), hoy.plusDays(24), 2, ahora.minusMinutes(50)),
                        new SolicitudExternaDTO("EXT-02", "Tomás Herrera", "79100022", "B-102",
                                hoy.plusDays(5), hoy.plusDays(8), 4, ahora.minusMinutes(20))),
                List.of(),
                List.of(new Orden("A-202", ahora.minusHours(3)), new Orden("C-101", ahora.minusHours(1))),
                List.of(),
                List.of(42L, 55L, 38L, 47L),
                List.of(),
                consecutivo[0]);
    }

    private static ApartaSuiteDTO suite(String codigo, String nombre, String bloque, int piso, int dormitorios,
                                        int capacidad, double baja, double media, double alta, String estado,
                                        String descripcion, String... caracteristicas) {
        return new ApartaSuiteDTO(codigo, nombre, bloque, piso, dormitorios, capacidad, baja, media, alta,
                List.of(caracteristicas), descripcion, estado, null);
    }

    /** El total y el cargo de alojamiento los calcula el conector al cargar (con las temporadas). */
    private static ConectorSGAHibrido.Reserva reserva(int[] consecutivo, HuespedDTO titular, String suite,
                                                      LocalDate entrada, LocalDate salida, String canal,
                                                      String estado, int personas, LocalTime hora) {
        ConectorSGAHibrido.Reserva r = new ConectorSGAHibrido.Reserva();
        r.codigo = "R-" + (++consecutivo[0]);
        r.documentoTitular = titular.documento();
        r.codigoSuite = suite;
        r.entrada = entrada;
        r.salida = salida;
        r.canal = canal;
        r.estado = estado;
        r.horaLlegada = hora;
        r.ocupantes.add(new OcupanteDTO(titular.nombre(), titular.documento(), titular.fechaNacimiento()));
        for (int i = 2; i <= personas; i++) {
            r.ocupantes.add(new OcupanteDTO("Acompañante " + i, null, null));
        }
        return r;
    }

    private static void movimientoDeshacible(ConectorSGAHibrido.Reserva r, String tipo, String concepto,
                                             double monto, LocalDateTime fecha) {
        MovimientoFolioDTO m = new MovimientoFolioDTO(tipo, concepto, monto, fecha);
        r.movimientos.add(m);
        r.deshacibles.add(m);
    }
}
