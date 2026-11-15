package modelo;

import estructuras.ListaDoblementeEnlazada;
import excepciones.CapacidadExcedidaException;

import java.util.Comparator;

public final class AsignadorGrupos {
    private final Apartamento[] candidatos;   // ordenados por capacidad descendente
    private final int[] capacidadRestanteDesde; // suma de capacidades de i..fin (para podar)
    private final int personas;

    private Apartamento[] mejor;
    private int mejorCantidad;
    private int mejorCapacidadTotal;

    private AsignadorGrupos(Apartamento[] candidatos, int personas) {
        this.candidatos = candidatos;
        this.personas = personas;
        this.capacidadRestanteDesde = new int[candidatos.length + 1];
        for (int i = candidatos.length - 1; i >= 0; i--) {
            capacidadRestanteDesde[i] = capacidadRestanteDesde[i + 1] + candidatos[i].getCapacidadMaxima();
        }
        this.mejor = null;
        this.mejorCantidad = Integer.MAX_VALUE;
        this.mejorCapacidadTotal = Integer.MAX_VALUE;
    }

    /**
     * @param disponibles apartamentos libres para las fechas (preparados y sin solapamiento)
     * @param personas    total de personas del grupo
     * @return lista con la combinación que usa menos apartamentos
     * @throws CapacidadExcedidaException si ni usando todos los disponibles cabe el grupo
     */
    public static ListaDoblementeEnlazada<Apartamento> asignar(
            ListaDoblementeEnlazada<Apartamento> disponibles, int personas) {
        if (personas <= 0) {
            throw new IllegalArgumentException("El número de personas debe ser mayor que cero");
        }
        if (disponibles == null || disponibles.estaVacio()) {
            throw new CapacidadExcedidaException("No hay apartamentos disponibles para alojar al grupo");
        }

        Apartamento[] arreglo = new Apartamento[disponibles.size()];
        int n = 0;
        for (Apartamento a : disponibles) {
            arreglo[n++] = a;
        }
        ordenar(arreglo, Comparator.comparingInt(Apartamento::getCapacidadMaxima).reversed()
                .thenComparing(Comparator.naturalOrder()));

        AsignadorGrupos asignador = new AsignadorGrupos(arreglo, personas);
        if (asignador.capacidadRestanteDesde[0] < personas) {
            throw new CapacidadExcedidaException("Los apartamentos disponibles no alcanzan para "
                    + personas + " personas (capacidad total: " + asignador.capacidadRestanteDesde[0] + ")");
        }

        asignador.explorar(0, personas, new Apartamento[arreglo.length], 0, 0);

        ListaDoblementeEnlazada<Apartamento> resultado = new ListaDoblementeEnlazada<>();
        for (int i = 0; i < asignador.mejorCantidad; i++) {
            resultado.agregarFinal(asignador.mejor[i]);
        }
        return resultado;
    }

    /**
     * Backtracking: en cada nivel decide si el apartamento i entra o no a la solución.
     *
     * @param indice          apartamento que se está evaluando
     * @param faltantes       personas aún sin alojar
     * @param elegidos        apartamentos elegidos hasta ahora
     * @param cantidad        cuántos se han elegido
     * @param capacidadTotal  suma de capacidades de los elegidos
     */
    private void explorar(int indice, int faltantes, Apartamento[] elegidos, int cantidad, int capacidadTotal) {
        // Caso base: el grupo ya cabe -> candidata a mejor solución
        if (faltantes <= 0) {
            if (cantidad < mejorCantidad
                    || (cantidad == mejorCantidad && capacidadTotal < mejorCapacidadTotal)) {
                mejorCantidad = cantidad;
                mejorCapacidadTotal = capacidadTotal;
                mejor = new Apartamento[cantidad];
                for (int i = 0; i < cantidad; i++) {
                    mejor[i] = elegidos[i];
                }
            }
            return;
        }

        // Podas: no quedan apartamentos, no alcanza la capacidad restante,
        // o ya no se puede mejorar la mejor solución encontrada.
        if (indice >= candidatos.length) return;
        if (capacidadRestanteDesde[indice] < faltantes) return;
        if (cantidad + 1 > mejorCantidad) return;

        Apartamento actual = candidatos[indice];

        // Opción 1: incluir el apartamento
        elegidos[cantidad] = actual;
        explorar(indice + 1, faltantes - actual.getCapacidadMaxima(),
                elegidos, cantidad + 1, capacidadTotal + actual.getCapacidadMaxima());
        elegidos[cantidad] = null; // deshacer (backtrack)

        // Opción 2: no incluirlo
        explorar(indice + 1, faltantes, elegidos, cantidad, capacidadTotal);
    }

    /** Ordenamiento por inserción sobre un arreglo (sin colecciones de java.util). */
    private static void ordenar(Apartamento[] arreglo, Comparator<Apartamento> comparador) {
        for (int i = 1; i < arreglo.length; i++) {
            Apartamento clave = arreglo[i];
            int j = i - 1;
            while (j >= 0 && comparador.compare(arreglo[j], clave) > 0) {
                arreglo[j + 1] = arreglo[j];
                j--;
            }
            arreglo[j + 1] = clave;
        }
    }
}
