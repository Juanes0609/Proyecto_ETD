package modelo;

import estructuras.ArbolBinarioBusqueda;
import estructuras.ListaDoblementeEnlazada;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * Catálogo principal del SGA para la gestión de huéspedes y apartamentos.
 * Mantiene los registros indexados por sus claves naturales.
 */
public class SistemaGestionAlojamiento {
    private final ArbolBinarioBusqueda<Huesped> huespedes;
    private final ArbolBinarioBusqueda<Apartamento> apartamentos;

    // Gestores operativos (grafo, colas y lista circular)
    private final GestorMapaEdificio mapa;
    private final GestorPreparacion preparacion;
    private final GestorCanalExterno canalExterno;
    private final GestorTurnos turnos;
    private final GestorEstadisticas estadisticas;

    public SistemaGestionAlojamiento() {
        huespedes = new ArbolBinarioBusqueda<>();
        apartamentos = new ArbolBinarioBusqueda<>();
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

    public void registrarHuesped(Huesped huesped) {
        if (huesped == null) throw new IllegalArgumentException("El huésped es obligatorio");
        if (buscarHuesped(huesped.getDocumento()) != null) {
            throw new IllegalArgumentException("Ya existe un huésped con documento " + huesped.getDocumento());
        }
        huespedes.insertar(huesped);
    }

    public Huesped buscarHuesped(String documento) {
        validarTexto(documento, "documento");
        return huespedes.buscar(new Huesped("", documento, "", LocalDate.of(2000, 1, 1), "", null));
    }

    public void actualizarHuesped(Huesped actualizado) {
        if (actualizado == null) throw new IllegalArgumentException("El huésped es obligatorio");
        Huesped existente = buscarHuesped(actualizado.getDocumento());
        if (existente == null) throw new IllegalArgumentException("No existe el huésped " + actualizado.getDocumento());
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

    public void registrarApartamento(Apartamento apartamento) {
        if (apartamento == null) throw new IllegalArgumentException("El apartamento es obligatorio");
        if (buscarApartamento(apartamento.getCodigo()) != null) {
            throw new IllegalArgumentException("Ya existe un apartamento con código " + apartamento.getCodigo());
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
        if (existente == null) throw new IllegalArgumentException("No existe el apartamento " + actualizado.getCodigo());
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
            if (apartamento.getCapacidadMaxima() >= personas
                    && apartamento.getEstadoOperativo() == EstadoOperativo.PREPARADO
                    && !apartamento.tieneSolapamiento(new Estancia(entrada, salida, null))) {
                insertarOrdenado(resultado, apartamento, orden);
            }
        }
        return resultado;
    }

    private void insertarOrdenado(ListaDoblementeEnlazada<Apartamento> lista,
                                  Apartamento nuevo, Comparator<Apartamento> comparador) {
        if (comparador == null || lista.estaVacio()) {
            lista.agregarFinal(nuevo);
            return;
        }
        ListaDoblementeEnlazada<Apartamento> auxiliar = new ListaDoblementeEnlazada<>();
        boolean insertado = false;
        for (Apartamento actual : lista) {
            if (!insertado && comparador.compare(nuevo, actual) < 0) {
                auxiliar.agregarFinal(nuevo);
                insertado = true;
            }
            auxiliar.agregarFinal(actual);
        }
        if (!insertado) auxiliar.agregarFinal(nuevo);
        while (!lista.estaVacio()) lista.eliminarInicio();
        for (Apartamento apartamento : auxiliar) lista.agregarFinal(apartamento);
    }

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El " + campo + " es obligatorio");
        }
    }

    public int cantidadHuespedes() { return huespedes.size(); }
    public int cantidadApartamentos() { return apartamentos.size(); }
}

