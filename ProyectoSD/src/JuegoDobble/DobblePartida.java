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
    
    // serializa las puntuaciones 'nombre1:puntuaciónX,nombre2:puntuaciónY'
    private String serializarPuntuaciones() {
        return puntuaciones.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(","));
    }
    
    
    // Métodos llamados por ClienteGestorHilos
    public void procesarDesconexion(ClienteGestorHilos perdedor) {}
    public void procesarRendicion(ClienteGestorHilos perdedor) {}
    /*
    Procesa el intento de un jugador.
    - se le pasa el hilo del jugador y el símbolo identificado
   */
    public void procesarIntento(ClienteGestorHilos jugador, int simbolo) {
      
    	// Si la partida ha terminado, se ignora el intento
    	if (cartaCentral == null) {
    		jugador.sendMessage("ERROR_JUEGO|La partida ha terminado. Esperando a ser redirigido.");
    		return; 
    	}
      
    	// se coge el nombre del jugador y a través del nombre se coge la carta del jugador
    	String nombre = jugador.getNombreUsuario();
    	List<Integer> cartaJugador = cartasJugadores.get(nombre);
      
    	// 1. Verificar la coincidencia, si es correcto, entra al if
    	if (logica.esCoincidenciaValida(simbolo, cartaJugador, cartaCentral)) {
       
    		// 2. Suma un punto y lo actualiza
    		int nuevaPuntuacion = puntuaciones.get(nombre) + 1;
    		puntuaciones.put(nombre, nuevaPuntuacion);
          
    		// 3. Notificar a todos los jugadores
    		notificarATodos("PUNTO|" + nombre + "|" + nuevaPuntuacion + "|" + serializarPuntuaciones());
          
    		// 4. se reparte nueva carta, la central pasa al jugador y una nueva en la central 
    		cartasJugadores.put(nombre, cartaCentral);
    		cartaCentral = logica.repartirCarta(); 
          
    		if (cartaCentral == null) {
    			terminarPartida(); // Llama a terminar si ya no hay cartas
    		} else {
    			iniciarNuevaRonda();
    		}
    	} else {// Coincidencia incorrecta, le avisa al jugador
    		jugador.sendMessage("ERROR_JUEGO|El símbolo " + simbolo + " no es la coincidencia. ¡Inténtalo de nuevo!");
    	}
    }

	//después de que un jugador gana un punto, se llama a este método,
	// para enviarles a todos los jugadores las nuevas cartas, para la siguiente ronda (actualiza)
	private void iniciarNuevaRonda() {
		//serializa la carta central a String
	    String cartaCentralSerializada = serializarCarta(cartaCentral);
	    
	    // avisa a todos los jugadores sobre la nueva carta central y sus cartas
	    for (ClienteGestorHilos jugador : jugadores) {
	        String nombre = jugador.getNombreUsuario();
	        String cartaJugadorSerializada = serializarCarta(cartasJugadores.get(nombre));
	        
	        String mensajeRonda = "NUEVA_RONDA|" + cartaJugadorSerializada + "|" + cartaCentralSerializada + "|" + serializarPuntuaciones();
	        jugador.sendMessage(mensajeRonda);
	    }
	}

    private void terminarPartida() {  }
    private String obtenerListaParticipantes() { return "jugadores"; }
    
    // devuelve el ganador comparando las puntuaciones y lo devuelve como String
    private String obtenerGanador() {
        return puntuaciones.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Empate/Error");
    }
}
