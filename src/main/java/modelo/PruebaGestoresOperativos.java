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
        probarCanalExternoColaVacia();
        probarPreparacionTransiciones();
        probarPreparacionPromedio();
        probarCerrarPasilloAmbosSentidos();
        probarListaCircularVacia();
        probarEstadisticas();
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

    private static SolicitudExterna solicitud(String codigo, String apartamento,
                                              LocalDate entrada, LocalDate salida) {
        return new SolicitudExterna(codigo, LocalDateTime.now(),
                entrada, salida, 2, apartamento);
    }

    private static void probarCanalExternoFIFO() {
        SistemaGestionAlojamiento sistema = new SistemaGestionAlojamiento();
        sistema.registrarApartamento(crearApartamento("A101"));
        GestorCanalExterno gestor = new GestorCanalExterno(sistema);

        SolicitudExterna s1 = solicitud("S1", "A101",
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3));
        SolicitudExterna s2 = solicitud("S2", "A101",
                LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12));

        gestor.recibirSolicitud(s1);
        gestor.recibirSolicitud(s2);

        // La primera que entró debe procesarse primero
        ConflictoCanal conflicto = gestor.procesarSiguiente();

        verificar(
                conflicto == null
                        && gestor.getAceptadas().getTam() == 1
                        && s1.getReserva() != null
                        && s2.getReserva() == null
                        && gestor.cantidadPendientes() == 1,
                "Canal externo: procesa en orden de llegada FIFO"
        );
    }

    private static void probarCanalExternoConflicto() {
        SistemaGestionAlojamiento sistema = new SistemaGestionAlojamiento();
        Apartamento a101 = crearApartamento("A101");
        sistema.registrarApartamento(a101);
        GestorCanalExterno gestor = new GestorCanalExterno(sistema);

        // Primera solicitud: 1 al 5 de diciembre (se acepta)
        gestor.recibirSolicitud(solicitud("S1", "A101",
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5)));
        gestor.procesarSiguiente();

        // Segunda solicitud: 3 al 7 de diciembre, comparte noches (conflicto)
        SolicitudExterna s2 = solicitud("S2", "A101",
                LocalDate.of(2026, 12, 3), LocalDate.of(2026, 12, 7));
        gestor.recibirSolicitud(s2);
        ConflictoCanal conflicto = gestor.procesarSiguiente();

        verificar(
                conflicto != null
                        && gestor.getConflictos().getTam() == 1
                        && s2.getConflictoCanal() == conflicto
                        && a101.getCantidadReservas() == 1,
                "Canal externo: detecta conflicto con reserva vigente y lo registra"
        );

        // Salida el 5 y entrada el 5: no comparten noche, NO es conflicto
        gestor.recibirSolicitud(solicitud("S3", "A101",
                LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 8)));
        verificar(
                gestor.procesarSiguiente() == null,
                "Canal externo: la noche de salida no choca con la siguiente entrada"
        );
    }

    private static void probarCanalExternoColaVacia() {
        GestorCanalExterno gestor = new GestorCanalExterno(new SistemaGestionAlojamiento());
        boolean lanzo = false;
        try {
            gestor.procesarSiguiente();
        } catch (excepciones.EstructuraVaciaException e) {
            lanzo = true;
        }
        verificar(lanzo, "Canal externo: cola vacía lanza EstructuraVaciaException");
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

    // ============ NUEVAS PRUEBAS ============

    private static void probarPreparacionTransiciones() {
        SistemaGestionAlojamiento sistema = new SistemaGestionAlojamiento();
        Apartamento a101 = crearApartamento("A101");
        sistema.registrarApartamento(a101);
        GestorPreparacion gestor = new GestorPreparacion(sistema);

        OrdenPreparacion orden = gestor.encolar(a101, false, null);

        boolean duplicada = false;
        try {
            gestor.encolar(a101, false, null);
        } catch (excepciones.TransicionEstadoInvalidaException e) {
            duplicada = true;
        }
        verificar(duplicada, "Preparación: no se encola dos veces el mismo apartamento");

        boolean sinDespachar = false;
        try {
            gestor.marcarPreparado(orden); // aún está Pendiente, no En preparación
        } catch (excepciones.TransicionEstadoInvalidaException e) {
            sinDespachar = true;
        }
        verificar(sinDespachar, "Preparación: no se puede terminar sin pasar por En preparación");

        Apartamento fuera = crearApartamento("A102");
        fuera.setEstadoOperativo(EstadoOperativo.FUERASERVICIO);
        boolean rechazado = false;
        try {
            gestor.encolar(fuera, false, null);
        } catch (excepciones.TransicionEstadoInvalidaException e) {
            rechazado = true;
        }
        verificar(rechazado, "Preparación: un apartamento fuera de servicio no se encola");

        gestor.despacharSiguiente();
        boolean vacia = false;
        try {
            gestor.despacharSiguiente();
        } catch (excepciones.EstructuraVaciaException e) {
            vacia = true;
        }
        verificar(vacia, "Preparación: despachar con cola vacía lanza EstructuraVaciaException");
    }

    private static void probarPreparacionPromedio() {
        SistemaGestionAlojamiento sistema = new SistemaGestionAlojamiento();
        Apartamento a101 = crearApartamento("A101");
        sistema.registrarApartamento(a101);
        GestorPreparacion gestor = new GestorPreparacion(sistema);

        gestor.encolar(a101, true, LocalDateTime.of(2026, 11, 12, 14, 0));
        OrdenPreparacion orden = gestor.despacharSiguiente();
        orden.setHoraInicioPreparacion(LocalDateTime.of(2026, 11, 12, 9, 0));
        gestor.marcarPreparado(orden);
        orden.setHoraFinPreparacion(LocalDateTime.of(2026, 11, 12, 9, 40));

        verificar(
                gestor.minutosPromedioPreparacion() == 40.0
                        && gestor.listarTerminadas().getTam() == 1,
                "Preparación: calcula el tiempo promedio de preparación"
        );
    }

    private static void probarCerrarPasilloAmbosSentidos() {
        GestorMapaEdificio gestor = new GestorMapaEdificio();
        gestor.agregarUbicacion(new NodoEdificio("REC-01", "Recepción", TipoNodoEdificio.RECEPCION));
        gestor.agregarUbicacion(new NodoEdificio("A101", "Apartamento 101", TipoNodoEdificio.APARTAMENTO));
        gestor.conectarPasillo("REC-01", "A101", 7);

        verificar(gestor.contarPasillos() == 2, "Mapa: un pasillo crea ida y vuelta");

        gestor.cerrarPasillo("REC-01", "A101"); // un solo llamado cierra los dos sentidos

        verificar(
                gestor.rutaMasCorta("REC-01", "A101").getTam() == 0
                        && gestor.rutaMasCorta("A101", "REC-01").getTam() == 0
                        && gestor.contarPasillosCerrados() == 2
                        && !gestor.pasilloAbierto("REC-01", "A101"),
                "Mapa: cerrar un pasillo lo cierra en ambos sentidos"
        );

        gestor.abrirPasillo("A101", "REC-01");
        verificar(
                gestor.distanciaDeRuta(gestor.rutaMasCorta("A101", "REC-01")) == 7.0,
                "Mapa: calcula la distancia total de la ruta"
        );
    }

    private static void probarListaCircularVacia() {
        GestorTurnos gestor = new GestorTurnos();
        boolean lanzo = false;
        try {
            gestor.turnoActual();
        } catch (excepciones.EstructuraVaciaException e) {
            lanzo = true;
        }
        verificar(lanzo, "Turnos: sin recepcionistas lanza EstructuraVaciaException");
    }

    private static void probarEstadisticas() {
        SistemaGestionAlojamiento sistema = new SistemaGestionAlojamiento();
        Apartamento a101 = crearApartamento("A101");
        sistema.registrarApartamento(a101);

        GestorMapaEdificio mapa = new GestorMapaEdificio();
        mapa.agregarUbicacion(new NodoEdificio("REC-01", "Recepción", TipoNodoEdificio.RECEPCION));
        mapa.cargarApartamentos(sistema);
        mapa.conectarPasillo("REC-01", "A101", 5);

        GestorPreparacion preparacion = new GestorPreparacion(sistema);
        GestorCanalExterno canal = new GestorCanalExterno(sistema);

        canal.recibirSolicitud(solicitud("S1", "A101",
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4)));   // 3 noches, se acepta
        canal.recibirSolicitud(solicitud("S2", "A101",
                LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 5)));   // choca
        canal.procesarTodas();

        Temporada alta = new Temporada(LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 31), TipoTemporada.ALTA);
        ListaDoblementeEnlazada<Temporada> temporadas = new ListaDoblementeEnlazada<>();
        temporadas.agregarFinal(alta);

        GestorEstadisticas estadisticas = new GestorEstadisticas(sistema, preparacion, canal, mapa);
        ReporteEstadisticas r = estadisticas.generar(
                LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1), temporadas, "REC-01");

        verificar(
                ReporteEstadisticas.valorDe(r.getReservasPorCanal(), "EXTERNA") == 1
                        && r.getConflictosDeCanal() == 1,
                "Estadísticas: reservas por canal y conflictos de canal"
        );
        verificar(
                ReporteEstadisticas.valorDe(r.getOcupacionPorApartamento(), "A101") == 3
                        && ReporteEstadisticas.valorDe(r.getOcupacionPorTemporada(), "ALTA") == 3
                        && ReporteEstadisticas.valorDe(r.getOcupacionPorTemporada(), "BAJA") == 0,
                "Estadísticas: ocupación por apartamento y por temporada"
        );
        verificar(
                r.getUbicaciones() == 2 && r.getPasillos() == 2
                        && r.getAlcanzablesPorBFS() == 2 && r.getVisitadosPorDFS() == 2
                        && r.getInalcanzables() == 0,
                "Estadísticas: recorridos BFS y DFS del grafo"
        );

        Estancia e = new Estancia(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12), null);
        int noches = 0;
        for (LocalDate noche : e) {
            noches++;
        }
        verificar(noches == 2 && e.cantidadNoches() == 2,
                "Estancia: for-each recorre las noches a cobrar (10 al 12 son 2)");
    }
}
