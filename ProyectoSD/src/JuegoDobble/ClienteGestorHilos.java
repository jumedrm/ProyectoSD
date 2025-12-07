package JuegoDobble;

import java.net.*;
import java.io.*;

/*
Es el agente esencial de la concurrencia en el servidor. 
Su objetivo es aislar la comunicación con un único cliente
en su propio hilo de ejecución, permitiendo que el servidor
atienda a cientos de jugadores simultáneamente sin bloquearse.
*/
//Define la clase como un hilo de ejecución.
//cada instancia de CienteGestorHilos se ejecutará en paralelo a las demás.
public class ClienteGestorHilos extends Thread {
	// conexión con el cliente
	private Socket clientSocket;
	// Stream para enviar datos (mensajes) al cliente.
	private PrintWriter out;
	// Stream para leer datos (comandos) que vienen del cliente.
	private BufferedReader in;
	// Almacena el nombre que el cliente proporciona al iniciar sesión.
	private String nombreUsuario;
	// Indicador booleano que es true si el jugador está jugando o esperando en una
	// sala,
	// y false si está en el menú principal.
	private boolean enPartida = false;
	// Mantiene una referencia a la instancia específica de DobblePartida
	// en la que está jugando este cliente.
	private DobblePartida partidaActual = null;

	// Pre: 'socket' es una instancia de Socket válida y ya aceptada por el
	// ServerSocket.
	// Post: Se inicializan los streams de entrada ('in') y salida ('out') asociados
	// al 'clientSocket'. La conexión está lista para recibir el nombre de usuario y
	// los comandos.
	public ClienteGestorHilos(Socket socket) {
		// Asigna el socket recién aceptado en el servidor a la variable de la clase
		// correspondiente.
		this.clientSocket = socket;
		try {
			// Inicialización de los streams en el constructor
			this.out = new PrintWriter(clientSocket.getOutputStream(), true);
			this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (IOException e) {
			System.err.println("Error al inicializar streams para el cliente: " + e.getMessage());
		}
	}

	// Pre: El hilo ha completado el proceso de login, por lo que 'nombreUsuario' no
	// es nulo.
	// Post: Retorna la cadena de texto que contiene el nombre de usuario de este
	// cliente.
	public String getNombreUsuario() {
		return nombreUsuario;
	}

	// Pre: El valor 'enPartida' es un booleano que indica el nuevo estado del
	// jugador (true si está en juego/sala, false si está en el menú).
	// Post: La variable de estado 'this.enPartida' se actualiza con el valor
	// proporcionado.
	public void setEnPartida(boolean enPartida) {
		this.enPartida = enPartida;
	}

	// Pre: 'partida' es la instancia de DobblePartida a la que el jugador se está
	// uniendo o 'null' si está saliendo.
	// Post: La referencia 'this.partidaActual' se establece a la instancia de la
	// partida o a 'null'.
	public void setPartidaActual(DobblePartida partida) {
		this.partidaActual = partida;
	}

	// Pre: Ninguna.
	// Post: Retorna la instancia de DobblePartida a la que pertenece este jugador o
	// 'null' si no está en ninguna partida.
	public DobblePartida getPartidaActual() {
		return partidaActual;
	}

	// Pre: 'message' es una cadena de texto (comando de protocolo) a enviar al
	// cliente.
	// Post: Si el stream 'out' no es nulo, la cadena 'message' se envía al cliente,
	// seguida de un salto de línea, y se vacía el búfer.
	public void sendMessage(String message) {
		if (out != null) {
			out.println(message);
		}
	}

	// Pre: Los streams 'in' y 'out' han sido inicializados en el constructor.
	// Post: Se completa el proceso de login. El hilo entra en un bucle continuo
	// para leer comandos del cliente. Si el cliente se desconecta o cierra la
	// conexión, se captura la IOException, se llama a
	// 'partidaActual.procesarDesconexion()' (si aplica) y se llama a
	// 'DobbleServer.removerCliente()' para la limpieza final.
	@Override
	public void run() {
		// En el log, usamos clientSocket que es la variable de instancia
		System.out.println("Cliente conectado desde: " + clientSocket.getInetAddress());

		try {
			// inicio de sesión comprobando duplicados
	        String linea;
	        
	        // intentamos leer el nombre de usuario inicial
	        String tempNombreUsuario = in.readLine(); 

	        // mientras se reciba un nombre y haya duplicado pedimos uno nuevo
	        while (tempNombreUsuario != null && DobbleServer.isUsuarioConectado(tempNombreUsuario)) {
	            // notificamos al cliente el error de duplicado
	            sendMessage("ERROR|El usuario '" + tempNombreUsuario + "' ya está conectado.");
	            System.out.println("Intento de login duplicado: " + tempNombreUsuario);
	            // intentamos leer el nuevo nombre
	            tempNombreUsuario = in.readLine(); 
	        }
	        
	        // si el cliente no se desconectó y no es duplicado, lo aceptamos.
	        if (tempNombreUsuario == null) {
	            System.out.println("Cliente desconectado durante la validación.");
	            return; // Salir del método run() si el cliente desconectó
	        }

	        this.nombreUsuario = tempNombreUsuario;

	        System.out.println("Usuario logueado: " + nombreUsuario);
	        sendMessage("LOGIN_OK"); // le confirma al cliente
			while ((linea = in.readLine()) != null) {
				System.out.println("Comando de " + nombreUsuario + ": " + linea);
				manejarComando(linea);
			}
		} catch (IOException e) {
			// La excepción se lanza cuando el cliente cierra la ventana (la X) por ejemplo.
			System.out.println((nombreUsuario != null ? nombreUsuario : "Cliente") + " ha perdido la conexión.");
		} finally {
			if (enPartida && partidaActual != null) {
				// Si el cliente estaba en una partida, la rendición es forzada
				partidaActual.procesarDesconexion(this);
			}
			// Quita el hilo de la lista de hilos que hay en DobbleServer
			DobbleServer.removerCliente(this);

			try {
				// Cerrar el socket
				if (clientSocket != null) {
					clientSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			// método de DobbleServer para el conteo de clientes
			System.out
					.println("Cliente desconectado. Conexiones activas: " + DobbleServer.getClientesConectadosCount());
		}
	}

	// Pre: 'comando' es una cadena de texto recibida del cliente, siguiendo el
	// protocolo COMMAND|DATOS.
	// Post: El comando se parsea y, basado en el valor de la acción (JUGAR,
	// HISTORIAL, RENDIRSE, INTENTO, DESCONECTAR), se redirige la solicitud al
	// CoordinadorPartida o a la instancia de DobblePartida actual. Si el comando es
	// inválido o se produce un error, se envía un mensaje 'ERROR' al cliente.
	private void manejarComando(String comando) {
		String[] partes = comando.split("\\|");
		String accion = partes[0];

		switch (accion) {
		case "JUGAR":
			if (!enPartida && partes.length == 2) {
				try {
					int maxJugadores = Integer.parseInt(partes[1]);
					// Se verifica que el número de jugadores esté en el rango permitido (2 a 8)
					if (maxJugadores >= 2 && maxJugadores <= 8) {
						// Envía el hilo a la sala de espera que hay en CoordinadorPartida
						DobbleServer.getCoordinadorPartida().joinWaitingList(this, maxJugadores);
					} else {
						sendMessage("ERROR|Número de jugadores no válido (2-8).");
					}
				} catch (NumberFormatException e) {
					sendMessage("ERROR|Comando JUGAR inválido. Debe ser JUGAR|N.");
				}
			} else if (enPartida) {
				sendMessage("ESPERA|Ya estás en una sala de espera o partida activa.");
			} else {
				sendMessage("ERROR|Comando JUGAR inválido. Debe ser JUGAR|N.");
			}
			break;
		case "HISTORIAL":
			// llama a CoordinadorPartida para obtener el historial
			String historial = DobbleServer.getCoordinadorPartida().getHistorial();
			sendMessage(historial);
			break;
		case "RANKING":
			// llama a DobbleRanking para obtener la lista serializada de victorias
			String rankingSerializado = DobbleServer.getRankingGlobal().getRankingSerializado();
			if (rankingSerializado.isEmpty()) {
				sendMessage("RANKING|NO_DATA");
			} else {
				sendMessage("RANKING|" + rankingSerializado);
			}
			break;
		case "RENDIRSE":
			// usa DobblePartida
			if (enPartida && partidaActual != null) {
				partidaActual.procesarRendicion(this);
			} else {
				sendMessage("ERROR|No puedes rendirte, no estás en una partida activa.");
				sendMessage("FIN_PARTIDA|Te hemos devuelto al menú principal.|");
			}
			break;
		case "INTENTO":
			// usa DobblePartida
			if (enPartida && partidaActual != null) {
				try {
					int simbolo = Integer.parseInt(partes[1]);
					partidaActual.procesarIntento(this, simbolo);
				} catch (NumberFormatException e) {
					sendMessage("ERROR|Símbolo no válido.");
				}
			} else {
				sendMessage("ERROR|No estás en una partida activa.");
			}
			break;
		case "DESCONECTAR":
			// Finaliza el bucle while y pasa al bloque finally al cerrar el socket
			try {
				if (clientSocket != null) {
					clientSocket.close();
				}
			} catch (IOException e) {
			}
			break;
		default:
			sendMessage("ERROR|Comando desconocido.");
			break;
		}
	}
}