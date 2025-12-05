package JuegoDobble;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
	
	// Rastrea si un jugador sigue activo o ha abandonado 
	private Map<String, Boolean> jugadoresActivos; 
	// Lista para registrar el orden de abandono (primero el que se rinde/desconecta primero)
	private List<String> perdedoresPartida;

	// Constructor necesario para inicialización en ClienteGestorHilos
	public DobblePartida(List<ClienteGestorHilos> jugadores) {
		// inicializa variables
		this.jugadores = jugadores;
		this.puntuaciones = new HashMap<>();
		this.logica = new DobbleLogic();
		this.cartasJugadores = new HashMap<>();
		this.jugadoresActivos = new ConcurrentHashMap<>(); 
	    this.perdedoresPartida = Collections.synchronizedList(new LinkedList<>());

		// Por cada jugador (hilo del jugador) inicializa su puntuación y
		// marca que no está en la sala de espera, para que el hilo lo sepa
		// Le pasa al hilo la partida que se juega con this.
		for (ClienteGestorHilos jugador : jugadores) {
			puntuaciones.put(jugador.getNombreUsuario(), 0);
			jugador.setEnPartida(true);
			jugador.setPartidaActual(this);
			jugadoresActivos.put(jugador.getNombreUsuario(), true);
		}

		inicializarJuego();
	}

	// Inicializa las puntuaciones y reparte las cartas iniciales
	private void inicializarJuego() {
		// 1. Asigna la carta central como una lista de números y la quita del mazo con
		// el método repartirCarta()
		List<Integer> cartaCentralRepartida = logica.repartirCarta();

		// por si el mazo está vacío, lo gestiona
		if (cartaCentralRepartida == null) {
			notificarATodos("ERROR|Fallo al iniciar partida: Mazo vacío (Necesita al menos 3 cartas).");
			return;
		}

		// asigna la carta central a la variable que le corresponde de la clase
		// serializa la carta central, para tenerla también como una String,
		// en esa Cadena, van a ir los números de la lista separados por comas:
		// tipo esto "1,45,22,33,43,21,5,9"
		this.cartaCentral = cartaCentralRepartida;
		String cartaCentralSerializada = serializarCarta(cartaCentral);

		// 2. Asigna/reparte una carta a cada jugador
		for (ClienteGestorHilos jugador : jugadores) {
			List<Integer> cartaJugadorRepartida = logica.repartirCarta();

			// si hay suficientes cartas entra en el if
			if (cartaJugadorRepartida != null) {
				// asigna la carta al jugador, en este caso a la variable de la clase que ele
				// corresponde
				puntuaciones.put(jugador.getNombreUsuario(), 0);
				jugador.setEnPartida(true);
				jugador.setPartidaActual(this);
				cartasJugadores.put(jugador.getNombreUsuario(), cartaJugadorRepartida);

				// serializa la carta del jugador, poniendola como uuna String
				String cartaJugadorSerializada = serializarCarta(cartaJugadorRepartida);
				// avisa al jugador (hilo) que inicia la partida
				// y le envía la carta del jugador y la central serializada y
				// la puntuación también serializada, en modo de String con el
				// 'nombre1:puntuaciónX,nombre2:puntuaciónY'
				String mensajeInicio = "INICIO_PARTIDA|" + cartaJugadorSerializada + "|" + cartaCentralSerializada + "|"
						+ serializarPuntuaciones();
				jugador.sendMessage(mensajeInicio);

			} else {// si no hay suficientes cartas, lo gestiona. Le notifica al propio jugador y
					// luego a todos (este último mensaje también al propio jugador)
				jugador.sendMessage("ERROR|No hay suficientes cartas. Partida cancelada.");
				notificarATodos("ERROR|Partida cancelada: Mazo insuficiente.");
				return;
			}
		}
	}

	// Envía mensajes o avisos a los jugadores que siguen en la partida.
	private void notificarATodos(String mensaje) {
	    // Usamos la lista de jugadores de la partida (this.jugadores)
	    for (ClienteGestorHilos jugador : jugadores) {
	        // Solo notifica si el hilo todavía tiene asignada esta partida.
	        // Un jugador que se rinde/desconecta ya tiene su partidaActual = null.
	        if (jugador.getPartidaActual() == this) {
	            jugador.sendMessage(mensaje);
	        }
	    }
	}

	// Convierte List<Integer> a String ("1,2,3,4,5,6,7,8")
	private String serializarCarta(List<Integer> carta) {
		if (carta == null) {
			// En caso de mazo vacío o error
			return "";
		}
		return carta.stream().map(Object::toString).collect(Collectors.joining(","));
	}

	// serializa las puntuaciones 'nombre1:puntuaciónX,nombre2:puntuaciónY'
	private String serializarPuntuaciones() {
		return puntuaciones.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue())
				.collect(Collectors.joining(","));
	}

	

	
	public void procesarDesconexion(ClienteGestorHilos perdedor) {
		String nombrePerdedor = perdedor.getNombreUsuario();
	    
	    // comprueba si ya estaba marcado como inactivo 
	    if (!jugadoresActivos.getOrDefault(nombrePerdedor, false)) {
	        return; 
	    }
	    
	    // marcar como inactivo
	    jugadoresActivos.put(nombrePerdedor, false);

	    // registrar en la lista de perdedores
	    perdedoresPartida.add(nombrePerdedor + " (Desconexión)");

	    // Notificación general a todos los que quedan
	    notificarATodos("EVENTO_ABANDONO|DESCONEXION|" + nombrePerdedor);
	    System.out.println(nombrePerdedor + " se ha desconectado.");

	    // limpieza del hilo perdedor (ya que el hilo ya está en 'finally' de ClienteGestorHilos)
	    perdedor.setEnPartida(false);
	    perdedor.setPartidaActual(null);

	    // Verificar si solo queda un jugador (el ganador)
	    verificarFinDePartidaPorAbandono("Abandono (Desconexión)");
	}

	
	public void procesarRendicion(ClienteGestorHilos perdedor) {
		String nombrePerdedor = perdedor.getNombreUsuario();

	    // marcar como inactivo si no lo está
	    if (!jugadoresActivos.getOrDefault(nombrePerdedor, false)) {
	        perdedor.sendMessage("ERROR|Ya has abandonado la partida.");
	        return;
	    }
	    jugadoresActivos.put(nombrePerdedor, false);
	    
	    // Registrar en la lista de perdedores
	    perdedoresPartida.add(nombrePerdedor + " (Rendición)");

	    // notificación general a todos los que quedan
	    notificarATodos("EVENTO_ABANDONO|RENDICION|" + nombrePerdedor);
	    System.out.println(nombrePerdedor + " se ha rendido.");

	    // notificación y limpieza del hilo perdedor
	    perdedor.sendMessage("FIN_PARTIDA|Te has rendido. Volviendo al menú principal.|" + serializarPuntuaciones());
	    perdedor.setEnPartida(false);
	    perdedor.setPartidaActual(null);
	    
	    // Verificar si solo queda un jugador (el ganador)
	    verificarFinDePartidaPorAbandono("Abandono (Rendición)");
		}
	
	private void verificarFinDePartidaPorAbandono(String causaAbandono) {
	    long activos = jugadoresActivos.values().stream().filter(b -> b).count();

	    if (activos <= 1) {
	        // Encontrar al único ganador (si lo hay)
	        ClienteGestorHilos ganador = jugadores.stream()
	            .filter(hilo -> jugadoresActivos.getOrDefault(hilo.getNombreUsuario(), false))
	            .findFirst()
	            .orElse(null);
	            
	        // Si hay un jugador activo (ganador), terminamos la partida
	        if (ganador != null) {
	            terminarPartidaPorGanadorUnico(ganador, causaAbandono);
	        } else {
	            // Caso: Partida terminada sin ganador (todos se deconectan)
	            terminarPartidaSinGanador("Todos se desconectaron/rindieron.");
	        }
	    }
	}
	
	private void terminarPartidaSinGanador(String causa) {
	    String participantes = obtenerListaParticipantes();
	    String resumen = String.format("PARTICIPANTES: %s @ RESULTADO: %s @ FIN: %s", 
	        participantes, serializarPuntuaciones(), causa);

	    DobbleServer.getCoordinadorPartida().registrarResultado(resumen);

	    // Limpieza de todos los hilos (aunque ya se hizo al abandonar/desconectar)
	    for (ClienteGestorHilos jugador : jugadores) {
	        if (jugador.getPartidaActual() == this) {
	             jugador.setEnPartida(false);
	             jugador.setPartidaActual(null);
	             // Solo notificar a los que no han sido notificados (ya se hizo en procesarRendicion)
	        }
	    }
	    System.out.println("Partida finalizada. Causa: " + causa);
	}
	
	
	/*
	 * Procesa el intento de un jugador. - se le pasa el hilo del jugador y el
	 * símbolo identificado
	 */
	public void procesarIntento(ClienteGestorHilos jugador, int simbolo) {

		// Si la partida ha terminado, se ignora el intento
		if (cartaCentral == null) {
			jugador.sendMessage("ERROR_JUEGO|La partida ha terminado. Esperando a ser redirigido.");
			return;
		}

		// se coge el nombre del jugador y a través del nombre se coge la carta del
		// jugador
		String nombre = jugador.getNombreUsuario();
		List<Integer> cartaJugador = cartasJugadores.get(nombre);

		// 1. Verificar la coincidencia, si es correcto, entra al if
		if (logica.esCoincidenciaValida(simbolo, cartaJugador, cartaCentral)) {

			// 2. Suma un punto y lo actualiza
			int nuevaPuntuacion = puntuaciones.get(nombre) + 1;
			puntuaciones.put(nombre, nuevaPuntuacion);

			// 3. Notificar a todos los jugadores
			notificarATodos("PUNTO|" + nombre + "|" + nuevaPuntuacion + "|" + serializarPuntuaciones());

			// 4. se reparte nueva carta, la central pasa al jugador y una nueva en la
			// central
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

	// después de que un jugador gana un punto, se llama a este método,
	// para enviarles a todos los jugadores las nuevas cartas, para la siguiente
	// ronda (actualiza)
	private void iniciarNuevaRonda() {
		// serializa la carta central a String
		String cartaCentralSerializada = serializarCarta(cartaCentral);

		// avisa a todos los jugadores sobre la nueva carta central y sus cartas
		for (ClienteGestorHilos jugador : jugadores) {
			String nombre = jugador.getNombreUsuario();
			String cartaJugadorSerializada = serializarCarta(cartasJugadores.get(nombre));

			String mensajeRonda = "NUEVA_RONDA|" + cartaJugadorSerializada + "|" + cartaCentralSerializada + "|"
					+ serializarPuntuaciones();
			jugador.sendMessage(mensajeRonda);
		}
	}

	
	private void terminarPartida() {
		String ganador = obtenerGanador();
	    String puntuacionesFinales = serializarPuntuaciones();
	    String participantes = obtenerListaParticipantes();
	    
	    // crear el ranking de los jugadores que terminaron jugando (activos)
	    List<String> rankingActivos = puntuaciones.entrySet().stream()
	        // filtrar solo los que estaban activos al final del juego (no abandonaron)
	        .filter(entry -> jugadoresActivos.getOrDefault(entry.getKey(), false)) 
	        // Ordenar por puntuación de forma descendente (el de mayor puntuación primero)
	        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
	        // Formatear: Nombre (Puntos)
	        .map(entry -> entry.getKey() + " (" + entry.getValue() + " puntos)")
	        .collect(Collectors.toList());

	    //crear la lista final del orden
	    // - Empieza con el ranking de activos (mayor puntuación a menor).
	    // - Le concatena la lista de perdedoresPartida (ordenados cronológicamente por abandono).
	    
	    // La lista 'perdedoresPartida' ya contiene el orden cronológico de abandono:
	    
	    
	    List<String> ordenFinal = new ArrayList<>();
	    
	    // Añadir primero el ranking por puntos
	    ordenFinal.addAll(rankingActivos);
	    
	    // añadir después los perdedores por abandono (los que se fueron primero van a la derecha)
	    ordenFinal.addAll(perdedoresPartida);

	    //generar el resumen del historial
	    String resumen = String.format("PARTICIPANTES: %s @ GANADOR: %s @ RESULTADO: %s @ FIN: Mazo Agotado @ ORDEN_FINAL: %s", 
	        participantes, ganador, puntuacionesFinales, String.join(" -> ", ordenFinal));

	    //registra el resultado en el CoordinadorPartida para guardarlo en el historial
	    DobbleServer.getCoordinadorPartida().registrarResultado(resumen);

	    // avisa el fin a todos los jugadores que siguen activos
	    notificarATodos("FIN_PARTIDA|Partida finalizada. Ganador: " + ganador + ".|"
	            + puntuacionesFinales);

	    // Actualiza el estado de los jugadores
	    for (ClienteGestorHilos jugador : jugadores) {
	        if (jugador.getPartidaActual() == this) {
	            jugador.setEnPartida(false);
	            jugador.setPartidaActual(null);
	        }
	    }

	    System.out.println("Partida finalizada. Ganador: " + ganador);
	}

	/*
	 * Obtiene una cadena con los nombres de todos los participantes. Ejemplo:
	 * "nombrejugador1, nombrejugador2" (serializa los nombres)
	 */
	private String obtenerListaParticipantes() {
		return this.jugadores.stream().map(ClienteGestorHilos::getNombreUsuario).collect(Collectors.joining(", "));
	}

	// devuelve el ganador único o el empate de ganadores
	private String obtenerGanador() {
		// Filtra las puntuaciones de los jugadores que estaban activos al final del mazo.
	    Map<String, Integer> puntuacionesActivas = puntuaciones.entrySet().stream()
	        .filter(entry -> jugadoresActivos.getOrDefault(entry.getKey(), false))
	        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	    // Caso extremo si todos abandonan antes de acabar el mazo.
	    if (puntuacionesActivas.isEmpty()) {
	        return "Nadie"; 
	    }

	    int maxPuntos = puntuacionesActivas.values().stream().max(Integer::compare).orElse(0);

	    // Encuentra a todos los que tienen la puntuación máxima (empate)
	    List<String> ganadores = puntuacionesActivas.entrySet().stream()
	        .filter(entry -> entry.getValue() == maxPuntos)
	        .map(Map.Entry::getKey)
	        .collect(Collectors.toList());

	    if (ganadores.size() > 1) {
	        return "Empate entre: " + String.join(", ", ganadores);
	    } else {
	        return ganadores.get(0);
	    }
	}
	
	
	private void terminarPartidaPorGanadorUnico(ClienteGestorHilos ganador, String causa) {
	    String nombreGanador = ganador.getNombreUsuario();
	    String puntuacionesFinales = serializarPuntuaciones();
	    // Incluye a todos, activos e inactivos
	    String participantes = obtenerListaParticipantes(); 
	    
	    // Añadir el ganador a la lista de participantes para el historial
	    List<String> listaFinal = new ArrayList<>(perdedoresPartida);
	    listaFinal.add(nombreGanador + " (Ganador)");

	    String resumen = String.format("PARTICIPANTES: %s @ RESULTADO: %s @ FIN: %s @ ORDEN_FINAL: %s", 
	        participantes, puntuacionesFinales, causa, String.join(" -> ", listaFinal));

	    DobbleServer.getCoordinadorPartida().registrarResultado(resumen);

	    String mensaje = String.format("¡Eres el único jugador restante! Has ganado por %s.", 
	        causa.toLowerCase());
	        
	    // Notifica el fin de partida solo al ganador
	    ganador.sendMessage("FIN_PARTIDA|" + mensaje + "|" + puntuacionesFinales);

	    // Actualiza el estado del ganador
	    ganador.setEnPartida(false);
	    ganador.setPartidaActual(null);

	    System.out.printf("Partida finalizada por %s. Ganador: %s.%n", causa.toLowerCase(), nombreGanador);
	}
}
