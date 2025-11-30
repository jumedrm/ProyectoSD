package JuegoDobble;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
Representa una partida activa de Dobble. 
Gestiona el estado del juego, las cartas y las puntuaciones.
*/
public class DobblePartida {
	
	// Lista de jugadores (hilos) en esta partida
    private List<ClienteGestorHilos> jugadores; 
    
    // Almacena las puntuaciones de cada jugador: Nombre con su puntuación
    private Map<String, Integer> puntuaciones; 
    
    // Instancia de la lógica para repartir cartas y validar coincidencias
    private DobbleLogic logica; 
    
    // La carta que está en el centro de la mesa
    private List<Integer> cartaCentral; 
    
    // Almacena la carta que cada jugador tiene en su mano en la ronda actual
    // Esta carta, está mapeada por el nombre de usuario
    private Map<String, List<Integer>> cartasJugadores;
	
	
	
    // Constructor necesario para inicialización en ClienteGestorHilos
    public DobblePartida(List<ClienteGestorHilos> jugadores) {
    	//inicializa variables
    	this.jugadores = jugadores;
        this.puntuaciones = new HashMap<>();
        this.logica = new DobbleLogic(); 
        this.cartasJugadores = new HashMap<>();
        
        // Por cada jugador (hilo del jugador) inicializa su puntuación y 
        // marca que no está en la sala de espera, para que el hilo lo sepa
        // Le pasa al hilo la partida que se juega con this.
        for (ClienteGestorHilos jugador : jugadores) {
            puntuaciones.put(jugador.getNombreUsuario(), 0);
            jugador.setEnPartida(true);
            jugador.setPartidaActual(this); 
        }
        
        inicializarJuego();
    }
    
    
    //Inicializa las puntuaciones y reparte las cartas iniciales
    private void inicializarJuego() {
    	// 1. Asigna la carta central como una lista de números y la quita del mazo con el método repartirCarta()
        List<Integer> cartaCentralRepartida = logica.repartirCarta();
        
        // por si el mazo está vacío, lo gestiona
        if (cartaCentralRepartida == null) {
            notificarATodos("ERROR|Fallo al iniciar partida: Mazo vacío (Necesita al menos 3 cartas).");
            return; 
        }
        
        //asigna la carta central a la variable que le corresponde de la clase
        // serializa la carta central, para tenerla también como una String,
        // en esa Cadena, van a ir los números de la lista separados por comas:
        // tipo esto "1,45,22,33,43,21,5,9"
        this.cartaCentral = cartaCentralRepartida;
        String cartaCentralSerializada = serializarCarta(cartaCentral);

        // 2. Asigna/reparte una carta a cada jugador
        for (ClienteGestorHilos jugador : jugadores) {
            List<Integer> cartaJugadorRepartida = logica.repartirCarta();
            
            //si hay suficientes cartas entra en el if
            if (cartaJugadorRepartida != null) {
            	// asigna la carta al jugador, en este caso a la variable de la clase que ele corresponde
                puntuaciones.put(jugador.getNombreUsuario(), 0);
                jugador.setEnPartida(true);
                jugador.setPartidaActual(this);
                cartasJugadores.put(jugador.getNombreUsuario(), cartaJugadorRepartida);
                
                // serializa la carta del jugador, poniendola como uuna String
                String cartaJugadorSerializada = serializarCarta(cartaJugadorRepartida);
                // avisa al jugador (hilo) que inicia la partida
                // y le envía la carta del jugador y la central serializada y 
                //la puntuación también serializada, en modo de String con el 'nombre1:puntuaciónX,nombre2:puntuaciónY'
                String mensajeInicio = "INICIO_PARTIDA|" + cartaJugadorSerializada + "|" + cartaCentralSerializada + "|" + serializarPuntuaciones();
                jugador.sendMessage(mensajeInicio);
                
            } else {//si no hay suficientes cartas, lo gestiona. Le notifica al propio jugador y luego a todos (este último mensaje también al propio jugador)
                jugador.sendMessage("ERROR|No hay suficientes cartas. Partida cancelada.");
                notificarATodos("ERROR|Partida cancelada: Mazo insuficiente.");
                return;
            }
        }
    }
    
    
 // envía mensajes o avisos a todos los jugadores (hilos)
    private void notificarATodos(String mensaje) {
        for (ClienteGestorHilos jugador : jugadores) {
            jugador.sendMessage(mensaje);
        }
    }
    
 // Convierte List<Integer> a String ("1,2,3,4,5,6,7,8")
    private String serializarCarta(List<Integer> carta) {
        if (carta == null) {
            // En caso de mazo vacío o error
            return ""; 
        }
        return carta.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }
    
    private String serializarPuntuaciones() {
         // Implementación mínima para que el cliente no falle al inicio 
         return "0:0"; 
    }
    
    
    // Métodos llamados por ClienteGestorHilos
    public void procesarDesconexion(ClienteGestorHilos perdedor) {}
    public void procesarRendicion(ClienteGestorHilos perdedor) {}
    public void procesarIntento(ClienteGestorHilos jugador, int simbolo) {
    	jugador.sendMessage("ERROR_JUEGO|Hay que implementaar esto");
    }
    private void iniciarNuevaRonda() {  }
    private void terminarPartida() {  }
    private String obtenerListaParticipantes() { return "jugadores"; }
    private String obtenerGanador() { return "ganador"; }
}
