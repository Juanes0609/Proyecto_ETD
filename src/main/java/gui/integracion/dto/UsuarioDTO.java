package gui.integracion.dto;

/** Usuario autenticado. {@code documento} enlaza con el huésped o el empleado. */
public record UsuarioDTO(String correo, String nombre, Rol rol, String documento) {

    /** Iniciales para el avatar ("Ana Gómez" → "AG"). */
    public String iniciales() {
        StringBuilder sb = new StringBuilder();
        for (String parte : nombre.trim().split("\\s+")) {
            if (!parte.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(parte.charAt(0)));
            }
        }
        return sb.toString();
    }
}
