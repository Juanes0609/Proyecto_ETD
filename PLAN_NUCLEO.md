# Plan de correcciones del núcleo (fuera de `gui/`)

Revisión del 14 de noviembre de 2026 contra el enunciado del proyecto. La GUI ya quedó
ajustada; esto es lo que falta en `estructuras/`, `modelo/` y `servicio/`. Cada punto dice
qué cambiar, dónde y por qué lo pide el enunciado.

Orden sugerido: primero la sección 1 (es lo único que falta por completo), luego la 2
(reglas que el profesor puede revisar en la sustentación) y al final la 3 (limpieza).

---

## 1. Lo que falta por crear

### 1.1 Asignación de grupos grandes con backtracking (obligatorio, no existe)

El enunciado pide: cuando un grupo no cabe en un solo apartamento, buscar con
recursividad y backtracking la combinación de apartamentos disponibles que aloje a todos
usando el menor número de apartamentos.

Crear `servicio/AsignadorGrupos.java`:

```java
package servicio;

import estructuras.ListaDoblementeEnlazada;
import modelo.Apartamento;

public class AsignadorGrupos {

    private ListaDoblementeEnlazada<Apartamento> mejor;   // mejor combinación encontrada

    /** @return la combinación con menos apartamentos, o una lista vacía si el grupo no cabe. */
    public ListaDoblementeEnlazada<Apartamento> asignar(
            ListaDoblementeEnlazada<Apartamento> disponibles, int personas) {
        mejor = null;
        buscar(disponibles, 0, personas, new ListaDoblementeEnlazada<>());
        return mejor == null ? new ListaDoblementeEnlazada<>() : mejor;
    }

    private void buscar(ListaDoblementeEnlazada<Apartamento> disponibles, int indice,
                        int faltan, ListaDoblementeEnlazada<Apartamento> elegidos) {
        if (faltan <= 0) {                                   // caso base: ya caben todos
            if (mejor == null || elegidos.getTam() < mejor.getTam()) {
                mejor = copiar(elegidos);
            }
            return;
        }
        if (indice == disponibles.getTam()) return;          // no quedan apartamentos
        if (mejor != null && elegidos.getTam() + 1 >= mejor.getTam()) return;   // poda

        Apartamento actual = disponibles.obtener(indice);
        elegidos.agregarFinal(actual);                       // elegir
        buscar(disponibles, indice + 1, faltan - actual.getCapacidadMaxima(), elegidos);
        elegidos.eliminarFinal();                            // deshacer (backtracking)
        buscar(disponibles, indice + 1, faltan, elegidos);   // probar sin él
    }

    private ListaDoblementeEnlazada<Apartamento> copiar(ListaDoblementeEnlazada<Apartamento> origen) {
        ListaDoblementeEnlazada<Apartamento> copia = new ListaDoblementeEnlazada<>();
        for (Apartamento a : origen) copia.agregarFinal(a);
        return copia;
    }
}
```

La lista `disponibles` sale de `SistemaGestionAlojamiento.buscarDisponibles(entrada, salida, 1, ...)`
(ver 2.1). Cuando exista, avisar a Juan: la GUI agrega la opción "grupo grande" en Reservar y
crea una reserva por cada apartamento de la combinación.

### 1.2 Empleados en árbol de búsqueda

El enunciado dice "administrar empleados, apartamentos y huéspedes mediante árboles de
búsqueda". Hoy `Recepcionista` no se usa y `GestorTurnos` guarda solo `String`.

- `Recepcionista implements Comparable<Recepcionista>` comparando por documento, con
  getter de `codigoEmpleado` y un campo `horario`.
- En `SistemaGestionAlojamiento`: `ArbolBinarioBusqueda<Recepcionista> empleados` con
  `registrarEmpleado`, `buscarEmpleado(documento)`, `listarEmpleados()`.
- `GestorTurnos`: cambiar `ListaCircular<String>` por `ListaCircular<Recepcionista>`.

Mientras tanto la GUI guarda los empleados en un `ArbolBinarioBusqueda<EmpleadoDTO>` propio.

### 1.3 Reservas y folios accesibles desde `SistemaGestionAlojamiento`

`ServicioReservas` y `ServicioFacturacion` no están conectados a `SistemaGestionAlojamiento`,
por eso la GUI lleva las reservas por su cuenta y solo las refleja en el modelo. Para que
la GUI delegue por completo hace falta:

- `SistemaGestionAlojamiento` debe crear y exponer `getReservas()` (un `ServicioReservas`)
  y guardar las temporadas (`ListaDoblementeEnlazada<Temporada>` con `agregarTemporada` y
  validación de que no se crucen).
- `ServicioReservas` debe poder buscar una reserva por código (`buscarReserva(codigo)`) y
  listar todas; hoy solo las devuelve al crearlas.
- `Reserva` necesita `horaLlegadaEstimada` (`LocalTime`): la usa la cola de preparación.
- `Ocupante` necesita `documento` (opcional).

---

## 2. Errores de lógica

