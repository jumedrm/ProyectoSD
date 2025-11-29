package JuegoDobble;

import java.util.List;

public class DobblePartida {
    // Constructor necesario para inicialización en ClienteGestorHilos
    public DobblePartida(List<ClienteGestorHilos> jugadores) {
        // No hace nada aún
    }
    // Métodos llamados por ClienteGestorHilos
    public void procesarDesconexion(ClienteGestorHilos perdedor) {}
    public void procesarRendicion(ClienteGestorHilos perdedor) {}
    public void procesarIntento(ClienteGestorHilos jugador, int simbolo) {}
}

