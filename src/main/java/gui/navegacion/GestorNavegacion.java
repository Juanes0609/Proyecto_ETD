package gui.navegacion;

import Estructuras.Pila;
import gui.VentanaPrincipalSGA;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Controla qué vista ocupa el área central y el historial del botón "Atrás".
 * <p>
 * El historial se guarda en la {@link Pila} propia del proyecto: cada vez que
 * se navega a una vista distinta se apila la actual, y "Atrás" la desapila.
 */
public class GestorNavegacion {

    private final VentanaPrincipalSGA ventana;
    private final Pila<TipoVista> historial = new Pila<>();
    private final Map<TipoVista, Function<VentanaPrincipalSGA, ? extends VistaBase>> fabricas =
            new EnumMap<>(TipoVista.class);
    private final Map<TipoVista, VistaBase> vistasCreadas = new EnumMap<>(TipoVista.class);

    private final ReadOnlyObjectWrapper<TipoVista> vistaActual = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyBooleanWrapper puedeRetroceder = new ReadOnlyBooleanWrapper(false);

    public GestorNavegacion(VentanaPrincipalSGA ventana) {
        this.ventana = ventana;
    }

    /** Asocia una sección del menú con la clase que la construye. */
    public void registrar(TipoVista tipo, Function<VentanaPrincipalSGA, ? extends VistaBase> fabrica) {
        fabricas.put(tipo, fabrica);
        vistasCreadas.remove(tipo);
    }

    public boolean estaRegistrada(TipoVista tipo) {
        return fabricas.containsKey(tipo);
    }

    /** Muestra la vista indicada y apila la anterior en el historial. */
    public void navegarA(TipoVista destino) {
        TipoVista actual = vistaActual.get();
        if (destino == actual) {
            return;
        }
        if (actual != null) {
            historial.push(actual);
        }
        mostrar(destino);
    }

    /** Vuelve a la vista anterior del historial, si existe. */
    public void retroceder() {
        if (historial.esVacia()) {
            return;
        }
        mostrar(historial.pop());
    }

    private void mostrar(TipoVista destino) {
        VistaBase anterior = vistaActual.get() == null ? null : vistasCreadas.get(vistaActual.get());
        if (anterior != null) {
            anterior.alOcultar();
        }

        VistaBase vista = obtenerVista(destino);
        vistaActual.set(destino);
        puedeRetroceder.set(!historial.esVacia());
        ventana.mostrarContenido(destino, vista);
        if (vista != null) {
            vista.alMostrar();
        }
    }

    private VistaBase obtenerVista(TipoVista tipo) {
        VistaBase vista = vistasCreadas.get(tipo);
        if (vista == null && fabricas.containsKey(tipo)) {
            vista = fabricas.get(tipo).apply(ventana);
            vistasCreadas.put(tipo, vista);
        }
        return vista;
    }

    public ReadOnlyObjectProperty<TipoVista> vistaActualProperty() {
        return vistaActual.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty puedeRetrocederProperty() {
        return puedeRetroceder.getReadOnlyProperty();
    }
}
