package modelo;

import java.time.LocalDateTime;

public class MovimientoFolio {
    private String codigo;
    private String concepto;
    private double valor;
    private LocalDateTime fechaHora;
    private TipoMovimientoFolio tipoMovimiento;

    public MovimientoFolio(String codigo, String concepto, double valor, LocalDateTime fechaHora,  TipoMovimientoFolio tipoMovimiento) {
        this.codigo = codigo;
        this.concepto = concepto;
        this.valor = valor;
        this.fechaHora = fechaHora;
        this.tipoMovimiento = tipoMovimiento;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public TipoMovimientoFolio getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(TipoMovimientoFolio tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }
}
