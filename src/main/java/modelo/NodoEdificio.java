package modelo;

public class NodoEdificio {
    private String codigo;
    private String nombre;
    private Apartamento apartamento;
    private TipoNodoEdificio tipoNodoEdificio;
    private double x;
    private double y;


    public NodoEdificio(String codigo, String nombre, TipoNodoEdificio tipoNodoEdificio) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipoNodoEdificio = tipoNodoEdificio;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Apartamento getApartamento() {
        return apartamento;
    }

    public void setApartamento(Apartamento apartamento) {
        this.apartamento = apartamento;
    }

    public TipoNodoEdificio getTipoNodoEdificio() {
        return tipoNodoEdificio;
    }

    public void setTipoNodoEdificio(TipoNodoEdificio tipoNodoEdificio) {
        this.tipoNodoEdificio = tipoNodoEdificio;
    }

    /** Posición en el plano del mapa (la usa la visualización gráfica). */
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setPosicion(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
