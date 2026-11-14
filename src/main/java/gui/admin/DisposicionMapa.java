package gui.admin;

import gui.integracion.dto.ConexionDTO;
import gui.integracion.dto.NodoEdificioDTO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula dónde dibujar cada nodo del mapa del edificio para que, agregue lo
 * que se agregue (bloques, pisos o aparta-suites), ningún nodo quede encima de
 * otro y ninguna línea atraviese un nodo. Las conexiones y distancias son las
 * del modelo; aquí solo se decide la posición.
 * <pre>
 *  Recepción ─┬─ Pasillo ─ [piso 1] ─ Escalera ─ [pisos altos]   ← un bloque por fila
 *    /        └─ Pasillo ─ ...
 *  Aseo
 * </pre>
 * Cada aparta-suite se coloca arriba o abajo (alternando) de la línea
 * pasillo–escalera, en el primer hueco donde su caja y su línea no choquen con
 * nada de su fila. Recepción queda a la altura media de los bloques y se aleja
 * de los pasillos lo necesario para que sus líneas no crucen otro pasillo.
 * <p>
 * No depende de JavaFX: solo hace geometría.
 */
final class DisposicionMapa {

    private static final double MARGEN_CAJA = 8;
    private static final double SEPARACION_FILAS = 20;
    private static final double X_PASILLO = 230;
    private static final double X_PISO_UNO = 350;
    private static final double X_ESCALERA = 470;
    private static final double X_PISOS_ALTOS = 590;
    private static final double DISTANCIA_RECEPCION = 160;
    private static final double ASEO_DEBAJO = 65;

    private final double ancho;
    private final double alto;
    private final double anchoArea;
    private final double altoArea;
    /** Cuántas columnas extra puede abrir un bloque hacia la derecha cuando la suya se llena. */
    private int columnasExtra;

    /**
     * @param anchoArea ancho disponible para el dibujo (se elige la variante que se vea más grande en él)
     * @param altoArea  alto disponible
     */
    DisposicionMapa(double anchoNodo, double altoNodo, double anchoArea, double altoArea) {
        this.ancho = anchoNodo;
        this.alto = altoNodo;
        this.anchoArea = anchoArea;
        this.altoArea = altoArea;
    }

    /**
     * Prueba varias variantes (bloques más altos o más anchos) y devuelve la que
     * se vería más grande en el área disponible.
     *
     * @return código del nodo → {x, y} del centro de su caja
     */
    Map<String, double[]> calcular(Collection<NodoEdificioDTO> nodos, List<ConexionDTO> conexiones) {
        Map<String, double[]> mejor = null;
        double mejorEscala = -1;
        for (columnasExtra = 0; columnasExtra <= 4; columnasExtra++) {
            Map<String, double[]> posiciones = calcularVariante(nodos, conexiones);
            double escala = escalaEnArea(posiciones);
            if (escala > mejorEscala + 0.005) {
                mejor = posiciones;
                mejorEscala = escala;
            }
        }
        return mejor;
    }

