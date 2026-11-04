package gui.integracion.dto;

import java.util.List;

/** Valor de una estancia con su desglose noche por noche. */
public record CotizacionDTO(List<NocheDTO> noches, int personasCobradas, double total) {

    public int numeroNoches() {
        return noches.size();
    }
}
