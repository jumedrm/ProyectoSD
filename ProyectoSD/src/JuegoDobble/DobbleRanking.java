package JuegoDobble;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/*
Gestor del ranking global de jugadores, almacenando el número de partidas ganadas.
Utiliza un mapa sincronizado para garantizar la seguridad en entorno concurrente.
*/
public class DobbleRanking {

	// Almacena las partidas ganadas por cada jugador: Nombre -> Partidas Ganadas
	private final Map<String, Integer> ranking = Collections.synchronizedMap(new LinkedHashMap<>());

	// Pre: 'nombreGanador' es una cadena de texto válida.
	// Post: La puntuación del jugador es incrementada en 1. Si no existía, se añade
	// con 1 punto.
	public void registrarGanador(String nombreGanador) {
		synchronized (ranking) {
			// Usa getOrDefault para obtener el valor actual (o 0 si es nuevo) y luego suma
			// 1.
			int victoriasActuales = ranking.getOrDefault(nombreGanador, 0);
			ranking.put(nombreGanador, victoriasActuales + 1);
		}
	}

	// Pre: Ninguna.
	// Post: Retorna una cadena de texto que representa el ranking completo de
	// partidas ganadas, ordenado de forma descendente por victorias.
	// Formato: "nombre1:victorias1,nombre2:victorias2,..."
	public String getRankingSerializado() {
		// Crea un stream de las entradas del ranking
		return ranking.entrySet().stream()
				// Ordena por el valor (victorias) de forma descendente
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				// Mapea a la cadena "nombre:victorias"
				.map(e -> e.getKey() + ":" + e.getValue())
				// Une todas las cadenas con comas
				.collect(Collectors.joining(","));
	}
}