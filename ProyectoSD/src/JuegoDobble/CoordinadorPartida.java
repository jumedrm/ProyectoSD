package JuegoDobble;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/*
Coordina la concurrencia y decide cuándo se
cumplen las condiciones para iniciar una partida.
*/
public class CoordinadorPartida {
	
	    // almacena la cola de espera para la partida. Integer es el número deseado de jugadores y
	// List es la lista de hilos de jugadores esperando a unirse a este tipo de partida.
    private Map<Integer, List<ClienteGestorHilos>> salasDeEspera = new HashMap<>();
    //Lista de todas las instancias de DobblePartida (partidas en juego)
    private List<DobblePartida> partidasActivas = new ArrayList<>();
    //almacena los resúmenes de las partidas terminadas. Se usa synchronized en sus métodos de acceso
    // para que varios hilos no intenten leer y escribir al mismo tiempo.
    private List<String> historialPartidas = new ArrayList<>();
    	
    
    	// Inicializa la lista de espera para tipos de partida comunes, mínimo 2 jugadores, máximo 8
    	// Crea la lista de espera vacía para cada posible tamaño de partida
	    public CoordinadorPartida() {
	        for (int i = 2; i <= 8; i++) {
	            salasDeEspera.put(i, new LinkedList<>());
	        }	       
	    }

	    // maneja la lógica de la sala de espera y pone una partida en activo o no
	    //se le pasa el jugador que quiere jugar y el tamaño de partida solicitada
	    public void joinWaitingList(ClienteGestorHilos jugador, int maxJugadores) {
	    	//lista de jugadores(hilos)en un principio vacía, que corresponde al número de jugadores deseado para jugar ese tipo de partida
	    	List<ClienteGestorHilos> sala = salasDeEspera.get(maxJugadores);
	        
	    	//para que el sistema no pueda iniciar dos partidas incompletas si dos jugadores se unen al mismo tiempo
	        synchronized (sala) {
	        	// si se pulsa el botón 'jugar' varias veces, no es añadido a la misma lista de espera varias veces 
	            if (!sala.contains(jugador)) { 
	                sala.add(jugador);
	                System.out.println(jugador.getNombreUsuario() + " se unió a sala de " + maxJugadores + ". Total: " + sala.size());
	                //si el número de jugadores alcanza o excede el límite
	                if (sala.size() >= maxJugadores) {
	                	//Crea una copia de la lista
	                	List<ClienteGestorHilos> jugadoresPartida = new ArrayList<>(sala);
	                	// Vacía la sala de espera
	                	sala.clear(); 
	                    iniciarNuevaPartida(jugadoresPartida);
	                } else {
	                     // si la sala no está llena, le dice al jugador cuántas personas quedan.
	                     jugador.sendMessage("ESPERA|Esperando a " + (maxJugadores - sala.size()) + " jugadores más.");
	                }
	            }
	        }
	    }
	    
	 // se llama desde el ClientHandler de un jugador que se desconecta.
	    public void removerJugador(ClienteGestorHilos jugador) {
	        // Quitarlo de cualquier sala de espera si está allí
	        for (List<ClienteGestorHilos> sala : salasDeEspera.values()) {
	            synchronized (sala) {
	                sala.remove(jugador);
	            }
	        }
	        }
	    
	    
	   
	    private void iniciarNuevaPartida(List<ClienteGestorHilos> jugadores) {
	        System.out.println("HAY QUE IMPLEMENTAR ESTA PARTE BIIIENN: " + jugadores.size() + " jugadores.");
	    }
	    
	    
	    
	    public void registrarResultado(String resumenPartida) { 
	        System.out.println("HAY QUE IMPLEMENTAR ESTA PARTE BIEEEENN: Resultado registrado.");
	    }
	    public String getHistorial() { 
	        return "HISTORIAL|NO_DATA"; 
	    }
	}
