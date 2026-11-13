package servicio;

import estructuras.ArbolBinarioBusqueda;
import estructuras.NodoArbol;
import modelo.Apartamento;
import modelo.EstadoOperativo;

public class ServicioApartamentos {

    private final ArbolBinarioBusqueda<Apartamento> apartamentos;

    public ServicioApartamentos() {
        apartamentos = new ArbolBinarioBusqueda<>();
    }

    // Registrar un apartamento.
    public boolean registrarApartamento(Apartamento apartamento) {
        if (apartamento == null) {
            throw new IllegalArgumentException(
                    "El apartamento no puede ser nulo."
            );
        }

        if (apartamento.getCodigo() == null
                || apartamento.getCodigo().isBlank()) {
            throw new IllegalArgumentException(
                    "El código del apartamento es obligatorio."
            );
        }

        if (buscarPorCodigo(apartamento.getCodigo()) != null) {
            return false;
        }

        return apartamentos.insertar(apartamento);
    }

    // Buscar un apartamento por su código.
    public Apartamento buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }

        return buscarPorCodigo(apartamentos.getRaiz(), codigo);
    }

    private Apartamento buscarPorCodigo(
            NodoArbol<Apartamento> nodo, String codigo) {

        if (nodo == null) {
            return null;
        }

        int comparacion = codigo.compareToIgnoreCase(
                nodo.getDato().getCodigo()
        );

        if (comparacion == 0) {
            return nodo.getDato();
        }

        if (comparacion < 0) {
            return buscarPorCodigo(nodo.getIzquierdo(), codigo);
        }

        return buscarPorCodigo(nodo.getDerecho(), codigo);
    }

    // Consultar si un apartamento está registrado.
    public boolean existeApartamento(String codigo) {
        return buscarPorCodigo(codigo) != null;
    }

    // Actualizar los datos básicos de un apartamento.
    public boolean actualizarApartamento(
            String codigo,
            String nuevoNombre,
            String nuevoBloque,
            int nuevoPiso,
            int nuevosDormitorios,
            int nuevaCapacidad,
            String nuevasCaracteristicas) {

        Apartamento apartamento = buscarPorCodigo(codigo);

        if (apartamento == null) {
            return false;
        }

        if (nuevoNombre == null || nuevoNombre.isBlank()
                || nuevoBloque == null || nuevoBloque.isBlank()
                || nuevosDormitorios < 0
                || nuevaCapacidad <= 0) {
            throw new IllegalArgumentException(
                    "Los datos del apartamento no son válidos."
            );
        }

        apartamento.setNombre(nuevoNombre);
        apartamento.setBloque(nuevoBloque);
        apartamento.setPiso(nuevoPiso);
        apartamento.setNumeroDormitorios(nuevosDormitorios);
        apartamento.setCapacidadMaxima(nuevaCapacidad);
        apartamento.setCaracteristicas(nuevasCaracteristicas);

        return true;
    }

    // Cambiar el estado operativo de un apartamento.
    public boolean cambiarEstado(
            String codigo, EstadoOperativo nuevoEstado) {

        Apartamento apartamento = buscarPorCodigo(codigo);

        if (apartamento == null) {
            return false;
        }

        if (nuevoEstado == null) {
            throw new IllegalArgumentException(
                    "El estado operativo es obligatorio."
            );
        }

        apartamento.setEstadoOperativo(nuevoEstado);
        return true;
    }

    // Actualizar las tarifas del apartamento.
    public boolean actualizarTarifas(
            String codigo, double baja, double media, double alta) {

        Apartamento apartamento = buscarPorCodigo(codigo);

        if (apartamento == null) {
            return false;
        }

        apartamento.actualizarTarifas(baja, media, alta);
        return true;
    }

    // Consultar la cantidad de reservas de un apartamento.
    public int consultarCantidadReservas(String codigo) {
        Apartamento apartamento = buscarPorCodigo(codigo);

        if (apartamento == null) {
            return -1;
        }

        return apartamento.getCantidadReservas();
    }
}