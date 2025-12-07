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
	// Lista para registrar el orden de abandono (primero el que se rinde/desconecta
	// primero)
	private List<String> perdedoresPartida;

	// Pre: 'jugadores' es una lista de ClienteGestorHilos con N >= 2 jugadores
	// listos para empezar a jugar.
	// Post: Se inicializan las estructuras de datos (puntuaciones,
	// jugadoresActivos, perdedoresPartida) y la instancia de DobbleLogic. El estado
	// 'enPartida' de cada hilo en 'jugadores' se establece a 'true' y su
	// 'partidaActual' se vincula a esta instancia. Finalmente, se llama a
	// 'inicializarJuego()' para repartir las cartas iniciales.
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

	// Pre: Se llama al inicio de la partida. El mazo de 'logica' debe contener
	// suficientes cartas (N+1, donde N es el número de jugadores).
	// Post: Se extrae una carta para la 'cartaCentral' y una carta para cada
	// jugador, almacenándolas en 'cartasJugadores'. Se envía el comando
	// "INICIO_PARTIDA|..." a cada cliente con sus respectivas cartas y
	// puntuaciones. Si no hay suficientes cartas, se notifica un error y la partida
	// se cancela.
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

	// Pre: 'mensaje' es una cadena de texto (comando de protocolo) a enviar.
	// Post: El 'mensaje' se envía a todos los hilos de la lista 'jugadores' cuya
	// referencia 'partidaActual' sea esta instancia ('this'), asegurando que los
	// clientes que ya se rindieron no reciban mensajes.
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

	// Pre: 'carta' es una lista de enteros que representan los símbolos de una
	// carta o 'null'.
	// Post: Retorna una cadena de texto con los símbolos de la lista unidos por
	// comas. Retorna una cadena vacía si la lista es nula o vacía.
	private String serializarCarta(List<Integer> carta) {
		// Convierte List<Integer> a String ("1,2,3,4,5,6,7,8")
		if (carta == null) {
			// En caso de mazo vacío o error
			return "";
		}
		return carta.stream().map(Object::toString).collect(Collectors.joining(","));
	}

	// Pre: La estructura 'puntuaciones' está inicializada y contiene las
	// puntuaciones actuales.
	// Post: Retorna una cadena de texto que representa el estado completo de
	// 'puntuaciones' en formato serializado "nombre1:puntos1,nombre2:puntos2,...".
	private String serializarPuntuaciones() {
		// serializa las puntuaciones 'nombre1:puntuaciónX,nombre2:puntuaciónY'
		return puntuaciones.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue())
				.collect(Collectors.joining(","));
	}

	// Pre: 'perdedor' es el hilo del cliente que ha cerrado la conexión (llamado
	// desde el bloque finally de ClienteGestorHilos).
	// Post: El jugador es marcado como inactivo en 'jugadoresActivos'. El evento y
	// el jugador son añadidos a 'perdedoresPartida'. Se notifica a todos los
	// jugadores activos restantes mediante "EVENTO_ABANDONO|DESCONEXION". El estado
	// del hilo 'perdedor' es limpiado (enPartida=false, partidaActual=null).
	// Finalmente, se verifica si la partida debe terminar llamando a
	// 'verificarFinDePartidaPorAbandono()'.
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

		// limpieza del hilo perdedor (ya que el hilo ya está en 'finally' de
		// ClienteGestorHilos)
		perdedor.setEnPartida(false);
		perdedor.setPartidaActual(null);

		// Verificar si solo queda un jugador (el ganador)
		verificarFinDePartidaPorAbandono("Abandono (Desconexión)");
	}

	// Pre: 'perdedor' es el hilo del cliente que ha enviado el comando "RENDIRSE".
	// Post: El jugador es marcado como inactivo en 'jugadoresActivos'. El evento y
	// el jugador son añadidos a 'perdedoresPartida'. Se notifica a todos los
	// jugadores activos restantes mediante "EVENTO_ABANDONO|RENDICION". El cliente
	// 'perdedor' recibe un mensaje de fin de partida y su estado de hilo es
	// limpiado. Finalmente, se verifica si la partida debe terminar llamando a
	// 'verificarFinDePartidaPorAbandono()'.
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

	// Pre: Se llama después de que un jugador se rinde o se desconecta.
	// Post: Si el número de jugadores activos es menor o igual a 1:
	// - Si se encuentra un ganador único, se llama a
	// 'terminarPartidaPorGanadorUnico()'.
	// - Si no queda ningún jugador activo (todos abandonaron), se llama a
	// 'terminarPartidaSinGanador()'.
	private void verificarFinDePartidaPorAbandono(String causaAbandono) {
		long activos = jugadoresActivos.values().stream().filter(b -> b).count();

		if (activos <= 1) {
			// Encontrar al único ganador (si lo hay)
			ClienteGestorHilos ganador = jugadores.stream()
					.filter(hilo -> jugadoresActivos.getOrDefault(hilo.getNombreUsuario(), false)).findFirst()
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

	// Pre: Se llama cuando el contador de jugadores activos llega a cero (todos
	// abandonaron).
	// Post: Se genera un resumen de la partida sin ganador y se registra en el
	// historial del coordinador. Se limpian los estados de los hilos remanentes.
	private void terminarPartidaSinGanador(String causa) {
		String participantes = obtenerListaParticipantes();
		String resumen = String.format("PARTICIPANTES: %s @ RESULTADO: %s @ FIN: %s", participantes,
				serializarPuntuaciones(), causa);

		DobbleServer.getCoordinadorPartida().registrarResultado(resumen);

		// Limpieza de todos los hilos (aunque ya se hizo al abandonar/desconectar)
		for (ClienteGestorHilos jugador : jugadores) {
			if (jugador.getPartidaActual() == this) {
				jugador.setEnPartida(false);
				jugador.setPartidaActual(null);
				// Solo notificar a los que no han sido notificados (ya se hizo en
				// procesarRendicion)
			}
		}
		System.out.println("Partida finalizada. Causa: " + causa);
	}

	// Pre: 'jugador' es un hilo de cliente activo en esta partida, y 'simbolo' es
	// el entero que representa la carta pulsada. 'cartaCentral' no debe ser nulo.
	// Post: Se valida la coincidencia. Si es correcta: la puntuación del jugador se
	// incrementa, se notifica el punto a todos, se actualizan las cartas (la
	// central pasa al jugador, se reparte una nueva central) y, si el mazo se
	// agota, se llama a 'terminarPartida()', si no, se llama a
	// 'iniciarNuevaRonda()'. Si es incorrecta, se envía un mensaje de error al
	// jugador.
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

	// Pre: Se ha completado el procesamiento de un punto y se ha extraído la nueva
	// 'cartaCentral' (que no es nula).
	// Post: El comando "NUEVA_RONDA|..." se envía a todos los jugadores,
	// conteniendo su nueva carta de mano, la nueva carta central y las puntuaciones
	// actualizadas, iniciando la siguiente ronda.
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

	// Pre: Se llama cuando 'logica.repartirCarta()' retorna nulo (el mazo se ha
	// agotado).
	// Post: Se determina el ganador (o empate) entre los jugadores activos. Se
	// genera el resumen final con el ranking por puntos seguido del orden inverso
	// de abandono, y se registra en 'CoordinadorPartida'. Se notifica el fin a
	// todos los clientes activos y se limpian sus estados.
	private void terminarPartida() {
		String ganador = obtenerGanador();
		String puntuacionesFinales = serializarPuntuaciones();
		String participantes = obtenerListaParticipantes();

		// si es un ganador único, se registra la victoria en el ranking.
		if (!ganador.startsWith("Empate entre") && !ganador.equals("Nadie")) {
			DobbleServer.getRankingGlobal().registrarGanador(ganador); // <-- LÍNEA AÑADIDA
		}

		// crear el ranking de los jugadores que terminaron jugando (activos)
		List<String> rankingActivos = puntuaciones.entrySet().stream()
				.filter(entry -> jugadoresActivos.getOrDefault(entry.getKey(), false))
				// Ordenar por puntuación de forma descendente
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				// Formatear: Nombre (Puntos)
				.map(entry -> entry.getKey() + " (" + entry.getValue() + " puntos)").collect(Collectors.toList());

		// aplicar el orden de abandono inverso
		List<String> ordenFinal = new ArrayList<>();

		// Añadir primero el ranking por puntos
		ordenFinal.addAll(rankingActivos);

		// invertir la lista de perdedores: el último en abandonar va antes.
		List<String> perdedoresInvertidos = new ArrayList<>(perdedoresPartida);
		Collections.reverse(perdedoresInvertidos);

		// añadir los perdedores invertidos. El primero en abandonar irá a la derecha
		// del todo.
		ordenFinal.addAll(perdedoresInvertidos);

		// generar el resumen del historial
		String resumen = String.format(
				"PARTICIPANTES: %s @ GANADOR: %s @ RESULTADO: %s @ FIN: Mazo Agotado @ ORDEN_FINAL: %s", participantes,
				ganador, puntuacionesFinales, String.join(" -> ", ordenFinal));

		DobbleServer.getCoordinadorPartida().registrarResultado(resumen);

		notificarATodos("FIN_PARTIDA|Partida finalizada. Ganador: " + ganador + ".|" + puntuacionesFinales);

		for (ClienteGestorHilos jugador : jugadores) {
			if (jugador.getPartidaActual() == this) {
				jugador.setEnPartida(false);
				jugador.setPartidaActual(null);
			}
		}

		System.out.println("Partida finalizada. Ganador: " + ganador);
	}

	// Pre: La lista 'jugadores' está inicializada.
	// Post: Retorna una cadena de texto que contiene los nombres de todos los
	// jugadores que participaron en la partida, separados por coma y espacio.
	private String obtenerListaParticipantes() {
		return this.jugadores.stream().map(ClienteGestorHilos::getNombreUsuario).collect(Collectors.joining(", "));
	}

	// Pre: Se llama al final de la partida por mazo agotado. 'puntuaciones' está
	// actualizada.
	// Post: Retorna una cadena de texto con el nombre del jugador (o jugadores en
	// caso de empate) con la puntuación más alta, considerando solo a los jugadores
	// que estaban activos al finalizar el mazo. Retorna "Nadie" si todos
	// abandonaron antes del fin.
	private String obtenerGanador() {
		// Filtra las puntuaciones de los jugadores que estaban activos al final del
		// mazo.
		Map<String, Integer> puntuacionesActivas = puntuaciones.entrySet().stream()
				.filter(entry -> jugadoresActivos.getOrDefault(entry.getKey(), false))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		// Caso extremo si todos abandonan antes de acabar el mazo.
		if (puntuacionesActivas.isEmpty()) {
			return "Nadie";
		}

		int maxPuntos = puntuacionesActivas.values().stream().max(Integer::compare).orElse(0);

		// Encuentra a todos los que tienen la puntuación máxima (empate)
		List<String> ganadores = puntuacionesActivas.entrySet().stream().filter(entry -> entry.getValue() == maxPuntos)
				.map(Map.Entry::getKey).collect(Collectors.toList());

		if (ganadores.size() > 1) {
			return "Empate entre: " + String.join(", ", ganadores);
		} else {
			return ganadores.get(0);
		}
	}

	// Pre: Se llama desde 'verificarFinDePartidaPorAbandono()' cuando solo queda un
	// jugador activo ('ganador').
	// Post: Se genera el resumen final con el 'ganador' seguido del orden inverso
	// de abandono, y se registra en el historial. El 'ganador' recibe el mensaje de
	// "FIN_PARTIDA|Ganaste por abandono" y su estado es limpiado.
	private void terminarPartidaPorGanadorUnico(ClienteGestorHilos ganador, String causa) {
		String nombreGanador = ganador.getNombreUsuario();
		String puntuacionesFinales = serializarPuntuaciones();
		String participantes = obtenerListaParticipantes();

		// registra la victoria en el ranking global.
		DobbleServer.getRankingGlobal().registrarGanador(nombreGanador);

		// crear la lista final del orden (ranking)
		List<String> ordenFinal = new ArrayList<>();

		// añadir el ganador al principio (izquierda del todo)
		ordenFinal.add(nombreGanador + " (Ganador - " + puntuaciones.get(nombreGanador) + " puntos)");

		// aplicar el orden de abandono inversoo
		// invertir la lista de perdedores: el último en abandonar va después del
		// ganador.
		List<String> perdedoresInvertidos = new ArrayList<>(perdedoresPartida);
		Collections.reverse(perdedoresInvertidos);

		// añadir los perdedores invertidos. El primero en abandonar irá a la derecha
		// del todo.
		ordenFinal.addAll(perdedoresInvertidos);

		// generar el resumen del historial
		String resumen = String.format("PARTICIPANTES: %s @ RESULTADO: %s @ FIN: %s @ ORDEN_FINAL: %s", participantes,
				puntuacionesFinales, causa, String.join(" -> ", ordenFinal));

		DobbleServer.getCoordinadorPartida().registrarResultado(resumen);

		String mensaje = String.format("¡Eres el único jugador restante! Has ganado por %s.", causa.toLowerCase());

		ganador.sendMessage("FIN_PARTIDA|" + mensaje + "|" + puntuacionesFinales);

		ganador.setEnPartida(false);
		ganador.setPartidaActual(null);

		System.out.printf("Partida finalizada por %s. Ganador: %s.%n", causa.toLowerCase(), nombreGanador);
	}
}
