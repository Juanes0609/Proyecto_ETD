package servicio;


import estructuras.ArbolBinarioBusqueda;
import estructuras.NodoArbol;
import modelo.Huesped;

public class ServicioHuespedes {

    private final ArbolBinarioBusqueda<Huesped> huespedes;

    public ServicioHuespedes() {
        huespedes = new ArbolBinarioBusqueda<>();
    }

    // Registrar un huésped.
    public boolean registrarHuesped(Huesped huesped) {
        if (huesped == null) {
            throw new IllegalArgumentException(
                    "El huésped no puede ser nulo."
            );
        }

        if (huesped.getDocumento() == null
                || huesped.getDocumento().isBlank()) {
            throw new IllegalArgumentException(
                    "El documento es obligatorio."
            );
        }

        if (buscarPorDocumento(huesped.getDocumento()) != null) {
            return false;
        }

        huespedes.insertar(huesped);
        return true;
    }

    // Buscar un huésped por su documento.
    public Huesped buscarPorDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            return null;
        }

        return buscarPorDocumento(huespedes.getRaiz(), documento);
    }

    private Huesped buscarPorDocumento(
            NodoArbol<Huesped> nodo, String documento) {

        if (nodo == null) {
            return null;
        }

        int comparacion = documento.compareTo(
                nodo.getDato().getDocumento()
        );

        if (comparacion == 0) {
            return nodo.getDato();
        }

        if (comparacion < 0) {
            return buscarPorDocumento(nodo.getIzquierdo(), documento);
        }

        return buscarPorDocumento(nodo.getDerecho(), documento);
    }

    // Consultar si un huésped está registrado.
    public boolean existeHuesped(String documento) {
        return buscarPorDocumento(documento) != null;
    }

    // Actualizar los datos de un huésped.
    public boolean actualizarHuesped(
            String documento,
            String nuevoNombre,
            String nuevoTelefono,
            String nuevoCorreo
    ) {
        Huesped huesped = buscarPorDocumento(documento);

        if (huesped == null) {
            return false;
        }

        if (nuevoNombre == null || nuevoNombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        huesped.setNombre(nuevoNombre);
        huesped.setTelefono(nuevoTelefono);
        huesped.setCorreo(nuevoCorreo);

        return true;
    }

    // Eliminar un huésped, siempre que el sistema
    // permita hacerlo según sus reglas de negocio.
    public boolean eliminarHuesped(String documento) {
        Huesped huesped = buscarPorDocumento(documento);

        if (huesped == null) {
            return false;
        }

        // No eliminar si tiene reservas o estancias asociadas.
        // Esta validación debe conectarse con las reglas
        // reales del proyecto antes de habilitar la operación.

        huespedes.eliminar(huesped);
        return true;
    }
}
