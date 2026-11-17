package modelo;

import estructuras.ListaCircular;
import estructuras.ListaDoblementeEnlazada;

/** Rotación de turnos de recepción sobre la lista circular propia. */
public class GestorTurnos {
    private final ListaCircular<Recepcionista> turnos;

    public GestorTurnos() {
        this.turnos = new ListaCircular<>();
    }

    /** El recepcionista nuevo entra al final de la rotación. */
    public void agregarRecepcionista(Recepcionista recepcionista) {
        if (recepcionista == null) {
            throw new IllegalArgumentException("El recepcionista es obligatorio");
        }
        turnos.agregar(recepcionista);
    }

    /** @throws excepciones.EstructuraVaciaException si no hay recepcionistas */
    public Recepcionista turnoActual() {
        return turnos.getActual();
    }

    /** Pasa al siguiente recepcionista y lo devuelve (después del último vuelve al primero). */
    public Recepcionista avanzarTurno() {
        turnos.avanzar();
        return turnos.getActual();
    }

    public void reiniciarTurno() {
        turnos.reiniciar();
    }

    public boolean estaVacio() {
        return turnos.estaVacia();
    }

    public int cantidadRecepcionistas() {
        return turnos.getTam();
    }

    /** Una vuelta de la rotación, desde el primero. */
    public ListaDoblementeEnlazada<Recepcionista> listarOrdenTurnos() {
        ListaDoblementeEnlazada<Recepcionista> lista = new ListaDoblementeEnlazada<>();
        for (Recepcionista r : turnos) {
            lista.agregarFinal(r);
        }
        return lista;
    }
}
