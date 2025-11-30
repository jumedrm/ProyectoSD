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
public class ClienteGestorHilos extends Thread{
	//conexión con el cliente
   private Socket clientSocket; 
   //Stream para enviar datos (mensajes) al cliente.
   private PrintWriter out;
   //Stream para leer datos (comandos) que vienen del cliente.
   private BufferedReader in;
   //Almacena el nombre que el cliente proporciona al iniciar sesión.
   private String nombreUsuario;
   //Indicador booleano que es true si el jugador está jugando o esperando en una sala,
   //y false si está en el menú principal.
   private boolean enPartida = false;
   //Mantiene una referencia a la instancia específica de DobblePartida
   //en la que está jugando este cliente.
   private DobblePartida partidaActual = null;
   
   //constructor: Se ejecuta una única vez, inmediatamente después de que
   //DobbleServer acepta una nueva conexión.
   public ClienteGestorHilos(Socket socket) {
	 //Asigna el socket recién aceptado en el servidor a la variable de la clase correspondiente.
   	this.clientSocket = socket;
       try {
           // Inicialización de los streams en el constructor
           this.out = new PrintWriter(clientSocket.getOutputStream(), true);
           this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
       } catch (IOException e) {
           System.err.println("Error al inicializar streams para el cliente: " + e.getMessage());
       }
   }
    
   //para obtener el nombre de usuario
   public String getNombreUsuario() {
       return nombreUsuario;
   }
   
   // para cambiar a true o false el booleano enPartida, dependiendo si está o no en una partida
   public void setEnPartida(boolean enPartida) {
       this.enPartida = enPartida;
   }
   
   //método usado por DobblePartida para asignar partida a un hilo
   public void setPartidaActual(DobblePartida partida) {
       this.partidaActual = partida;
   }
   
   //método usado por DobblePartida para consultar a qué partida pertenece el hilo
   public DobblePartida getPartidaActual() {
       return partidaActual;
   }
   
   // envía datos y mensajes al cliente
   public void sendMessage(String message) {
       if (out != null) { 
           out.println(message);
       }
   }
   
    @Override
    public void run() {
    	// En el log, usamos clientSocket que es la variable de instancia
        System.out.println("Cliente conectado desde: " + clientSocket.getInetAddress());
        
        try {
            // Inicio de sesión del cliente
            this.nombreUsuario = in.readLine();
            System.out.println("Usuario logueado: " + nombreUsuario);
            sendMessage("LOGIN_OK"); //le confirma al cliente
            
            String linea;
            // mientras la conexión esté abierta y haya datos para leer se ejecuta todo el rato el bucle
            while ((linea = in.readLine()) != null) { 
                System.out.println("Comando de " + nombreUsuario + ": " + linea);
                manejarComando(linea);
            }
        } catch (IOException e) {
        	// La excepción se lanza cuando el cliente cierra la ventana (la X) por ejemplo.
            System.out.println(nombreUsuario + " ha perdido la conexión.");
        } finally {          
        	// Quita el hilo de la lista de hilos que hay en DobbleServer llamando a removerCliente y cierra el socket
            DobbleServer.removerCliente(this); 
            try { clientSocket.close(); 
            } catch (IOException e) 
            {
            	 e.printStackTrace();
            }
        }
    } 
    
    
     //Procesa los comandos recibidos del cliente (JUGAR, INTENTO,...)
    private void manejarComando(String comando) {
        String[] partes = comando.split("\\|");
        String accion = partes[0];
        
        switch (accion) {
            case "JUGAR":
                
                int numJugadores = 2; 
                try {
                    if (partes.length > 1) {
                        numJugadores = Integer.parseInt(partes[1]);
                    }
                } catch (NumberFormatException e) {
                    sendMessage("ERROR|Formato de JUGAR incorrecto. Usando 2 jugadores.");
                }
                
                if (numJugadores < 2 || numJugadores > 8) {
                    sendMessage("ERROR|Número de jugadores debe ser entre 2 y 8.");
                    break;
                }

                if (!enPartida) { 
                    DobbleServer.getCoordinadorPartida().joinWaitingList(this, numJugadores); 
                } else {
                    sendMessage("ERROR|Ya estás en una partida o sala de espera.");
                }
                break;
                
            case "DESCONECTAR":
                 try { 
                	 clientSocket.close(); 
                 } catch (IOException e) {
                	 e.printStackTrace();
                 }
                 break;

            case "INTENTO":
            	//usa DobblePartida
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

            case "HISTORIAL":
            	//llama a GameManager para obtener el historial
                String historial = DobbleServer.getCoordinadorPartida().getHistorial();
                sendMessage(historial);
                break;
            case "RENDIRSE":
            	//usa DobblePartida
                if (enPartida && partidaActual != null) {
                    partidaActual.procesarRendicion(this); 
                } else {
                    sendMessage("ERROR|No puedes rendirte, no estás en una partida activa.");
                    sendMessage("FIN_PARTIDA|Te hemos devuelto al menú principal.|"); 
                }
                break;


            default:
                 sendMessage("ERROR|Comando desconocido: " + accion);
                 break;
        }
    }
}