    private double escalaEnArea(Map<String, double[]> posiciones) {
        if (posiciones.isEmpty()) {
            return 1;
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (double[] p : posiciones.values()) {
            minX = Math.min(minX, p[0] - ancho / 2);
            maxX = Math.max(maxX, p[0] + ancho / 2);
            minY = Math.min(minY, p[1] - alto / 2);
            maxY = Math.max(maxY, p[1] + alto / 2);
        }
        return Math.min(anchoArea / (maxX - minX), altoArea / (maxY - minY));
    }

    private Map<String, double[]> calcularVariante(Collection<NodoEdificioDTO> nodos, List<ConexionDTO> conexiones) {
        Map<String, double[]> posiciones = new HashMap<>();
        List<String> bloques = nodos.stream().filter(n -> "APARTAMENTO".equals(n.tipo()))
                .map(NodoEdificioDTO::bloque).distinct().sorted().toList();

        List<double[]> centros = new ArrayList<>();
        double siguienteBorde = 0;
        for (String b : bloques) {
            Map<String, double[]> fila = disponerBloque(b, nodos, conexiones);
            double minY = Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            for (double[] p : fila.values()) {
                minY = Math.min(minY, p[1] - alto / 2);
                maxY = Math.max(maxY, p[1] + alto / 2);
            }
            double desplazar = siguienteBorde - minY;
            for (Map.Entry<String, double[]> e : fila.entrySet()) {
                posiciones.put(e.getKey(), new double[]{e.getValue()[0], e.getValue()[1] + desplazar});
            }
            centros.add(new double[]{desplazar});
            siguienteBorde = maxY + desplazar + SEPARACION_FILAS;
        }
        if (bloques.isEmpty()) {
            return posiciones;
        }

        double medio = (centros.getFirst()[0] + centros.getLast()[0]) / 2;
        String recepcion = codigoDeTipo(nodos, "RECEPCION");
        String aseo = codigoDeTipo(nodos, "CUARTO_ASEO");
        double xRecepcion = ubicarRecepcion(posiciones, bloques, medio);
        if (recepcion != null) {
            posiciones.put(recepcion, new double[]{xRecepcion, medio});
        }
        if (aseo != null) {
            // Debajo y a la izquierda: su borde derecho queda antes del centro de recepción,
            // así las líneas de recepción (que van hacia la derecha) nunca lo cruzan.
            posiciones.put(aseo, new double[]{xRecepcion - ancho / 2 - 3, medio + ASEO_DEBAJO});
        }
        return posiciones;
    }

    /** Pasillo y escalera en la línea central; los aparta-suites, en huecos libres arriba y abajo. */
    private Map<String, double[]> disponerBloque(String b, Collection<NodoEdificioDTO> nodos,
                                                 List<ConexionDTO> conexiones) {
        String pasillo = "P-" + b;
        String escalera = "ESC-" + b;
        Map<String, double[]> fila = new HashMap<>();
        fila.put(pasillo, new double[]{X_PASILLO, 0});
        fila.put(escalera, new double[]{X_ESCALERA, 0});
        List<double[][]> lineas = new ArrayList<>();
        lineas.add(new double[][]{fila.get(pasillo), fila.get(escalera)});

        List<String> apartamentos = nodos.stream()
                .filter(n -> "APARTAMENTO".equals(n.tipo()) && b.equals(n.bloque()))
                .map(NodoEdificioDTO::codigo).sorted().toList();
        List<String> pisoUno = apartamentos.stream().filter(c -> conectados(conexiones, pasillo, c)).toList();
        List<String> pisosAltos = apartamentos.stream().filter(c -> !pisoUno.contains(c)).toList();
        colocar(pisoUno, fila.get(pasillo), X_PISO_UNO, fila, lineas);
        colocar(pisosAltos, fila.get(escalera), X_PISOS_ALTOS, fila, lineas);
        return fila;
    }

    /** Busca para cada aparta-suite el hueco más cercano a su columna, alternando arriba y abajo. */
    private void colocar(List<String> codigos, double[] ancla, double xPreferida,
                         Map<String, double[]> fila, List<double[][]> lineas) {
        for (int k = 0; k < codigos.size(); k++) {
            double signo = k % 2 == 0 ? -1 : 1;
            double[] hueco = buscarHueco(ancla, xPreferida, signo, fila, lineas);
            fila.put(codigos.get(k), hueco);
            lineas.add(new double[][]{ancla, hueco});
        }
    }

    private double[] buscarHueco(double[] ancla, double xPreferida, double signo,
                                 Map<String, double[]> fila, List<double[][]> lineas) {
        // Se prefiere el hueco más cercano a la línea central y a la columna propia;
        // cuando la columna se llena, se abren columnas hacia la derecha.
        double[] mejor = null;
        double mejorCosto = Double.MAX_VALUE;
        for (double dy = 38; dy < 2000 && mejor == null; dy += 10) {
            for (double dx = -60; dx <= columnasExtra * (ancho + 12) + 60; dx += 10) {
                double x = xPreferida + dx;
                if (x - ancho / 2 <= ancla[0] + ancho / 2 - 20) {
                    continue; // siempre a la derecha de su pasillo o escalera
                }
                double[] candidato = {x, ancla[1] + signo * dy};
                double costo = Math.abs(dx);
                if (costo < mejorCosto && cabe(candidato, ancla, fila, lineas)) {
                    mejor = candidato;
                    mejorCosto = costo;
                }
            }
        }
        return mejor != null ? mejor : new double[]{xPreferida, ancla[1] + signo * 38};
    }

    private boolean cabe(double[] p, double[] ancla, Map<String, double[]> fila, List<double[][]> lineas) {
        for (double[] otro : fila.values()) {
            if (Math.abs(otro[0] - p[0]) < ancho + MARGEN_CAJA && Math.abs(otro[1] - p[1]) < alto + MARGEN_CAJA) {
                return false; // cajas encimadas
            }
        }
        for (double[][] l : lineas) {
            if (cruza(l[0], l[1], p)) {
                return false; // una línea existente pasaría por la caja nueva
            }
        }
        for (double[] otro : fila.values()) {
            if (otro != ancla && cruza(ancla, p, otro)) {
                return false; // la línea nueva pasaría por otra caja
            }
        }
        return true;
    }

    /**
     * Aleja recepción de la columna de pasillos hasta que ninguna de sus líneas
     * cruce un pasillo ajeno (con muchos bloques, las líneas más inclinadas lo harían).
     */
    private double ubicarRecepcion(Map<String, double[]> posiciones, List<String> bloques, double medio) {
        double x = X_PASILLO - DISTANCIA_RECEPCION;
        for (int intento = 0; intento < 200; intento++, x -= 20) {
            double[] centro = {x, medio};
            boolean libre = true;
            for (String b : bloques) {
                double[] destino = posiciones.get("P-" + b);
                for (String otro : bloques) {
                    if (!otro.equals(b) && cruza(centro, destino, posiciones.get("P-" + otro))) {
                        libre = false;
                    }
                }
            }
            if (libre) {
                return x;
            }
        }
        return x;
    }

    /** ¿El segmento a–b pasa por la caja centrada en c (con un pequeño margen)? */
    private boolean cruza(double[] a, double[] b, double[] c) {
        if (c == null || a == c || b == c || (c[0] == a[0] && c[1] == a[1]) || (c[0] == b[0] && c[1] == b[1])) {
            return false;
        }
        double minX = c[0] - ancho / 2 - 3;
        double maxX = c[0] + ancho / 2 + 3;
        double minY = c[1] - alto / 2 - 3;
        double maxY = c[1] + alto / 2 + 3;
        // Liang–Barsky: recorta el segmento contra el rectángulo
        double t0 = 0;
        double t1 = 1;
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {a[0] - minX, maxX - a[0], a[1] - minY, maxY - a[1]};
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                if (q[i] < 0) {
                    return false;
                }
            } else {
                double t = q[i] / p[i];
                if (p[i] < 0) {
                    t0 = Math.max(t0, t);
                } else {
                    t1 = Math.min(t1, t);
                }
                if (t0 > t1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean conectados(List<ConexionDTO> conexiones, String a, String b) {
        return conexiones.stream().anyMatch(c -> (c.origen().equals(a) && c.destino().equals(b))
                || (c.origen().equals(b) && c.destino().equals(a)));
    }

    private static String codigoDeTipo(Collection<NodoEdificioDTO> nodos, String tipo) {
        return nodos.stream().filter(n -> tipo.equals(n.tipo())).map(NodoEdificioDTO::codigo).findFirst()
                .orElse(null);
    }
}