### 2.1 `SistemaGestionAlojamiento.buscarDisponibles` descarta apartamentos libres
Exige `getEstadoOperativo() == PREPARADO`. Un apartamento ocupado hoy sí está libre el mes
entrante. Cambiar la condición por `!= EstadoOperativo.FUERASERVICIO`.

### 2.2 `ServicioReservas.crearReserva` cobra con las tarifas globales
En el paso 6 usa `facturacion.liquidar(...)`. Debe ser
`facturacion.paraApartamento(apartamento).liquidar(...)`, igual que ya hace `pagarAnticipo`.
Hoy el cargo y el anticipo se calculan con tarifas distintas. (El commit `26dcc1e` cambió
solo `pagarAnticipo`; `crearReserva` sigue con las tarifas globales.)

### 2.3 `ServicioReservas.finalizar` no libera el árbol
`cancelar` y `marcarNoShow` llaman `liberarNoches`, `finalizar` no. La reserva finalizada
se queda en `reservasPorFecha` del apartamento. Agregar `liberarNoches(reserva)`.

### 2.4 Solapamiento sin aprovechar el orden del árbol
`Apartamento.tieneSolapamiento` usa `existeSi`, que recorre todo el árbol. El enunciado pide
resolverlo "recorriendo el árbol de forma recursiva, sin revisar todas las reservas".
Reemplazar por un recorrido con poda (el árbol está ordenado por fecha de entrada):

```java
public boolean tieneSolapamiento(Estancia nueva) {
    if (nueva == null) throw new IllegalArgumentException("La estancia es obligatoria");
    return choca(reservasPorFecha.getRaiz(), nueva);
}

private boolean choca(NodoArbol<Reserva> nodo, Estancia nueva) {
    if (nodo == null) return false;
    Estancia otra = nodo.getDato().getEstancia();
    EstadoReserva estado = nodo.getDato().getEstadoReserva();
    boolean activa = estado != null && estado.esActiva();   // igual que hoy: sin estado no cuenta
    if (activa && nueva.getFechaEntrada().isBefore(otra.getFechaSalida())
               && nueva.getFechaSalida().isAfter(otra.getFechaEntrada())) {
        return true;
    }
    // A la izquierda están las que entran antes: una estancia larga aún puede chocar.
    if (choca(nodo.getIzquierdo(), nueva)) return true;
    // A la derecha todas entran después que esta: si esta ya entra cuando la nueva
    // salió, ninguna de la derecha puede chocar (poda).
    if (!otra.getFechaEntrada().isBefore(nueva.getFechaSalida())) return false;
    return choca(nodo.getDerecho(), nueva);
}
```

### 2.5 `Reserva.compareTo` solo compara la fecha de entrada
El árbol no admite duplicados: dos reservas con la misma fecha de entrada se consideran
"la misma" y `eliminar` puede borrar la equivocada. Desempatar por código:

```java
int c = estancia.getFechaEntrada().compareTo(otra.estancia.getFechaEntrada());
return c != 0 ? c : codigo.compareTo(otra.codigo);
```

### 2.6 `ListaDoblementeEnlazada.eliminar` compara con `==`
Debe usar `equals` (como ya hace `contiene`). Hoy `Conjunto.eliminar` falla si se le pasa
otra instancia del mismo apartamento, aunque `equals` diga que son iguales.

### 2.7 Fuera de servicio sin motivo obligatorio
Agregar en `Apartamento` un método `ponerFueraDeServicio(String motivo)` que lance
`IllegalArgumentException` si el motivo está vacío y `TransicionEstadoInvalidaException` si
está ocupado o en preparación, y `volverAServicio()`. La GUI ya valida esto, pero la regla
debe vivir en el modelo.

### 2.8 `GestorCanalExterno` crea reservas incompletas
Las reservas aceptadas quedan con `titular = null`, sin ocupantes y sin cargo en el folio.
`SolicitudExterna` necesita `nombreHuesped` y `documentoHuesped`, y `procesarSiguiente` debería
crear la reserva con `ServicioReservas.crearReserva(Canal.EXTERNA, ...)` y registrar el
conflicto cuando ese método lance `ReservaSolapadaException`, `CapacidadExcedidaException`
o `FechasInvalidasException`. Por esto la GUI procesa hoy la cola externa por su cuenta.

### 2.9 `Folio`: deshacer y reversos
- `operaciones` apila también el cargo inicial de alojamiento, así que "deshacer" puede
  revertir la estancia completa. Apilar solo los movimientos registrados en recepción
  (un parámetro `deshacible` en `registrar`).
- `GestorEstadisticas.ingresosDe` reconoce un reverso porque el concepto empieza por
  "Reverso". Agregar `boolean esReverso` a `MovimientoFolio` y usarlo.

### 2.10 Ingresos "por periodo"
`GestorEstadisticas.calcularReservasEIngresos` suma todos los pagos sin mirar fechas. Debe
filtrar por `MovimientoFolio.getFechaHora()` dentro de `[desde, hasta)`.

---

