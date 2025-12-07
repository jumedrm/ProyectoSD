package JuegoDobble;

import java.io.*;

//Hilo dedicado a escuchar permanentemente mensajes del servidor al cliente
public class ClienteHiloEscucha implements Runnable {

	// instancia del cliente para pillar sus métodos, como procesarRespuesta
	private DobbleClient gui;
	// para recibir datos del server
	private BufferedReader in;

	// Pre: 'gui' es una instancia válida de DobbleClient y 'in' es un
	// BufferedReader inicializado, vinculado al InputStream del Socket con el
	// servidor.
	// Post: Se inicializan las variables de instancia 'this.gui' y 'this.in',
	// preparando el hilo para comenzar a escuchar.
	public ClienteHiloEscucha(DobbleClient gui, BufferedReader in) {
		this.gui = gui;
		this.in = in;
	}

	// Pre: El hilo ha sido iniciado por 'new Thread(this).start()'. El
	// BufferedReader 'in' está abierto y conectado al servidor.
	// Post: El hilo se bloquea en 'in.readLine()' esperando indefinidamente
	// respuestas del servidor. Cada respuesta recibida se pasa a
	// 'gui.procesarRespuesta()' para su manejo en el hilo de la GUI (EDT). El bucle
	// finaliza si el servidor cierra la conexión, lanzando una IOException.
	@Override
	public void run() {
		try {
			String response;
			while ((response = in.readLine()) != null) {
				// Pasa la respuesta al método de la gui/cliente para su procesamiento
				gui.procesarRespuesta(response);
			}
		} catch (IOException e) {
			// error de lectura (por ejemplo: servidor cerrado o desconexión forzada)
			System.out.println("Conexión con el servidor perdida.");
		}
	}

}