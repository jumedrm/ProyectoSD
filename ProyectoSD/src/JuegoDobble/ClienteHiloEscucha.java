package JuegoDobble;

import java.io.*;
import java.net.Socket;

//Hilo dedicado a escuchar permanentemente mensajes del servidor al cliente
public class ClienteHiloEscucha implements Runnable {

	// instancia del cliente para pillar sus métodos, como procesarRespuesta
	private DobbleClient gui;
	// para recibir datos del server
	private BufferedReader in;

	// inicializa las variables en el constructor
	public ClienteHiloEscucha(DobbleClient gui, BufferedReader in) {
		this.gui = gui;
		this.in = in;
	}

	// comportamiento del hilo: recibir todo el rato mensajes, pero se bloquea y así
	// no se bloquea el cliente/gui/interfaz gráfica
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