## 3. Limpieza y reglas del enunciado

### 3.1 `java.util` en el núcleo (prohibido por el enunciado)
| Archivo | Qué quitar |
|---|---|
| `modelo/Estancia.java` | `List<DetalleNoche> listDetalleNoche` → `ListaDoblementeEnlazada<DetalleNoche>` |
| `modelo/Temporada.java` | `List<DetalleNoche> listaDetalleNoche` → borrar (no se usa) |
| `modelo/Huesped.java`, `modelo/Vertice.java` | `import java.util.List` sin usar |
| `modelo/GestorSolicitudesExternas.java` | toda la clase (ver 3.3) |
| `modelo/PruebasGestorMapaEdificio.java` | `ArrayList`/`List` → `ListaDoblementeEnlazada` |

`Iterator`, `Comparator` y `NoSuchElementException` sí se pueden usar: no son colecciones.

### 3.2 Estructuras atadas al dominio o mal ubicadas
- `Grafo` importa `modelo.Vertice` y `modelo.Arista`. Mover esas dos clases a `estructuras/`
  y ajustar los imports (`GestorMapaEdificio`, pruebas; Juan ajusta `ConectorSGAProveedor`).
- `ListaCircular`: los mensajes dicen "No hay recepcionistas registrados". Cambiar a
  "La lista circular está vacía" y renombrar `avanzarTurno`/`reiniciarTurno` a
  `avanzar`/`reiniciar`.
- `ColaPrioridad` mueve nodos de la lista con `getInicio`, `setTam`, etc. Mover esa lógica
  a `ListaDoblementeEnlazada.insertarOrdenado(T dato, Comparator<T> c)` y quitar
  `setInicio`, `setFin` y `setTam` de la lista.
- `ListaDoblementeEnlazada` tiene `size()`, `getTam()`, `isEmpty()` y `estaVacio()`: dejar
  un solo nombre para cada cosa.

### 3.3 Clases duplicadas o sin uso (borrar)
| Clase | Motivo |
|---|---|
| `estructuras/ListaDoble` | duplica `ListaDoblementeEnlazada`; nadie la usa |
| `modelo/EstadoApartamento` | duplica `EstadoOperativo` |
| `modelo/CanalReserva` | duplica `Canal` |
| `modelo/GestorSolicitudesExternas` | duplica `GestorCanalExterno` y usa `java.util.List` |
| `modelo/ComparadorApartamentoPorPrecio`, `...PorCapacidad`, `...PorNumeroReservas`, `ComparadorPrioridadPreparacion` | duplican `ComparadoresApartamento` y el comparador de `GestorPreparacion` |
| `servicio/ServicioApartamentos`, `servicio/ServicioHuespedes` | duplican el catálogo de `SistemaGestionAlojamiento` |
| `excepciones/RegistroDuplicadoException`, `RegistroNoEncontradoException` | sin uso: o se borran, o se usan en `SistemaGestionAlojamiento` en vez de `IllegalArgumentException` |

`ComparadorPrioridadPreparacion` lo usa `modelo/PruebaColaPrioridad`: al borrarlo, cambiar esa
prueba para que use el comparador de `GestorPreparacion` (o borrarla también).

Antes de borrar los comparadores, confirmar que quedan los cuatro que exige el enunciado en
`ComparadoresApartamento` (precio, capacidad, número de reservas) más el de la cola de
preparación con `thenComparing` en `GestorPreparacion`: sí están.

### 3.4 Otros
- `README.md`: menciona `comparadores/`, `algoritmos/`, `persistencia/` y
  `escenario_inicial.txt`, que no existen, y ubica `SistemaGestionAlojamiento` en `servicio/`.
- Las clases `Prueba*` con `main` están dentro de `modelo/`; moverlas a un paquete `pruebas/`.
- Commits: usar `tipo(ambito): descripcion` (`feat(modelo): ...`, `fix(servicio): ...`).
  `(feat) ...`, `Update` y `Ajustes E.D` no son Conventional Commits. Revisar también el
  mínimo de commits por integrante que pida el profesor.

---

## 4. Qué hace hoy la GUI con el núcleo (para la sustentación)

| Función | Estructura o servicio del núcleo que usa |
|---|---|
| Solapamiento de reservas | `Apartamento.tieneSolapamiento` (árbol por fecha de entrada) |
| Cambios de estado de la reserva | `EstadoReserva.puedeTransicionarA` |
| Cotización y valor de la estancia | `ServicioFacturacion.desglosar` (iterador de `Estancia`) y `liquidar` (recursivo) |
| Búsqueda de disponibilidad ordenada | `ColaPrioridad` + `ComparadoresApartamento` |
| Canal externo | `Cola` (FIFO) |
| Vencimiento a las 24 horas | `ColaPrioridad` por hora de vencimiento |
| Deshacer en el folio | `Pila` |
| Empleados | `ArbolBinarioBusqueda` |
| Catálogo, favoritos, mapa, preparación, turnos, estadísticas | `SistemaGestionAlojamiento` y sus gestores |
