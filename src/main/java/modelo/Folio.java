package modelo;

import estructuras.ListaDoblementeEnlazada;
import estructuras.Pila;

import java.time.LocalDateTime;

public class Folio {
    private double saldoActual;
    private Reserva reserva;
    private ListaDoblementeEnlazada<MovimientoFolio> listMovimientosFolio;
    private final Pila<MovimientoFolio> operaciones = new Pila<>();          // NUEVO
    private int consecutivo;

    public Folio(double saldoActual, Reserva reserva) {
        this.saldoActual = saldoActual;
        this.reserva = reserva;
        this.listMovimientosFolio = new ListaDoblementeEnlazada<>();
    }


    public void registrarCargo(String concepto, double valor) {
        registrar(TipoMovimientoFolio.CARGO, concepto, valor);
    }

    public void registrarPago(String concepto, double valor) {
        registrar(TipoMovimientoFolio.PAGO, concepto, valor);
    }

    private void registrar(TipoMovimientoFolio tipo, String concepto, double valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException("El valor del movimiento debe ser mayor que cero");
        }
        MovimientoFolio movimiento = new MovimientoFolio(
                generarCodigo(), concepto, valor, LocalDateTime.now(), tipo);
        listMovimientosFolio.agregarFinal(movimiento);
        operaciones.push(movimiento);
        aplicarAlSaldo(movimiento);
    }

    public MovimientoFolio deshacerUltimaOperacion() {
        MovimientoFolio original = operaciones.pop();   // lanza EstructuraVaciaException si no hay nada

        TipoMovimientoFolio tipoContrario = (original.getTipoMovimiento() == TipoMovimientoFolio.CARGO)
                ? TipoMovimientoFolio.PAGO
                : TipoMovimientoFolio.CARGO;

        MovimientoFolio reverso = new MovimientoFolio(
                generarCodigo(),
                "Reverso de " + original.getCodigo() + ": " + original.getConcepto(),
                original.getValor(),
                LocalDateTime.now(),
                tipoContrario);

        listMovimientosFolio.agregarFinal(reverso);     // el original NO se borra
        aplicarAlSaldo(reverso);
        return reverso;
    }

    private void aplicarAlSaldo(MovimientoFolio movimiento) {
        if (movimiento.getTipoMovimiento() == TipoMovimientoFolio.CARGO) {
            saldoActual += movimiento.getValor();
        } else {
            saldoActual -= movimiento.getValor();
        }
    }

    private String generarCodigo() {
        consecutivo++;
        return "MOV-" + consecutivo;
    }



    public double getSaldoActual() {
        return saldoActual;
    }
    public void setSaldoActual(double saldoActual) {
        this.saldoActual = saldoActual;
    }

    public ListaDoblementeEnlazada<MovimientoFolio> getListMovimientosFolio() {
        return listMovimientosFolio;
    }

    public void setListMovimientosFolio(ListaDoblementeEnlazada<MovimientoFolio> listMovimientosFolio) {
        this.listMovimientosFolio = listMovimientosFolio;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }
}
