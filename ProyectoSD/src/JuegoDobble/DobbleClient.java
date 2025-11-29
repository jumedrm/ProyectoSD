package JuegoDobble;


import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

/*
Es el cliente, ejecuta la interfaz gráfica,
gestiona la conexión y traduce los mensajes del servidor 
en botones, puntuaciones, cambios de pantalla...
*/
public class DobbleClient extends JFrame {
	// constantes fijas y para pasarle el mismo puerto al server
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PUERTO = 12345;
    
    //el socket, para enviar y recibir
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // contenedor principal mainPanel
    private JPanel mainPanel;
    private JTextField txtUsuario;
    private JTextArea logArea;
    
    // Estados de la interfaz para saber qué pantalla mostrar
    private static final String VISTA_LOGIN = "Login";
    private static final String VISTA_MENU = "Menu";
    private CardLayout cardLayout;

    
    //el main se encarga de que todo funcione por el hilo de la interfaz gráfica, comenzando a gestionar este hilo el constructor de la clase
    // a partir de ahí, gestiona ese hilo de la interfaz gráfica
    public static void main(String[] args) {
        // crear una nueva clase anónima que implementa la interfaz Runnable.
        Runnable iniciador = new Runnable() {
            @Override
            public void run() {
                // llama al constructor y crea la ventana
                new DobbleClient();
            }
        };
        
        // Pone la iniciador en la cola del Hilo de Despacho de Eventos (EDT).
        SwingUtilities.invokeLater(iniciador);
    }
    
    //crea la ventana principal.
    //el constructor que es llamado por el main
    public DobbleClient() {
        setTitle("Dobble Online - Cliente");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        
        // Usa CardLayout para cambiar entre vistas (Login, Menú, Juego)
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        add(mainPanel);

        // vista de Login
        mainPanel.add(crearVistaLogin(), VISTA_LOGIN);
        // Vista del menú
        mainPanel.add(crearVistaMenu(), VISTA_MENU); 
        
        // Inicializar log (necesario para la conexión)
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
     
        // Empezar en la vista de login 
        cardLayout.show(mainPanel, VISTA_LOGIN);
        setVisible(true);
    }

    // crea la ventana para el login
    private JPanel crearVistaLogin() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(new JLabel("Nombre de Usuario:"));
        txtUsuario = new JTextField(20);
        panel.add(txtUsuario);
        JButton btnConectar = new JButton("Conectar");
        panel.add(btnConectar);
        
        // Acción para solicitar conectarse al logearse
        btnConectar.addActionListener(e -> intentarConexion());
        panel.add(new JScrollPane(logArea)); 
        return panel;
    }

    // crea la ventana para el menú
    private JPanel crearVistaMenu() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Menú Principal - Conexión Exitosa"), BorderLayout.CENTER);
        return panel;
    }
    
    // establece conexión con el servidor
    private void intentarConexion() {
        String usuario = txtUsuario.getText().trim();
        if (usuario.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe introducir un nombre de usuario.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            socket = new Socket(SERVER_IP, PUERTO);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // envía el nombre de usuario al servidor
            out.println(usuario); 

            // inicia el hilo de escucha para recibir respuestas del servidor
            // se hace en el hilo secundario al crear ClienteHiloEscucha, para que no se quede bloqueado
            new Thread(new ClienteHiloEscucha(this, in)).start();
            logArea.append("[CONEXIÓN] Intentando Login...\n");

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor: " + e.getMessage(), "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }
    
 // envía instrucciones al server
    public void enviarComando(String comando) {
        if (out != null) {
            out.println(comando);
        }
    }


    // lo usa el hilo secundario ClienteHiloEscucha cuando el servidor envía un mensaje
    public void procesarRespuesta(String respuesta) {
        // SwingUtilities.invokeLater es crucial para que la GUI no falle, es su hilo
        SwingUtilities.invokeLater(() -> {
            logArea.append("[SERVIDOR] " + respuesta + "\n");
            
            if (respuesta.equals("LOGIN_OK")) {
                // Si el servidor valida, cambiamos al menú
                cardLayout.show(mainPanel, VISTA_MENU); 
                setTitle("Dobble Online - " + txtUsuario.getText());
            }
        });
    }

}