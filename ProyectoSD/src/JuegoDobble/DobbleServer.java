package JuegoDobble;

import java.net.*;
import java.io.*;
import java.util.*;

//servidor que crea y gestiona los hilos para comunicarse con el cliente (ClienteGestorHilos)
public class DobbleServer {
	
 private static final int PUERTO = 12345;
 //lista maestra de todos los clientes activos. el set asegura que no haya duplicados y 
 //Collections.synchronizedSet(...) asegura añadir o eliminar elementos aunque lo hagan muchos hilos a la vez
 private static Set<ClienteGestorHilos> clientesConectados = Collections.synchronizedSet(new HashSet<>());
//instancia de CoordinadorPartida para poder jugar varias partidas a la vez
 private static CoordinadorPartida coordinadorPartida = new CoordinadorPartida();

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

 
 // Método para eliminar un cliente desconectado
 public static void removerCliente(ClienteGestorHilos cliente) {
     clientesConectados.remove(cliente);
     coordinadorPartida.removerJugador(cliente);
     System.out.println("Cliente desconectado. Conexiones activas: " + clientesConectados.size());
 }
 
 //Devuelve el número actual de clientes conectados.
 public static int getClientesConectadosCount() {
     return clientesConectados.size();
 }

 // Devuelve la instancia única del CoordinadorPartida
 public static CoordinadorPartida getCoordinadorPartida() {
     return coordinadorPartida;
 }
}
