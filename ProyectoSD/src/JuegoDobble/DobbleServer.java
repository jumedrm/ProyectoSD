package JuegoDobble;

import java.net.*;
import java.io.*;
import java.util.*;

//servidor que crea y gestiona los hilos para comunicarse con el cliente (ClienteGestorHilos)
public class DobbleServer {

	private static final int PUERTO = 12345;
	// lista maestra de todos los clientes activos. el set asegura que no haya
	// duplicados y
	// Collections.synchronizedSet(...) asegura añadir o eliminar elementos aunque
	// lo hagan muchos hilos a la vez
	private static Set<ClienteGestorHilos> clientesConectados = Collections.synchronizedSet(new HashSet<>());
	//instancia de CoordinadorPartida para poder jugar varias partidas a la vez
	private static CoordinadorPartida coordinadorPartida = new CoordinadorPartida();

	// Pre: Ninguna. El sistema operativo debe permitir la apertura del puerto
	// definido (PUERTO = 12345).
	// Post: El servidor se inicia y comienza a escuchar indefinidamente en el
	// puerto especificado. Por cada conexión de cliente entrante, se crea, se añade
	// a 'clientesConectados' y se arranca un nuevo hilo (ClienteGestorHilos) para
	// gestionarla. El bucle de aceptación solo finaliza si ocurre una IOException
	// grave.
	public static void main(String[] args) {
		System.out.println("Servidor Dobble iniciando...");
		try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
			System.out.println("Servidor escuchando en el puerto " + PUERTO);
			while (true) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("Cliente conectado desde: " + clientSocket.getInetAddress());

				ClienteGestorHilos hilo = new ClienteGestorHilos(clientSocket);
				clientesConectados.add(hilo);
				hilo.start();
			}
		} catch (IOException e) {
			System.out.println("Error en el servidor principal: " + e.getMessage());
		}
	}

	// Pre: 'cliente' es una instancia válida de ClienteGestorHilos que previamente
	// estuvo en 'clientesConectados'.
	// Post: La instancia 'cliente' es eliminada del conjunto 'clientesConectados'.
	// Además, se llama a 'coordinadorPartida.removerJugador(cliente)' para asegurar
	// que el cliente sea eliminado de cualquier sala de espera.
	public static void removerCliente(ClienteGestorHilos cliente) {
		clientesConectados.remove(cliente);
		coordinadorPartida.removerJugador(cliente);
		System.out.println("Cliente desconectado. Conexiones activas: " + clientesConectados.size());
	}

	// Pre: Ninguna.
	// Post: Retorna un entero igual al número actual de hilos (clientes) presentes
	// en el conjunto 'clientesConectados'.
	public static int getClientesConectadosCount() {
		return clientesConectados.size();
	}

	// Pre: Ninguna.
	// Post: Retorna la única instancia estática y global de CoordinadorPartida
	// utilizada por el servidor para la gestión de salas e historial.
	public static CoordinadorPartida getCoordinadorPartida() {
		return coordinadorPartida;
	}
}
