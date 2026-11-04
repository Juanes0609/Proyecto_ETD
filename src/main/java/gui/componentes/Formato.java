package gui.componentes;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatos de presentación en español de Colombia (moneda, fechas y horas). */
public final class Formato {

    private static final Locale ES_CO = Locale.of("es", "CO");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("d MMM", ES_CO);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d MMM yyyy", ES_CO);
    private static final DateTimeFormatter DIA_SEMANA = DateTimeFormatter.ofPattern("EEE d MMM", ES_CO);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("d MMM · HH:mm", ES_CO);

    private Formato() {
    }

    /** 95000 → "$95.000". */
    public static String moneda(double valor) {
        NumberFormat formato = NumberFormat.getIntegerInstance(ES_CO);
        return (valor < 0 ? "−$" : "$") + formato.format(Math.round(Math.abs(valor)));
    }

    /** "15 nov" (meses en minúscula, sin punto). */
    public static String diaMes(LocalDate fecha) {
        return limpiar(DIA_MES.format(fecha));
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "—" : limpiar(FECHA.format(fecha));
    }

    public static String diaSemana(LocalDate fecha) {
        return limpiar(DIA_SEMANA.format(fecha));
    }

    public static String hora(LocalTime hora) {
        return hora == null ? "—" : HORA.format(hora);
    }

    public static String fechaHora(LocalDateTime fechaHora) {
        return limpiar(FECHA_HORA.format(fechaHora));
    }

    /** "15–18 nov" o "28 nov – 2 dic". */
    public static String rango(LocalDate entrada, LocalDate salida) {
        if (entrada.getMonth() == salida.getMonth() && entrada.getYear() == salida.getYear()) {
            return entrada.getDayOfMonth() + "–" + diaMes(salida);
        }
        return diaMes(entrada) + " – " + diaMes(salida);
    }

    public static String plural(long cantidad, String singular, String plural) {
        return cantidad + " " + (cantidad == 1 ? singular : plural);
    }

    /** Convierte un nombre de enum en texto legible: "FUERA_DE_SERVICIO" → "Fuera de servicio". */
    public static String estado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "—";
        }
        String texto = estado.replace('_', ' ').toLowerCase(Locale.ROOT)
                .replace("preparacion", "preparación")
                .replace("anulacion", "anulación");
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static String limpiar(String texto) {
        return texto.replace(".", "");
    }
}
