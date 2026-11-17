package modelo;

import estructuras.ArbolBinarioBusqueda;
import estructuras.ListaDoblementeEnlazada;
import excepciones.FechasInvalidasException;
import excepciones.RegistroDuplicadoException;
import excepciones.RegistroNoEncontradoException;
import servicio.ServicioFacturacion;
import servicio.ServicioReservas;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * Catálogo principal del SGA. Huéspedes, apartamentos y empleados viven en
 * árboles binarios de búsqueda indexados por su clave natural (documento o
 * código), así que buscarlos es O(altura del árbol). También reúne las
 * temporadas, las reservas y los gestores operativos.
 */
public class SistemaGestionAlojamiento {
    private final ArbolBinarioBusqueda<Huesped> huespedes;
    private final ArbolBinarioBusqueda<Apartamento> apartamentos;
    private final ArbolBinarioBusqueda<Recepcionista> empleados;

    /** Temporadas definidas; las noches que no caen en ninguna son BAJA (temporada base). */
    private final ListaDoblementeEnlazada<Temporada> temporadas;
    private final ServicioFacturacion facturacion;
    private final ServicioReservas reservas;

    // Gestores operativos (grafo, colas y lista circular)
    private final GestorMapaEdificio mapa;
    private final GestorPreparacion preparacion;
    private final GestorCanalExterno canalExterno;
    private final GestorTurnos turnos;
    private final GestorEstadisticas estadisticas;

    public SistemaGestionAlojamiento() {
        huespedes = new ArbolBinarioBusqueda<>();
        apartamentos = new ArbolBinarioBusqueda<>();
        empleados = new ArbolBinarioBusqueda<>();
        temporadas = new ListaDoblementeEnlazada<>();
        // La facturación lee la lista viva de temporadas: las nuevas se aplican de inmediato.
        facturacion = new ServicioFacturacion(temporadas, 0, 0, 0);
        reservas = new ServicioReservas(facturacion);
        mapa = new GestorMapaEdificio();
        preparacion = new GestorPreparacion(this);
        canalExterno = new GestorCanalExterno(this);
        turnos = new GestorTurnos();
        estadisticas = new GestorEstadisticas(this, preparacion, canalExterno, mapa);
    }

    public GestorMapaEdificio getMapa() { return mapa; }
    public GestorPreparacion getPreparacion() { return preparacion; }
    public GestorCanalExterno getCanalExterno() { return canalExterno; }
    public GestorTurnos getTurnos() { return turnos; }
    public GestorEstadisticas getEstadisticas() { return estadisticas; }
    public ServicioReservas getReservas() { return reservas; }
    public ServicioFacturacion getFacturacion() { return facturacion; }

    // ---------- Temporadas ----------

    /**
     * Agrega una temporada. Las fechas son inclusivas y no puede cruzarse con
     * otra ya definida.
     */
    public void agregarTemporada(Temporada temporada) {
        if (temporada == null || temporada.getFechaInicio() == null || temporada.getFechaFin() == null
                || temporada.getTipoTemporada() == null) {
            throw new IllegalArgumentException("La temporada necesita tipo, inicio y fin");
        }
        if (temporada.getFechaFin().isBefore(temporada.getFechaInicio())) {
            throw new FechasInvalidasException("La fecha final de la temporada es anterior a la inicial");
        }
        for (Temporada t : temporadas) {
            boolean cruza = !temporada.getFechaFin().isBefore(t.getFechaInicio())
                    && !temporada.getFechaInicio().isAfter(t.getFechaFin());
            if (cruza) {
                throw new FechasInvalidasException("La temporada se cruza con otra ya definida ("
                        + t.getFechaInicio() + " a " + t.getFechaFin() + ")");
            }
        }
        temporadas.agregarFinal(temporada);
    }

    public ListaDoblementeEnlazada<Temporada> listarTemporadas() {
        return temporadas;
    }

    public void limpiarTemporadas() {
        temporadas.limpiar();
    }

    /** Tipo de temporada de una noche (BAJA si no cae en ninguna definida). */
    public TipoTemporada temporadaDe(LocalDate noche) {
        return facturacion.temporadaDe(noche);
    }

    // ---------- Empleados ----------

    /** Registra al recepcionista en el árbol y lo agrega al final de la rotación de turnos. */
    public void registrarEmpleado(Recepcionista empleado) {
        if (empleado == null) throw new IllegalArgumentException("El empleado es obligatorio");
        if (buscarEmpleado(empleado.getDocumento()) != null) {
            throw new RegistroDuplicadoException("Ya existe un empleado con documento " + empleado.getDocumento());
        }
        empleados.insertar(empleado);
        turnos.agregarRecepcionista(empleado);
    }

    public Recepcionista buscarEmpleado(String documento) {
        validarTexto(documento, "documento");
        return empleados.buscar(new Recepcionista("búsqueda", documento, "", ""));
    }

    /** Empleados ordenados por documento (recorrido in-orden del árbol). */
    public ListaDoblementeEnlazada<Recepcionista> listarEmpleados() {
        ListaDoblementeEnlazada<Recepcionista> lista = new ListaDoblementeEnlazada<>();
        empleados.recorridoInOrden(lista::agregarFinal);
        return lista;
    }

    // ---------- Huéspedes ----------

    public void registrarHuesped(Huesped huesped) {
        if (huesped == null) throw new IllegalArgumentException("El huésped es obligatorio");
        if (buscarHuesped(huesped.getDocumento()) != null) {
            throw new RegistroDuplicadoException("Ya existe un huésped con documento " + huesped.getDocumento());
        }
        huespedes.insertar(huesped);
    }

