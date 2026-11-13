package modelo;

import estructuras.ListaDoblementeEnlazada;

public final class ConstructorEdificio {
    public static final String RECEPCION = "REC";
    public static final String ASEO = "ASEO";

    private ConstructorEdificio() {}

    public static void construir(GestorMapaEdificio mapa, SistemaGestionAlojamiento sistema) {
        NodoEdificio recepcion = new NodoEdificio(RECEPCION, "Recepción", TipoNodoEdificio.RECEPCION);
        recepcion.setPosicion(70, 210);
        NodoEdificio aseo = new NodoEdificio(ASEO, "Cuarto de aseo", TipoNodoEdificio.CUARTOLIMPIEZA);
        aseo.setPosicion(70, 340);
        mapa.agregarUbicacion(recepcion);
        mapa.agregarUbicacion(aseo);
        mapa.conectarPasillo(RECEPCION, ASEO, 12);

        ListaDoblementeEnlazada<String> bloques = new ListaDoblementeEnlazada<>();
        for (Apartamento a : sistema.listarApartamentos()) {
            if (!bloques.contiene(a.getBloque())) {
                bloques.agregarFinal(a.getBloque());
            }
        }

        int indiceBloque = 0;
        String bloqueAnterior = null;
        for (String bloque : bloques) {
            double y = 80 + 130 * indiceBloque;
            NodoEdificio pasillo = new NodoEdificio("P-" + bloque, "Pasillo " + bloque, TipoNodoEdificio.PASILLO);
            pasillo.setPosicion(230, y);
            NodoEdificio escalera = new NodoEdificio("ESC-" + bloque, "Escalera " + bloque, TipoNodoEdificio.ESCALERAS);
            escalera.setPosicion(470, y);
            mapa.agregarUbicacion(pasillo);
            mapa.agregarUbicacion(escalera);
            mapa.conectarPasillo(RECEPCION, pasillo.getCodigo(), indiceBloque == 1 ? 15 : 25);
            mapa.conectarPasillo(pasillo.getCodigo(), escalera.getCodigo(), 18);
            if (bloqueAnterior != null) {
                mapa.conectarPasillo("P-" + bloqueAnterior, pasillo.getCodigo(), 20);
            }

            int enPiso1 = 0;
            int enPisosAltos = 0;
            for (Apartamento a : sistema.listarApartamentos()) {
                if (!a.getBloque().equals(bloque)) {
                    continue;
                }
                boolean piso1 = a.getPiso() <= 1;
                int posicion = piso1 ? enPiso1++ : enPisosAltos++;
                NodoEdificio nodo = a.getNodoEdificio();
                nodo.setPosicion(piso1 ? 350 : 590, y + (posicion % 2 == 0 ? -38 : 38));
                mapa.agregarApartamento(a);
                mapa.conectarPasillo(piso1 ? pasillo.getCodigo() : escalera.getCodigo(),
                        nodo.getCodigo(), posicion % 2 == 0 ? 10 : 14);
            }
            bloqueAnterior = bloque;
            indiceBloque++;
        }
    }
}