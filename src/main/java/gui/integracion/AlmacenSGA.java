package gui.integracion;

import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.ConexionDTO;
import gui.integracion.dto.EmpleadoDTO;
import gui.integracion.dto.HuespedDTO;
import gui.integracion.dto.SolicitudExternaDTO;
import gui.integracion.dto.TemporadaDTO;
import gui.integracion.dto.UsuarioDTO;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Guarda en disco (archivo serializado) todo lo que se crea o modifica en la
 * aplicación, para que siga ahí la próxima vez que se abra.
 * <p>
 * Las clases del modelo no son serializables, así que se guarda una
 * {@link Instantanea} con los datos en forma de DTO y, al abrir, se vuelve a
 * armar el {@code SistemaGestionAlojamiento} a partir de ella.
 */
final class AlmacenSGA {

    /** Archivo por defecto: {@code ~/.sga-apartasuites/datos.ser}. */
    static final Path ARCHIVO = Path.of(System.getProperty("user.home"), ".sga-apartasuites", "datos.ser");

    private AlmacenSGA() {
    }

    record Cuenta(String clave, UsuarioDTO usuario) implements Serializable {
    }

    /** Orden de preparación: aparta-suite y momento en que entró a la cola. */
    record Orden(String codigoApartaSuite, LocalDateTime ingreso) implements Serializable {
    }

    /** Todos los datos de la aplicación en un momento dado. */
    record Instantanea(
            List<ApartaSuiteDTO> apartaSuites,
            List<HuespedDTO> huespedes,
            Map<String, List<String>> favoritos,
            Map<String, Cuenta> cuentas,
            List<ConectorSGAHibrido.Reserva> reservas,
            List<TemporadaDTO> temporadas,
            List<EmpleadoDTO> empleados,
            String turnoActual,
            List<SolicitudExternaDTO> colaExterna,
            List<String> conflictos,
            List<Orden> pendientesPreparacion,
            List<Orden> enPreparacion,
            List<Long> minutosPreparacion,
            List<ConexionDTO> pasillosCerrados,
            int consecutivoReserva) implements Serializable {
    }

    /** Escribe primero en un archivo temporal para no dejar el archivo a medias si algo falla. */
    static void guardar(Path archivo, Instantanea datos) throws IOException {
        Files.createDirectories(archivo.getParent());
        Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        try (OutputStream salida = Files.newOutputStream(temporal);
             ObjectOutputStream objetos = new ObjectOutputStream(salida)) {
            objetos.writeObject(datos);
        }
        Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /** @return los datos guardados, o {@code null} si no hay archivo o no se puede leer. */
    static Instantanea cargar(Path archivo) {
        if (!Files.exists(archivo)) {
            return null;
        }
        try (InputStream entrada = Files.newInputStream(archivo);
             ObjectInputStream objetos = new ObjectInputStream(entrada)) {
            return (Instantanea) objetos.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            return null; // archivo de una versión anterior o dañado: se parte del escenario inicial
        }
    }
}
