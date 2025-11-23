package JuegoDobble;

import java.net.*;
import java.io.*;
import java.util.*;

public class DobbleServer {
    private static final int PUERTO = 12345;
   

    public static void main(String[] args) {
        System.out.println("Servidor Dobble iniciando...");
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor escuchando en el puerto " + PUERTO);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado desde: " + clientSocket.getInetAddress());

               
                
            }
        } catch (IOException e) {
            System.out.println("Error en el servidor principal: " + e.getMessage());
        }
    }

}