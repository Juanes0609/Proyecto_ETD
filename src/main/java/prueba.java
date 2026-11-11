import modelo.*;
import servicio.ServicioFacturacion;
import java.time.LocalDate;
import java.util.List;

class PruebaFacturacion {
    public static void main(String[] args) {
        List<Temporada> temporadas = List.of(
                new Temporada(LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 31), TipoTemporada.ALTA),
                new Temporada(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 10), TipoTemporada.MEDIA)
        );

        List<Ocupante> ocupantes = List.of(
                new Ocupante("Adulto 1", LocalDate.of(1990, 5, 10)),
                new Ocupante("Adulto 2", LocalDate.of(1995, 3, 3)),
                new Ocupante("Niño", LocalDate.of(2022, 1, 1))   // 4 años: ocupa cupo, no paga
        );

        ServicioFacturacion servicio = new ServicioFacturacion(temporadas, 100000, 150000, 200000);
        Estancia estancia = new Estancia(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2), null);

        double total = servicio.liquidar(estancia, ocupantes);
        System.out.println("Total: " + total);                                  // 1100000.0
        System.out.println("Anticipo: " + servicio.calcularAnticipo(total));    // 330000.0
    }
}
