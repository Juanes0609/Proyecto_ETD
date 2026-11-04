# SGA — Sistema de Gestión de Alojamiento (Aparta-Suites UQ)

Aplicación de escritorio en **Java 25 (JavaFX)** para la administración integral del alojamiento turístico **Aparta-Suites UQ** en Salento (Quindío), desarrollada para la asignatura de **Estructura de Datos (2026-2)** apoyándose 100 % en Estructuras de datos genéricas propias.

## Arquitectura de Paquetes (`src/main/java/`)

- `Estructuras/`: Implementación desde cero de las 8 Estructuras genéricas propias con `Iterable<T>` e `Iterator<T>` (`ListaDoblementeEnlazada`, `ListaCircular`, `Pila`, `Cola`, `ColaPrioridad`, `Conjunto`, `ArbolBinarioBusqueda`, `Grafo`).
- `excepciones/`: Excepciones propias de validación de dominio y Estructuras (`ReservaSolapadaException`, `CapacidadExcedidaException`, `FechasInvalidasException`, `TransicionEstadoInvalidaException`, `EstructuraVaciaException`).
- `modelo/`: Entidades del negocio con `Comparable<T>` e `Iterable<LocalDate>` en `Estancia`.
- `comparadores/`: Criterios de ordenamiento alternativos con `Comparator<T>` y `thenComparing`.
- `algoritmos/`: Detección recursiva de solapamientos en ABB, liquidación recursiva noche por noche, *backtracking* para asignación de grupos grandes y algoritmos sobre grafos (Dijkstra, BFS y DFS recursivo).
- `persistencia/`: Carga de escenarios de prueba iniciales desde archivo plano (`escenario_inicial.txt`).
- `servicio/`: Fachada principal (`SistemaGestionAlojamiento`) con la lógica operativa de los 3 canales, folios, preparación y reportes.
- `gui/`: Interfaz gráfica de usuario en **JavaFX** con arquitectura *Single-Window* (una sola ventana, panel inspector lateral y notificaciones *inline*), navegación "Atrás" respaldada por `Pila` y punto de integración `gui/integracion/ConectorSGA`.

## Ejecución

- **IntelliJ IDEA**: abrir `src/main/java/Main.java` y pulsar **Run (▶)**. No requiere configurar `--module-path`.
- **Terminal**: `mvn javafx:run`.
