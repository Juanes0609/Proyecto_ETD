package modelo;

import estructuras.ListaDoblementeEnlazada;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PruebaGestoresOperativos {
    private static int pruebasPasadas = 0;

    public static void main(String[] args) {
        System.out.println("=== PRUEBAS GESTORES OPERATIVOS ===\n");

        probarPreparacionPrioridad();
        probarPreparacionDespachar();
        probarCanalExternoFIFO();
        probarCanalExternoConflicto();
        probarTurnosRotacion();
        probarTurnosVuelveAlInicio();
        probarMapaInalcanzables();
        probarCerrarPasillo();
        probarRutaPreparacion();
        probarAgruparPorBloque();

        System.out.println("\n==================================");
        System.out.println("Pruebas superadas: " + pruebasPasadas);
        System.out.println("Todas las pruebas terminaron.");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError("FALLÓ: " + mensaje);
        }
        pruebasPasadas++;
        System.out.println("OK: " + mensaje);
    }

    private static Apartamento crearApartamento(String codigo) {
        return new Apartamento(
                codigo, "Apartamento " + codigo, "A", 1, 2, 4,
                EstadoOperativo.PREPARADO, "Dos habitaciones", "");
    }

    // ============ PREPARACIÓN ============

    private static void probarPreparacionPrioridad() {
        SistemaGestionAlojamiento sistema = new SistemaGestionAlojamiento();
        Apartamento a101 = crearApartamento("A101");
        Apartamento a102 = crearApartamento("A102");
        sistema.registrarApartamento(a101);
        sistema.registrarApartamento("A102".equals(a102.getCodigo()) ? a102 : a102);

        GestorPreparacion gestor = new GestorPreparacion(sistema);

        // A102 NO tiene entrada hoy, A101 SÍ tiene entrada hoy
        gestor.encolar(a102, false, null);
        gestor.encolar(a101, true, LocalDateTime.of(2026, 11, 12, 15, 0));

        // Aunque A102 entró primero, A101 debe salir primero por prioridad 1
        OrdenPreparacion primero = gestor.verSiguiente();

        verificar(
                primero.getCodigo().equals("A101")
                        && primero.getPrioridad() == 1,
                "Preparación: entrada hoy tiene prioridad sobre resto"
        );
    }

    private static void probarPreparacionDespachar() {
        SistemaGestionAlojamiento sistema = new SistemaGestionAlojamiento();
        Apartamento a101 = crearApartamento("A101");
        sistema.registrarApartamento(a101);

        GestorPreparacion gestor = new GestorPreparacion(sistema);
        gestor.encolar(a101, true,
                LocalDateTime.of(2026, 11, 12, 14, 0));

        verificar(
                a101.getEstadoOperativo() == EstadoOperativo.PENDIENTEPREPARACION,
                "Preparación: al encolar queda Pendiente de preparación"
        );

        OrdenPreparacion orden = gestor.despacharSiguiente();

        verificar(
                a101.getEstadoOperativo() == EstadoOperativo.ENPREPARACION,
                "Preparación: al despachar pasa a En preparación"
        );

        gestor.marcarPreparado(orden);

        verificar(
                a101.getEstadoOperativo() == EstadoOperativo.PREPARADO,
                "Preparación: al terminar queda Preparado"
        );
    }

    // ============ CANAL EXTERNO ============

    private static Reserva crearReservaExterna(String codigo,
                                               Apartamento apartamento,
                                               LocalDate entrada,
                                               LocalDate salida) {
        Estancia estancia = new Estancia(entrada, salida, null);
        Huesped huesped = new Huesped(
                "Test", "123", "test@test.com",
                LocalDate.of(2000, 1, 1), "123", null);

        return new Reserva(
                codigo,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(24),
                EstadoReserva.CONFIRMADA, // AHORA SÍ ACTIVA
                Canal.EXTERNA,
                apartamento,
                huesped,
                estancia
        );
    }

    private static void probarCanalExternoFIFO() {
        GestorCanalExterno gestor = new GestorCanalExterno();
        Apartamento a101 = crearApartamento("A101");

        Reserva r1 = crearReservaExterna("R1", a101,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3));
        Reserva r2 = crearReservaExterna("R2", a101,
                LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12));

        gestor.recibirSolicitud(r1);
        gestor.recibirSolicitud(r2);

        // La primera que entró debe procesarse primero
        gestor.procesarSiguiente();

        verificar(
                gestor.getAceptadas().getTam() == 1
                        && gestor.getAceptadas().getInicio().getDato() == r1,
                "Canal externo: procesa en orden de llegada FIFO"
        );
    }

    private static void probarCanalExternoConflicto() {
        GestorCanalExterno gestor = new GestorCanalExterno();
        Apartamento a101 = crearApartamento("A101");

        // Primera reserva: 1 al 5 de diciembre
        Reserva r1 = crearReservaExterna("R1", a101,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5));
        gestor.recibirSolicitud(r1);
        gestor.procesarSiguiente();

        // Segunda reserva: choca, 3 al 7 de diciembre
        Reserva r2 = crearReservaExterna("R2", a101,
                LocalDate.of(2026, 12, 3), LocalDate.of(2026, 12, 7));
        gestor.recibirSolicitud(r2);
        boolean aceptada = gestor.procesarSiguiente();

        verificar(
                !aceptada && gestor.getConflictos().getTam() == 1,
                "Canal externo: detecta conflicto con reserva vigente"
        );
    }

    // ============ TURNOS ============

    private static void probarTurnosRotacion() {
        GestorTurnos gestor = new GestorTurnos();
        gestor.agregarRecepcionista("Ana");
        gestor.agregarRecepcionista("Luis");
        gestor.agregarRecepcionista("Marta");

        verificar(
                gestor.turnoActual().equals("Ana"),
                "Turnos: inicia con el primer recepcionista"
        );

        verificar(
                gestor.avanzarTurno().equals("Luis"),
                "Turnos: avanza al siguiente"
        );

        verificar(
                gestor.avanzarTurno().equals("Marta"),
                "Turnos: avanza al tercero"
        );
    }

    private static void probarTurnosVuelveAlInicio() {
        GestorTurnos gestor = new GestorTurnos();
        gestor.agregarRecepcionista("Ana");
        gestor.agregarRecepcionista("Luis");

        gestor.avanzarTurno(); // Luis
        String vuelve = gestor.avanzarTurno(); // debe volver a Ana

        verificar(
                vuelve.equals("Ana"),
                "Turnos: lista circular vuelve al inicio"
        );
    }

    // ============ MAPA / ESTADÍSTICAS ============

    private static void probarMapaInalcanzables() {
        GestorMapaEdificio gestor = new GestorMapaEdificio();

        gestor.agregarUbicacion(new NodoEdificio(
                "REC-01", "Recepción", TipoNodoEdificio.RECEPCION));
        gestor.agregarUbicacion(new NodoEdificio(
                "A101", "Apartamento 101", TipoNodoEdificio.APARTAMENTO));
        gestor.agregarUbicacion(new NodoEdificio(
                "AIS-01", "Zona aislada", TipoNodoEdificio.CUARTOLIMPIEZA));

        gestor.conectarUbicaciones("REC-01", "A101", 5);
        gestor.conectarUbicaciones("A101", "REC-01", 5);
        // AIS-01 queda sin conexión a propósito

        verificar(
                gestor.contarUbicaciones() == 3
                        && gestor.contarPasillos() == 2,
                "Mapa: cuenta ubicaciones y pasillos"
        );

        verificar(
                gestor.apartamentosInalcanzables("REC-01").getTam() == 1,
                "Mapa: detecta apartamento inalcanzable"
        );
    }
    private static void probarCerrarPasillo() {
        GestorMapaEdificio gestor = new GestorMapaEdificio();

        gestor.agregarUbicacion(new NodoEdificio(
                "REC-01", "Recepción", TipoNodoEdificio.RECEPCION));
        gestor.agregarUbicacion(new NodoEdificio(
                "ESC-01", "Escaleras", TipoNodoEdificio.ESCALERAS));
        gestor.agregarUbicacion(new NodoEdificio(
                "LIM-01", "Limpieza", TipoNodoEdificio.CUARTOLIMPIEZA));

        gestor.conectarUbicaciones("REC-01", "ESC-01", 5);
        gestor.conectarUbicaciones("ESC-01", "REC-01", 5);
        gestor.conectarUbicaciones("ESC-01", "LIM-01", 3);
        gestor.conectarUbicaciones("LIM-01", "ESC-01", 3);

        // Cerrar el pasillo de ida y vuelta
        gestor.cerrarPasillo("REC-01", "ESC-01");
        gestor.cerrarPasillo("ESC-01", "REC-01");

        verificar(
                gestor.getGrafo().caminoMasCorto("REC-01", "LIM-01").getTam() == 0,
                "Mapa: cerrar pasillo deja sin camino"
        );

        // Volver a abrir y debe haber camino de nuevo
        gestor.abrirPasillo("REC-01", "ESC-01");
        gestor.abrirPasillo("ESC-01", "REC-01");

        verificar(
                gestor.getGrafo().caminoMasCorto("REC-01", "LIM-01").getTam() == 3,
                "Mapa: abrir pasillo recupera el camino"
        );
    }

    private static void probarRutaPreparacion() {
        SistemaGestionAlojamiento sistema = new SistemaGestionAlojamiento();
        Apartamento a101 = crearApartamento("A101");
        Apartamento a102 = crearApartamento("A102");
        sistema.registrarApartamento(a101);
        sistema.registrarApartamento(a102);

        GestorMapaEdificio mapa = new GestorMapaEdificio();
        mapa.agregarUbicacion(new NodoEdificio(
                "REC-01", "Recepción", TipoNodoEdificio.RECEPCION));
        mapa.cargarApartamentos(sistema);
        mapa.conectarUbicaciones("REC-01", "A101", 5);
        mapa.conectarUbicaciones("A101", "REC-01", 5);
        mapa.conectarUbicaciones("A101", "A102", 4);
        mapa.conectarUbicaciones("A102", "A101", 4);

        GestorPreparacion preparacion = new GestorPreparacion(sistema);
        preparacion.encolar(a101, true,
                LocalDateTime.of(2026, 11, 12, 14, 0));
        preparacion.encolar(a102, true,
                LocalDateTime.of(2026, 11, 12, 15, 0));

        ListaDoblementeEnlazada<NodoEdificio> ruta =
                mapa.rutaParaPreparacion("REC-01",
                        preparacion.listarPendientes());

        // REC -> A101 -> A102 = 3 nodos
        verificar(
                ruta.getTam() == 3,
                "Mapa: ruta de preparación recorre los pendientes en orden"
        );
    }

    private static void probarAgruparPorBloque() {
        GestorMapaEdificio gestor = new GestorMapaEdificio();

        Apartamento a101 = new Apartamento(
                "A101", "Apartamento 101", "A", 1, 2, 4,
                EstadoOperativo.PREPARADO, "", "");
        Apartamento a102 = new Apartamento(
                "A102", "Apartamento 102", "A", 1, 2, 4,
                EstadoOperativo.PREPARADO, "", "");
        Apartamento b201 = new Apartamento(
                "B201", "Apartamento 201", "B", 2, 2, 4,
                EstadoOperativo.PREPARADO, "", "");

        gestor.agregarApartamento(a101);
        gestor.agregarApartamento(a102);
        gestor.agregarApartamento(b201);

        verificar(
                gestor.agruparPorBloque().getTam() == 2,
                "Mapa: agrupa apartamentos por bloque, A y B"
        );
    }
}
