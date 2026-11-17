# SGA — Sistema de Gestión de Alojamiento (Aparta-Suites UQ)

Aplicación de escritorio en **Java 25 + JavaFX** para administrar el alojamiento turístico
**Aparta-Suites UQ** (Salento, Quindío). Proyecto final de **Estructura de Datos (2026-2)**:
todo el núcleo se apoya en estructuras de datos genéricas propias, sin colecciones de `java.util`.

## Ejecución

- **IntelliJ IDEA**: abrir `src/main/java/Main.java` y pulsar **Run (▶)**.
- **Terminal**: `mvn javafx:run`.

### Cuentas de prueba (clave `1234`)

| Correo | Rol |
|---|---|
| `huesped@demo.co` | Huésped (Ana María Gómez) |
| `recepcion@demo.co` | Recepcionista (Laura Ríos) |
| `admin@demo.co` | Administrador |

### Datos

- La primera vez la aplicación carga el escenario de prueba del archivo plano
  [`src/main/resources/datos/escenario_inicial.txt`](src/main/resources/datos/escenario_inicial.txt)
  (apartamentos, temporadas, huéspedes, empleados, reservas, canal externo y cola de preparación;
  el formato está explicado dentro del archivo).
- Todo lo que se crea o modifica se guarda solo en `~/.sga-apartasuites/datos.ser` y sigue ahí
  al volver a abrir la aplicación.
- El botón **Cargar escenario** (al pie de la barra lateral del administrador) descarta lo
  guardado y vuelve al escenario inicial.

## Paquetes (`src/main/java/`)

| Paquete | Contenido |
|---|---|
| `estructuras/` | Estructuras genéricas propias, todas `Iterable<T>` con su propio `Iterator<T>`: `ListaDoblementeEnlazada`, `ListaCircular`, `Pila`, `Cola`, `ColaPrioridad`, `Conjunto`, `ArbolBinarioBusqueda` y `Grafo` (con `Vertice` y `Arista`). |
| `excepciones/` | `ReservaSolapadaException`, `CapacidadExcedidaException`, `FechasInvalidasException`, `TransicionEstadoInvalidaException`, `EstructuraVaciaException`, `RegistroDuplicadoException`, `RegistroNoEncontradoException`. |
| `modelo/` | Entidades del dominio y gestores: `SistemaGestionAlojamiento` (catálogo en árboles), `Apartamento`, `Huesped`, `Recepcionista`, `Reserva`, `Estancia`, `Folio`, `GestorMapaEdificio`, `GestorPreparacion`, `GestorCanalExterno`, `GestorTurnos`, `GestorEstadisticas`, `AsignadorGrupos`, `ComparadoresApartamento`. |
| `servicio/` | `ServicioReservas` (ciclo de vida, vencimientos) y `ServicioFacturacion` (liquidación noche por noche, anticipo, devoluciones). |
| `gui/` | Interfaz JavaFX de una sola ventana. Habla con el núcleo solo a través de `gui/integracion/ConectorSGA`. |
| `pruebas/` | Programas `main` que ejercitan el núcleo (`PruebaGestoresOperativos`, `PruebasGestorMapaEdificio`, `PruebaGrafo`, `PruebaColaPrioridad`). |

## Dónde está cada requisito (para la sustentación)

| Requisito | Dónde |
|---|---|
| Genéricos | Todas las clases de `estructuras/`; `ArbolBinarioBusqueda<T extends Comparable<? super T>>`. |
| Comparable | `Apartamento` (código), `Huesped` (documento), `Recepcionista` (documento), `Reserva` (fecha de entrada y, si empatan, código). |
| Comparator (4+) | `ComparadoresApartamento.porPrecio`, `POR_CAPACIDAD`, `POR_NUMERO_RESERVAS`, `POR_DORMITORIOS`; `GestorPreparacion.PRIORIDAD` (entrada hoy → hora de llegada con `thenComparing`). |
| Iterable / Iterator | Todas las estructuras; `Estancia` recorre sus noches: `for (LocalDate noche : estancia)`. |
| Recursividad | Árbol (insertar, buscar, eliminar, altura, recorridos); `Apartamento.tieneSolapamiento` (árbol de reservas con poda); `Grafo.recorridoDFS`; `ServicioFacturacion.liquidar`; `AsignadorGrupos` (backtracking). |
| Grafo + Dijkstra/BFS/DFS | `GestorMapaEdificio`: ruta de preparación (Dijkstra), inalcanzables (BFS), agrupación por bloque (DFS). |
| Cola de prioridad | Cola de preparación (`GestorPreparacion`) y reservas pendientes por vencimiento (`ServicioReservas`). |
| Cola (FIFO) | Canal externo (`GestorCanalExterno`): rechaza y registra conflicto si choca. |
| Pila | Deshacer en el folio (`Folio`); botón "Atrás" entre pantallas (`GestorNavegacion`, `VentanaCliente`). |
| Lista circular | Rotación de turnos de recepción (`GestorTurnos`). |
| Árbol binario de búsqueda | Apartamentos por código, huéspedes y empleados por documento, reservas de cada apartamento por fecha. |
| Conjunto | Favoritos del huésped sin repetidos. |
| Excepciones propias | Paquete `excepciones/`. |

## Funciones por rol

- **Huésped**: perfil, búsqueda de disponibilidad por fechas y personas ordenada por precio,
  capacidad o dormitorios, cotización con desglose noche por noche, reservas por el portal con
  anticipo del 30 %, cancelación viendo antes la devolución, favoritos e historial.
- **Recepción**: reservas directas, grupos grandes repartidos en varios aparta-suites
  (backtracking), cola del canal externo, confirmación con anticipo y cancelación, check-in
  (solo si está Preparado), check-out, no-show, cola de preparación con ruta más corta, folio con
  deshacer, turnos y calendario de ocupación.
- **Administración**: tablero, catálogo de aparta-suites (fuera de servicio con motivo),
  empleados, temporadas y tarifas, mapa interactivo del edificio (estados en tiempo real, cierre
  de pasillos, inalcanzables, rutas con Dijkstra, zoom), reportes de ocupación, ingresos por
  periodo, reservas por canal y estado, conflictos de canal y tiempo promedio de preparación.

## Commits

Formato [Conventional Commits](https://www.conventionalcommits.org/es/): `tipo(ámbito): descripción`,
por ejemplo `feat(modelo): ...`, `fix(servicio): ...`, `feat(gui): ...`, `docs: ...`.
