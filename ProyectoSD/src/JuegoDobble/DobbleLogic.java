package JuegoDobble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/* Contiene las reglas del Dobble
   - Genera el mazo de cartas
   - Reparte cartas
   - Comprueba si el jugador ha identificado el símbolo/número de su carta con la central
*/

public class DobbleLogic {

	/*
	 * mazo/baraja completa, lista de listas. Lista externa de 57 cartas y cada
	 * lista interna tiene 8 números de esa carta.
	 */
	private List<List<Integer>> mazo;
	// constante fija para la baraja
	private static final int ORDEN = 7;
	// constante fija para el tamaño de cada lista interna (números).
	private static final int SIMBOLOS_POR_CARTA = ORDEN + 1;
	// número total de cartas = tamaño del mazo y rango de números.
	// 57 cartas y 57 números (del 1 al 57)
	private static final int NUMERO_TOTAL_SIMBOLOS = ORDEN * ORDEN + ORDEN + 1;

	public DobbleLogic() {
		// genera el mazo
		this.mazo = generarMazoDobble();
		// barajea el mazo de forma aleatoria después de crearlo.
		Collections.shuffle(this.mazo);

		System.out.println(
				"Mazo Dobble generado. Cartas: " + this.mazo.size() + ", Símbolos por carta: " + SIMBOLOS_POR_CARTA);
	}

	/*
	 * Genera el mazo Dobble basado en el Plano Proyectivo de Orden 7. - Se van a
	 * generar 57 cartas, cada una con 8 números distintos (entre el 1 y el 57) y se
	 * va a cumplir que dos cartas cualesquiera, sólo tienen un número en común.
	 */
	public List<List<Integer>> generarMazoDobble() {
		// crea la lista con tamaño 57, para las 57 cartas
		List<List<Integer>> mazoGenerado = new ArrayList<>(NUMERO_TOTAL_SIMBOLOS);
		// El método divide las 57 cartas en tres grupos principales (49 líneas finitas,
		// 7 líneas verticales y 1 línea especial)

		// 1. Grupo
		// cartas de pendientes finitas (49 cartas)
		// estas 49 cartas representan las líneas de la forma y=m*x+k (mod 7)
		for (int m = 0; m < ORDEN; m++) {// m va de [0,...,6], m la pendiente
			// Luego simboloPendiente va a tener valores del [50..56]
			int simboloPendiente = NUMERO_TOTAL_SIMBOLOS - ORDEN + m;

			for (int k = 0; k < ORDEN; k++) { // k va de [0..6], k el desplazamiento
				// crea la lista de tamaño 8, de los 8 números distintos que tendrá cada carta
				List<Integer> carta = new ArrayList<>(SIMBOLOS_POR_CARTA);
				carta.add(simboloPendiente); // el símbolo representa la pendiente m
				// va a ver 7 primeras cartas con el símbolo 50 en común, las 7 siguientes con
				// el 51
				// y así hasta la carta 43 a la 49 que tendrán en común el 56
				for (int x = 0; x < ORDEN; x++) { // x va de [0..6]
					int y = (m * x + k) % ORDEN; // saco coordenada y
					// Fórmula para mapear las coordenadas(x,y)a un símbolo (del 1 al 49)
					int simbolo = y * ORDEN + x + 1;
					carta.add(simbolo);
				}

				mazoGenerado.add(carta); // Añade 49 cartas (7 pendientes(m) * 7 desplazamientos(k))
			}
		}

		// 2. Grupo
		// cartas de pendientes infinitas (7 cartas)
		// estas 7 cartas representan las líneas verticales de la forma x=k
		for (int k = 0; k < ORDEN; k++) { // k va de [0..6], k = la posición vertical de la línea

			List<Integer> carta = new ArrayList<>(SIMBOLOS_POR_CARTA);
			carta.add(NUMERO_TOTAL_SIMBOLOS); // Símbolo 57 (Infinito)

			for (int y = 0; y < ORDEN; y++) { // y va de [0..6], y = coordenada
				// x es constante (línea vertical)
				int x = k;
				int simbolo = y * ORDEN + x + 1; // Símbolos 1..49
				carta.add(simbolo);
			}

			mazoGenerado.add(carta); // Añade 7 cartas, todas comparten el símbolo 57
										// (el 57 es el punto en el infinito de las líneas verticales)
		}

		// 3. Grupo
		// carta final faltante (1 carta)
		// Esta única carta recoge los 7 símbolos de pendiente (50 a 56) y el Símbolo
		// Infinito (57).
		// Esta es la carta que garantiza la coincidencia con las 7 cartas de pendiente
		// infinita.
		List<Integer> cartaFinal = new ArrayList<>(SIMBOLOS_POR_CARTA);
		cartaFinal.add(NUMERO_TOTAL_SIMBOLOS); // Símbolo 57 (Infinito)

		// Símbolos 50 a 56
		for (int m = 0; m < ORDEN; m++) {
			int simboloPendiente = NUMERO_TOTAL_SIMBOLOS - ORDEN + m;
			cartaFinal.add(simboloPendiente);
		}
		mazoGenerado.add(cartaFinal); // Añade 1 carta

		// en total (49 + 7 + 1 = 57)

		return mazoGenerado;
	}

	/*
	 * Saca y devuelve la siguiente carta del mazo. - El remove(0), elimina del mazo
	 * la primera carta y la devuelve con el return, en este caso como la carta es
	 * una lista de 8 enteros, devuelve la lista de números de esta primera carta
	 */
	public List<Integer> repartirCarta() {
		if (!mazo.isEmpty()) {
			return mazo.remove(0);
		}
		return null;
	}

	/*
	 * Verifica que el símbolo seleccionado esté en ambas cartas. Esta es la lógica
	 * de validación.
	 */
	public boolean esCoincidenciaValida(int simbolo, List<Integer> cartaJugador, List<Integer> cartaCentral) {
		if (cartaJugador == null || cartaCentral == null) {
			return false;
		}

		// El símbolo debe existir en la carta del jugador y en la carta central
		boolean enJugador = cartaJugador.contains(simbolo);
		boolean enCentral = cartaCentral.contains(simbolo);

		return enJugador && enCentral;
	}
}