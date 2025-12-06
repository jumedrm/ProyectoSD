package JuegoDobble;

import java.util.*;

/*
Coordina la concurrencia y decide cuándo se
cumplen las condiciones para iniciar una partida.
*/
public class CoordinadorPartida {

	// almacena la cola de espera para la partida. Integer es el número deseado de
	// jugadores y
	// List es la lista de hilos de jugadores esperando a unirse a este tipo de
	// partida.
	private Map<Integer, List<ClienteGestorHilos>> salasDeEspera = new HashMap<>();
	// Lista de todas las instancias de DobblePartida (partidas en juego)
	private List<DobblePartida> partidasActivas = new ArrayList<>();
	// almacena los resúmenes de las partidas terminadas. Se usa synchronized en sus
	// métodos de acceso
	// para que varios hilos no intenten leer y escribir al mismo tiempo.
	private List<String> historialPartidas = new ArrayList<>();

	// Pre: Ninguna.
	// Post: La estructura 'salasDeEspera' se inicializa, creando una lista de
	// espera vacía (LinkedList) para cada tamaño de partida posible (2 a 8
	// jugadores).
	public CoordinadorPartida() {
		for (int i = 2; i <= 8; i++) {
			salasDeEspera.put(i, new LinkedList<>());
		}
	}

	// Pre: 'jugador' es un ClienteGestorHilos válido que no está actualmente en una
	// partida o sala de espera; 'maxJugadores' está entre 2 y 8 (esta validación se
	// hace en ClienteGestorHilos).
	// Post: Se sincroniza el acceso a la sala. Si la sala correspondiente a
	// 'maxJugadores' se llena (alcanza o excede el límite), la lista de jugadores
	// se vacía y se llama a 'iniciarNuevaPartida()'. Si la sala no se llena, el
	// jugador se añade a la cola y se le envía un mensaje de espera.
	public void joinWaitingList(ClienteGestorHilos jugador, int maxJugadores) {
		// lista de jugadores(hilos)en un principio vacía, que corresponde al número de
		// jugadores deseado para jugar ese tipo de partida
		List<ClienteGestorHilos> sala = salasDeEspera.get(maxJugadores);

		// para que el sistema no pueda iniciar dos partidas incompletas si dos
		// jugadores se unen al mismo tiempo
		synchronized (sala) {
			// si se pulsa el botón 'jugar' varias veces, no es añadido a la misma lista de
			// espera varias veces
			if (!sala.contains(jugador)) {
				sala.add(jugador);
				System.out.println(
						jugador.getNombreUsuario() + " se unió a sala de " + maxJugadores + ". Total: " + sala.size());
				// si el número de jugadores alcanza o excede el límite
				if (sala.size() >= maxJugadores) {
					// Crea una copia de la lista
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

	// Pre: 'jugador' es una instancia válida de ClienteGestorHilos.
	// Post: Se recorren todas las salas de espera. Si el 'jugador' se encuentra en
	// alguna de ellas, es removido de la lista de espera.
	public void removerJugador(ClienteGestorHilos jugador) {
		// Quitarlo de cualquier sala de espera si está allí
		for (List<ClienteGestorHilos> sala : salasDeEspera.values()) {
			synchronized (sala) {
				sala.remove(jugador);
			}
		}
	}

	// Pre: 'jugadores' es una lista de ClienteGestorHilos cuyo tamaño es igual o
	// mayor al número de jugadores requerido para la partida.
	// Post: Se crea una nueva instancia de 'DobblePartida' con la lista de
	// 'jugadores'. Esta nueva instancia se añade a la lista global
	// 'partidasActivas'.
	private void iniciarNuevaPartida(List<ClienteGestorHilos> jugadores) {
		System.out.println("Iniciando nueva partida con " + jugadores.size() + " jugadores.");
		// crea la partida
		DobblePartida nuevaPartida = new DobblePartida(jugadores);

		// Añade la partida activa a la lista global.
		partidasActivas.add(nuevaPartida);
	}

	// Pre: 'resumenPartida' es una cadena de texto formateada que contiene todos
	// los detalles del resultado de una partida recién terminada.
	// Post: Se añade 'resumenPartida' al final de la lista 'historialPartidas'. La
	// operación se realiza bajo un bloque sincronizado para garantizar la seguridad
	// en un entorno concurrente.
	public void registrarResultado(String resumenPartida) {
		// si dos partidas terminan simultaneamente, el resumen de ambas se guarda bien
		synchronized (historialPartidas) {
			historialPartidas.add(resumenPartida);
		}
	}

	// Pre: Ninguna.
	// Post: Si 'historialPartidas' está vacío, retorna el comando
	// "HISTORIAL|NO_DATA". Si hay datos, retorna el comando "HISTORIAL|" seguido de
	// una cadena que une todos los resúmenes de partida ('resumenPartida')
	// separados por el delimitador '###'.
	public String getHistorial() {
		if (historialPartidas.isEmpty()) {
			return "HISTORIAL|NO_DATA";
		}
		// Unimos todos los resúmenes en una sola cadena, separados por un delimitador
		// ('###')
		String datos = String.join("###", historialPartidas);
		return "HISTORIAL|" + datos;
	}
}
