package modelo;

import estructuras.ListaCircular;
import estructuras.ListaDoblementeEnlazada;

public class GestorTurnos {
    private final ListaCircular<String> turnos;

    public GestorTurnos() {
        this.turnos = new ListaCircular<>();
    }

    public void agregarRecepcionista(String nombre) {
        turnos.agregar(nombre);
    }

    public String turnoActual() {
        return turnos.getActual();
    }

    /** Pasa al siguiente recepcionista y lo devuelve. */
    public String avanzarTurno() {
        turnos.avanzarTurno();
        return turnos.getActual();
    }

    public void reiniciarTurno() {
        turnos.reiniciarTurno();
    }

    public boolean estaVacio() {
        return turnos.estaVacia();
    }

    public int cantidadRecepcionistas() {
        return turnos.getTam();
    }

    public ListaDoblementeEnlazada<String> listarOrdenTurnos() {
        ListaDoblementeEnlazada<String> lista =
                new ListaDoblementeEnlazada<>();
        for (String nombre : turnos) {
            lista.agregarFinal(nombre);
        }
        return lista;
    }
}
