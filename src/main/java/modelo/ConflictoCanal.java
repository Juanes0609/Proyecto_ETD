package modelo;

import java.time.LocalDateTime;

public class ConflictoCanal {
    private String codigo;
    private LocalDateTime fechaRegistro;
    private String motivo;
    private SolicitudExterna solicitudExterna;

    public ConflictoCanal(String codigo, LocalDateTime fechaRegistro, String motivo, SolicitudExterna solicitudExterna) {
        this.codigo = codigo;
        this.fechaRegistro = fechaRegistro;
        this.motivo = motivo;
        this.solicitudExterna = solicitudExterna;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public SolicitudExterna getSolicitudExterna() {
        return solicitudExterna;
    }

    public void setSolicitudExterna(SolicitudExterna solicitudExterna) {
        this.solicitudExterna = solicitudExterna;
    }

    @Override
    public String toString() {
        return "ConflictoCanal{" +
                "codigo='" + codigo + '\'' +
                ", fechaRegistro=" + fechaRegistro +
                ", motivo='" + motivo + '\'' +
                '}';
    }
}