    public Huesped buscarHuesped(String documento) {
        validarTexto(documento, "documento");
        // Huésped "llave": solo importa el documento (el árbol compara por documento),
        // pero el constructor exige un nombre no vacío.
        return huespedes.buscar(new Huesped("búsqueda", documento, "", LocalDate.of(2000, 1, 1), "", null));
    }

    public void actualizarHuesped(Huesped actualizado) {
        if (actualizado == null) throw new IllegalArgumentException("El huésped es obligatorio");
        Huesped existente = buscarHuesped(actualizado.getDocumento());
        if (existente == null) throw new RegistroNoEncontradoException("No existe el huésped " + actualizado.getDocumento());
        huespedes.eliminar(existente);
        huespedes.insertar(actualizado);
    }

    public boolean eliminarHuesped(String documento) {
        Huesped existente = buscarHuesped(documento);
        return existente != null && huespedes.eliminar(existente);
    }

    public ListaDoblementeEnlazada<Huesped> listarHuespedes() {
        ListaDoblementeEnlazada<Huesped> lista = new ListaDoblementeEnlazada<>();

        huespedes.recorridoInOrden(huesped -> lista.agregarFinal(huesped));

        return lista;
    }

    // ---------- Apartamentos ----------

    public void registrarApartamento(Apartamento apartamento) {
        if (apartamento == null) throw new IllegalArgumentException("El apartamento es obligatorio");
        if (buscarApartamento(apartamento.getCodigo()) != null) {
            throw new RegistroDuplicadoException("Ya existe un apartamento con código " + apartamento.getCodigo());
        }
        if (apartamento.getCapacidadMaxima() <= 0) {
            throw new IllegalArgumentException("La capacidad máxima debe ser mayor que cero");
        }
        apartamentos.insertar(apartamento);
    }

    public Apartamento buscarApartamento(String codigo) {
        validarTexto(codigo, "código");
        return apartamentos.buscar(new Apartamento(codigo, "", "", 0, 0, 1,
                EstadoOperativo.PREPARADO, "", ""));
    }

    public void actualizarApartamento(Apartamento actualizado) {
        if (actualizado == null) throw new IllegalArgumentException("El apartamento es obligatorio");
        Apartamento existente = buscarApartamento(actualizado.getCodigo());
        if (existente == null) throw new RegistroNoEncontradoException("No existe el apartamento " + actualizado.getCodigo());
        if (actualizado.getCapacidadMaxima() <= 0) {
            throw new IllegalArgumentException("La capacidad máxima debe ser mayor que cero");
        }
        apartamentos.eliminar(existente);
        apartamentos.insertar(actualizado);
    }

    public boolean eliminarApartamento(String codigo) {
        Apartamento existente = buscarApartamento(codigo);
        return existente != null && apartamentos.eliminar(existente);
    }

    public ListaDoblementeEnlazada<Apartamento> listarApartamentos() {
        ListaDoblementeEnlazada<Apartamento> lista = new ListaDoblementeEnlazada<>();

        apartamentos.recorridoInOrden(apartamento -> lista.agregarFinal(apartamento));

        return lista;
    }

    public ListaDoblementeEnlazada<Apartamento> buscarDisponibles(
            LocalDate entrada, LocalDate salida, int personas, Comparator<Apartamento> orden) {
        if (entrada == null || salida == null || !salida.isAfter(entrada)) {
            throw new IllegalArgumentException("El rango de fechas no es válido");
        }
        if (personas <= 0) throw new IllegalArgumentException("El número de personas debe ser mayor que cero");

        ListaDoblementeEnlazada<Apartamento> resultado = new ListaDoblementeEnlazada<>();
        for (Apartamento apartamento : apartamentos) {
            // El estado operativo es el de hoy: un apartamento ocupado o en preparación
            // hoy puede estar libre en otras fechas. Solo se descartan los fuera de servicio.
            if (apartamento.getCapacidadMaxima() >= personas
                    && apartamento.getEstadoOperativo() != EstadoOperativo.FUERASERVICIO
                    && !apartamento.tieneSolapamiento(new Estancia(entrada, salida, null))) {
                insertarOrdenado(resultado, apartamento, orden);
            }
        }
        return resultado;
    }
    public ListaDoblementeEnlazada<Apartamento> asignarGrupoGrande(
            LocalDate entrada, LocalDate salida, int personas) {
        if (entrada == null || salida == null || !salida.isAfter(entrada)) {
            throw new IllegalArgumentException("El rango de fechas no es válido");
        }

        ListaDoblementeEnlazada<Apartamento> libres = new ListaDoblementeEnlazada<>();
        Estancia estancia = new Estancia(entrada, salida, null);
        for (Apartamento apartamento : apartamentos) {
            if (apartamento.getEstadoOperativo() != EstadoOperativo.FUERASERVICIO
                    && !apartamento.tieneSolapamiento(estancia)) {
                libres.agregarFinal(apartamento);
            }
        }
        return AsignadorGrupos.asignar(libres, personas);
    }

    /** Sin comparador queda en orden de código; con él, ordenada por ese criterio. */
    private void insertarOrdenado(ListaDoblementeEnlazada<Apartamento> lista,
                                  Apartamento nuevo, Comparator<Apartamento> comparador) {
        if (comparador == null) {
            lista.agregarFinal(nuevo);
        } else {
            lista.insertarOrdenado(nuevo, comparador);
        }
    }

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El " + campo + " es obligatorio");
        }
    }

    public int cantidadHuespedes() { return huespedes.size(); }
    public int cantidadApartamentos() { return apartamentos.size(); }
    public int cantidadEmpleados() { return empleados.size(); }
}

