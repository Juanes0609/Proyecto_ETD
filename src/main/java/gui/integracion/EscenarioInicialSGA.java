package gui.integracion;

import gui.integracion.AlmacenSGA.Cuenta;
import gui.integracion.AlmacenSGA.Instantanea;
import gui.integracion.AlmacenSGA.Orden;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.ConexionDTO;
import gui.integracion.dto.EmpleadoDTO;
import gui.integracion.dto.HuespedDTO;
import gui.integracion.dto.MovimientoFolioDTO;
import gui.integracion.dto.OcupanteDTO;
import gui.integracion.dto.Rol;
import gui.integracion.dto.SolicitudExternaDTO;
import gui.integracion.dto.TemporadaDTO;
import gui.integracion.dto.UsuarioDTO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Escenario de prueba inicial, leído del archivo plano
 * {@code src/main/resources/datos/escenario_inicial.txt}: apartamentos,
 * temporadas, huéspedes, favoritos, empleados, cuentas, reservas con su folio,
 * solicitudes del canal externo, cola de preparación y pasillos cerrados. Los
 * pasillos del edificio los arma {@code ConstructorEdificio} a partir de los
 * bloques y pisos.
 * <p>
 * Se carga la primera vez que se abre la aplicación y cada vez que se pulsa
 * «Cargar escenario». El formato de cada línea está explicado en el propio archivo.
 * <p>
 * Cuentas de prueba, todas con la clave {@value #CLAVE_PRUEBA}:
 * {@code huesped@demo.co}, {@code recepcion@demo.co} y {@code admin@demo.co}.
 */
public final class EscenarioInicialSGA {

    public static final String CLAVE_PRUEBA = "1234";
    static final String RECURSO = "/datos/escenario_inicial.txt";

    private EscenarioInicialSGA() {
    }

    static Instantanea crear() {
        try (InputStream entrada = EscenarioInicialSGA.class.getResourceAsStream(RECURSO)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontró el escenario inicial " + RECURSO + ".");
            }
            return leer(new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el escenario inicial: " + e.getMessage(), e);
        }
    }

    /** Lee el escenario línea por línea; un error indica la línea y el motivo. */
    static Instantanea leer(BufferedReader lector) throws IOException {
        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();
        List<ApartaSuiteDTO> suites = new ArrayList<>();
        List<HuespedDTO> huespedes = new ArrayList<>();
        Map<String, List<String>> favoritos = new LinkedHashMap<>();
        Map<String, Cuenta> cuentas = new LinkedHashMap<>();
        List<ConectorSGAHibrido.Reserva> reservas = new ArrayList<>();
        List<TemporadaDTO> temporadas = new ArrayList<>();
        List<EmpleadoDTO> empleados = new ArrayList<>();
        List<SolicitudExternaDTO> solicitudes = new ArrayList<>();
        List<Orden> preparacion = new ArrayList<>();
        List<Long> minutos = new ArrayList<>();
        List<ConexionDTO> cerrados = new ArrayList<>();
        int consecutivo = 1000;

        String linea;
        int numero = 0;
        while ((linea = lector.readLine()) != null) {
            numero++;
            String texto = linea.strip();
            if (texto.isEmpty() || texto.startsWith("#")) {
                continue;
            }
            String[] c = texto.split("\\|", -1);
            try {
                switch (c[0]) {
                    case "APARTAMENTO" -> suites.add(new ApartaSuiteDTO(c[1], c[2], c[3], entero(c[4]),
                            entero(c[5]), entero(c[6]), decimal(c[7]), decimal(c[8]), decimal(c[9]),
                            lista(c[13]), c[12], c[10], null, c[11]));
                    case "TEMPORADA" -> temporadas.add(new TemporadaDTO(c[1], LocalDate.parse(c[2]),
                            LocalDate.parse(c[3])));
                    case "HUESPED" -> huespedes.add(new HuespedDTO(c[1], c[2], fecha(c[3]), vacio(c[4]),
                            vacio(c[5])));
                    case "FAVORITO" -> favoritos.computeIfAbsent(c[1], d -> new ArrayList<>()).add(c[2]);
                    case "EMPLEADO" -> empleados.add(new EmpleadoDTO(c[1], c[2], c[3]));
                    case "CUENTA" -> cuentas.put(c[1].toLowerCase(), new Cuenta(c[2],
                            new UsuarioDTO(c[1].toLowerCase(), c[4], Rol.valueOf(c[3]), c[5])));
                    case "RESERVA" -> reservas.add(reserva("R-" + (++consecutivo), c, hoy, huespedes));
                    case "MOVIMIENTO" -> {
                        if (reservas.isEmpty()) {
                            throw new IllegalArgumentException("MOVIMIENTO sin una RESERVA antes");
                        }
                        MovimientoFolioDTO m = new MovimientoFolioDTO(c[1], c[2], decimal(c[3]), ahora);
                        reservas.getLast().movimientos.add(m);
                        reservas.getLast().deshacibles.add(m);
                    }
                    case "SOLICITUD" -> solicitudes.add(new SolicitudExternaDTO(c[1], c[2], c[3], c[4],
                            hoy.plusDays(entero(c[5])), hoy.plusDays(entero(c[6])), entero(c[7]),
                            ahora.minusMinutes(entero(c[8]))));
                    case "PREPARACION" -> preparacion.add(new Orden(c[1], ahora.minusHours(entero(c[2]))));
                    case "TIEMPO_PREPARACION" -> minutos.add((long) entero(c[1]));
                    case "PASILLO_CERRADO" -> cerrados.add(new ConexionDTO(c[1], c[2], 0, false));
                    default -> throw new IllegalArgumentException("tipo de registro desconocido «" + c[0] + "»");
                }
            } catch (RuntimeException e) {
                throw new IllegalStateException("Escenario inicial, línea " + numero + ": " + e.getMessage(), e);
            }
        }
        return new Instantanea(suites, huespedes, favoritos, cuentas, reservas, temporadas, empleados, null,
                solicitudes, new ArrayList<>(), preparacion, new ArrayList<>(), minutos, cerrados, consecutivo);
    }

    /** El total y el cargo de alojamiento los calcula el conector al cargar (con las temporadas). */
    private static ConectorSGAHibrido.Reserva reserva(String codigo, String[] c, LocalDate hoy,
                                                      List<HuespedDTO> huespedes) {
        HuespedDTO titular = huespedes.stream().filter(h -> h.documento().equals(c[1])).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("el titular " + c[1]
                        + " no está registrado (escribe su línea HUESPED antes)"));
        ConectorSGAHibrido.Reserva r = new ConectorSGAHibrido.Reserva();
        r.codigo = codigo;
        r.documentoTitular = titular.documento();
        r.codigoSuite = c[2];
        r.entrada = hoy.plusDays(entero(c[3]));
        r.salida = hoy.plusDays(entero(c[4]));
        r.canal = c[5];
        r.estado = c[6];
        r.horaLlegada = c[8].isBlank() ? null : LocalTime.parse(c[8]);
        r.ocupantes.add(new OcupanteDTO(titular.nombre(), titular.documento(), titular.fechaNacimiento()));
        for (int i = 2; i <= entero(c[7]); i++) {
            r.ocupantes.add(new OcupanteDTO("Acompañante " + i, null, null));
        }
        return r;
    }

    /** Acepta "+20", "-30" y "0". */
    private static int entero(String texto) {
        return Integer.parseInt(texto.strip().replace("+", ""));
    }

    private static double decimal(String texto) {
        return Double.parseDouble(texto.strip());
    }

    private static LocalDate fecha(String texto) {
        return texto.isBlank() ? null : LocalDate.parse(texto.strip());
    }

    private static String vacio(String texto) {
        return texto.isBlank() ? null : texto.strip();
    }

    private static List<String> lista(String texto) {
        return texto.isBlank() ? List.of()
                : Arrays.stream(texto.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }
}
