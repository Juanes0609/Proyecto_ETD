package gui.recepcion;

import gui.VentanaPrincipalSGA;
import gui.componentes.FabricaComponentesUI;
import gui.componentes.Formato;
import gui.componentes.Iconos;
import gui.integracion.dto.ApartaSuiteDTO;
import gui.integracion.dto.ReservaDTO;
import gui.navegacion.TipoVista;
import gui.navegacion.VistaBase;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Llegadas y salidas: check-in (solo si el aparta-suite está Preparado) y
 * check-out (el aparta-suite pasa a la cola de preparación). Cada acción se
 * confirma en el mini menú lateral, donde se explica por qué está bloqueada
 * cuando no se puede hacer.
 */
public class VistaLlegadasSalidas extends VistaBase {

    private final VentanaPrincipalSGA ventana;
    private final TableView<ReservaDTO> tabla = FabricaComponentesUI.crearTabla("Sin reservas en esta lista",
            "Cuando haya llegadas o estancias en curso aparecerán aquí.");
    private final Map<String, String> estadoSuites = new HashMap<>();
    private int filtro;

    public VistaLlegadasSalidas(VentanaPrincipalSGA ventana) {
        super(ventana);
        this.ventana = ventana;
    }

    @Override
    protected Node construir() {
        FabricaComponentesUI.agregarColumna(tabla, "Reserva", ReservaDTO::codigo);
        FabricaComponentesUI.agregarColumna(tabla, "Huésped", ReservaDTO::nombreTitular);
        FabricaComponentesUI.agregarColumna(tabla, "Aparta-suite", r -> r.codigoApartaSuite() + " · " + r.nombreApartaSuite());
        FabricaComponentesUI.agregarColumna(tabla, "Estancia", r -> Formato.rango(r.entrada(), r.salida()));
        FabricaComponentesUI.agregarColumna(tabla, "Llegada", r -> Formato.hora(r.horaLlegada()));
        FabricaComponentesUI.agregarColumnaEstado(tabla, "Estado", ReservaDTO::estado);
        FabricaComponentesUI.agregarColumnaEstado(tabla, "Estado del aparta-suite",
                r -> estadoSuites.get(r.codigoApartaSuite()));
        // Clic o Enter sobre una fila abre su detalle (también si ya estaba seleccionada).
        tabla.setOnMouseClicked(e -> abrirSeleccionada());
        tabla.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                abrirSeleccionada();
            }
        });

        HBox segmentado = FabricaComponentesUI.crearSegmentado(List.of("Llegadas de hoy", "En curso", "Todas las activas"),
                0, indice -> {
                    filtro = indice;
                    ocultarHoja();
                    cargar();
                });
        return new VBox(16, segmentado, tabla);
    }

    @Override
    public void alMostrar() {
        cargar();
    }

    private void cargar() {
        notificador().ejecutarConManejo(() -> {
            estadoSuites.clear();
            for (ApartaSuiteDTO s : conector().listarApartaSuites()) {
                estadoSuites.put(s.codigo(), s.estadoOperativo());
            }
            LocalDate hoy = LocalDate.now();
            List<ReservaDTO> reservas = conector().reservasActivas().stream().filter(r -> switch (filtro) {
                case 0 -> !r.entrada().isAfter(hoy) && !"EN_CURSO".equals(r.estado());
                case 1 -> "EN_CURSO".equals(r.estado());
                default -> true;
            }).toList();
            tabla.getItems().setAll(reservas);
        }, null);
    }

    private void abrirSeleccionada() {
        ReservaDTO r = tabla.getSelectionModel().getSelectedItem();
        if (r != null) {
            abrirDetalle(r);
        }
    }

    private void abrirDetalle(ReservaDTO r) {
        String estadoSuite = estadoSuites.get(r.codigoApartaSuite());
        VBox detalle = new VBox(14,
                FabricaComponentesUI.crearEncabezado(r.nombreTitular(), "Documento " + r.documentoTitular()),
                fila("Aparta-suite", r.codigoApartaSuite() + " · " + r.nombreApartaSuite()),
                fila("Estancia", Formato.rango(r.entrada(), r.salida()) + " · " + Formato.plural(r.noches(), "noche", "noches")),
                fila("Personas", String.valueOf(r.personas())),
                fila("Canal", Formato.estado(r.canal())),
                fila("Llegada estimada", Formato.hora(r.horaLlegada())),
                filaBadge("Estado de la reserva", r.estado()),
                filaBadge("Estado del aparta-suite", estadoSuite),
                FabricaComponentesUI.crearSeparador());

        switch (r.estado()) {
            case "CONFIRMADA" -> agregarCheckIn(detalle, r, estadoSuite);
            case "EN_CURSO" -> {
                detalle.getChildren().add(FabricaComponentesUI.crearTextoConIcono(Iconos.Icono.INFO,
                        "Al hacer check-out, el aparta-suite entra a la cola de preparación.", "campo-ayuda"));
                detalle.getChildren().add(boton("Registrar check-out", () -> conector().registrarCheckOut(r.codigo()),
                        "Check-out de " + r.codigo() + " registrado. " + r.nombreApartaSuite() + " pasó a preparación."));
            }
            default -> detalle.getChildren().add(FabricaComponentesUI.crearTextoConIcono(Iconos.Icono.RELOJ,
                    "Esperando el pago del anticipo por el portal para confirmar la reserva.", "campo-ayuda"));
        }
        mostrarHoja("Reserva " + r.codigo(), detalle);
    }

    private void agregarCheckIn(VBox detalle, ReservaDTO r, String estadoSuite) {
        Button checkIn = boton("Registrar check-in", () -> conector().registrarCheckIn(r.codigo()),
                "Check-in de " + r.codigo() + " registrado. ¡Bienvenida a " + r.nombreTitular() + "!");
        if (r.entrada().isAfter(LocalDate.now())) {
            checkIn.setDisable(true);
            detalle.getChildren().add(FabricaComponentesUI.crearTextoConIcono(Iconos.Icono.CALENDARIO,
                    "La llegada es el " + Formato.fecha(r.entrada()) + ".", "campo-ayuda"));
        } else if (!"PREPARADO".equals(estadoSuite)) {
            checkIn.setDisable(true);
            detalle.getChildren().add(FabricaComponentesUI.crearErrorInline(r.nombreApartaSuite() + " está "
                    + Formato.estado(estadoSuite).toLowerCase() + ". Solo se puede hacer check-in cuando está Preparado."));
            detalle.getChildren().add(FabricaComponentesUI.conIcono(FabricaComponentesUI.crearEnlace(
                    "Ir a Preparación", () -> ventana.getNavegacion().navegarA(TipoVista.RECEPCION_PREPARACION)),
                    Iconos.Icono.PORTAPAPELES));
        }
        detalle.getChildren().add(checkIn);
    }

    private Button boton(String texto, Runnable accion, String exito) {
        Button boton = FabricaComponentesUI.crearBotonPrimario(texto, () -> {
            if (notificador().ejecutarConManejo(accion, exito)) {
                ocultarHoja();
                cargar();
            }
        });
        boton.setMaxWidth(Double.MAX_VALUE);
        return boton;
    }

    private static HBox fila(String etiqueta, String valor) {
        Label lbl = FabricaComponentesUI.crearTexto(etiqueta, "texto-secundario");
        Label val = new Label(valor);
        val.getStyleClass().add("encabezado-vista");
        val.setWrapText(true);
        HBox fila = new HBox(12, lbl, FabricaComponentesUI.crearEspaciador(), val);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private static HBox filaBadge(String etiqueta, String estado) {
        HBox fila = new HBox(12, FabricaComponentesUI.crearTexto(etiqueta, "texto-secundario"),
                FabricaComponentesUI.crearEspaciador(), FabricaComponentesUI.crearBadgeEstado(estado));
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }
}
