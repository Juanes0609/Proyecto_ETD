package modelo;

import estructuras.ColaPrioridad;

import java.time.LocalDateTime;

public class PruebaColaPrioridad {
    public static void main(String[] args) {

        ColaPrioridad<OrdenPreparacion> cola =
                new ColaPrioridad<>(
                        new ComparadorPrioridadPreparacion()
                );

        LocalDateTime ahora = LocalDateTime.now();

        OrdenPreparacion o1 = new OrdenPreparacion(
                "APT101", ahora, ahora.plusHours(3), 2
        );

        OrdenPreparacion o2 = new OrdenPreparacion(
                "APT102", ahora, ahora.plusHours(2), 1
        );

        OrdenPreparacion o3 = new OrdenPreparacion(
                "APT103", ahora, ahora.plusHours(1), 1
        );

        cola.encolar(o1);
        cola.encolar(o2);
        cola.encolar(o3);

        while (!cola.estaVacia()) {
            OrdenPreparacion orden = cola.desencolar();
            System.out.println(
                    orden.getCodigo() + " - Prioridad: "
                            + orden.getPrioridad()
            );
        }
    }
}
