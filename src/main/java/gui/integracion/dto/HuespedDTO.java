package gui.integracion.dto;

import java.time.LocalDate;

/** Datos personales de un huésped (titular de reservas). */
public record HuespedDTO(String documento, String nombre, LocalDate fechaNacimiento,
                         String telefono, String correo) {
}
