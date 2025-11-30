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

	// Contenedor para la carta central
	private JPanel panelCartaCentral;
	// Contenedor para la carta del jugador
	private JPanel panelCartaJugador;

	private JTextArea txtPuntuaciones;
	private JTextArea txtHistorial;

	// el socket, para enviar y recibir
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
	private static final String VISTA_JUEGO = "Juego";
	private static final String VISTA_HISTORIAL = "Historial";
	private CardLayout cardLayout;

	// el main se encarga de que todo funcione por el hilo de la interfaz gráfica,
	// comenzando a gestionar este hilo el constructor de la clase
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

	// crea la ventana principal.
	// el constructor que es llamado por el main
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
		// vista del juego
		mainPanel.add(crearVistaJuego(), VISTA_JUEGO);
		// vista del historial
		mainPanel.add(crearVistaHistorial(), VISTA_HISTORIAL);

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
		JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
		panel.setBorder(BorderFactory.createTitledBorder("Menú Principal"));

		JButton btnJugar = new JButton("Unirse a Partida (2 Jugadores)");
		JButton btnHistorial = new JButton("Ver Historial");
		JButton btnDesconectar = new JButton("Desconectar");

		panel.add(btnJugar);
		panel.add(btnHistorial);
		panel.add(btnDesconectar);

		// Acción para solicitar jugar y desconectarse
		btnJugar.addActionListener(e -> enviarComando("JUGAR"));
		btnDesconectar.addActionListener(e -> {
			enviarComando("DESCONECTAR");
			System.exit(0);
		});

		// Acción para solicitar el historial
		btnHistorial.addActionListener(e -> enviarComando("HISTORIAL"));

		// se envía la acción al servidor con enviarComando
		return panel;
	}

	// crea la ventana para el historial
	private JPanel crearVistaHistorial() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Historial de Partidas"));

		txtHistorial = new JTextArea();
		txtHistorial.setEditable(false);

		// Acción para solicitar volver al menú
		JButton btnVolver = new JButton("Volver al Menú");
		btnVolver.addActionListener(e -> cardLayout.show(mainPanel, VISTA_MENU));

		panel.add(new JScrollPane(txtHistorial), BorderLayout.CENTER);
		panel.add(btnVolver, BorderLayout.SOUTH);
		return panel;
	}

	// crea la ventana o pantalla del juego, de la partida
	private JPanel crearVistaJuego() {
		JPanel panel = new JPanel(new BorderLayout(5, 5));

		logArea = new JTextArea(5, 50);
		logArea.setEditable(false);
		panel.add(new JScrollPane(logArea), BorderLayout.NORTH);

		// para que salgan las puntuaciones
		txtPuntuaciones = new JTextArea(15, 15);
		txtPuntuaciones.setEditable(false);
		txtPuntuaciones.setBorder(BorderFactory.createTitledBorder("Puntuaciones"));
		panel.add(new JScrollPane(txtPuntuaciones), BorderLayout.WEST);

		// Contenedor principal del juego (Cartas)
		JPanel gameContainer = new JPanel(new GridLayout(1, 2, 20, 20));
		gameContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		// carta central
		panelCartaCentral = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panelCartaCentral.setBorder(BorderFactory.createTitledBorder("Carta Central"));
		gameContainer.add(panelCartaCentral);

		// carta del Jugador
		panelCartaJugador = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panelCartaJugador.setBorder(BorderFactory.createTitledBorder("Mi Carta"));
		gameContainer.add(panelCartaJugador);

		panel.add(gameContainer, BorderLayout.CENTER);
		// Botón de Rendirse
		JButton btnRendirse = new JButton("Rendirse y Salir");
		btnRendirse.addActionListener(e -> {
			// Confirmación antes de rendirse
			int confirm = JOptionPane.showConfirmDialog(DobbleClient.this,
					"¿Estás seguro de que quieres rendirte? La partida se dará por perdida.", "Confirmar Rendición",
					JOptionPane.YES_NO_OPTION);
			// se envía la acción al servidor con enviarComando
			if (confirm == JOptionPane.YES_OPTION) {
				enviarComando("RENDIRSE");
			}
		});

		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		controlPanel.add(btnRendirse);

		panel.add(new JScrollPane(logArea), BorderLayout.NORTH);
		panel.add(new JScrollPane(txtPuntuaciones), BorderLayout.WEST);
		panel.add(gameContainer, BorderLayout.CENTER);
		panel.add(controlPanel, BorderLayout.SOUTH);

		return panel;
	}

	// crea la carta
	private void dibujarCarta(JPanel panel, String simbolosStr) {
		// limpia el panel antes de añadir nuevos botones
		panel.removeAll();

		// Convierte la cadena "1,2,3" en un array de Strings
		String[] simbolos = simbolosStr.split(",");

		// Crea un botón por cada símbolo
		for (String simbolo : simbolos) {
			if (simbolo.isEmpty())
				continue;

			JButton btnSimbolo = new JButton(simbolo);
			// Tamaño para los botones
			btnSimbolo.setPreferredSize(new Dimension(80, 80));

			// acción de enviar el INTENTO al servidor
			btnSimbolo.addActionListener(e -> {
				// El comando va a ser INTENTO|SIMBOLO (ej: INTENTO|2)
				enviarComando("INTENTO|" + simbolo);
				// se envía el intento al servidor
			});

			panel.add(btnSimbolo);
		}

		// Actualiza la vista del panel
		panel.revalidate();
		panel.repaint();
	}

	// método para actualizar las puntuaciones
	private void actualizarPuntuaciones(String puntuacionesStr) {
		// puntuacionesStr "juanlu:1,juan:0"
		StringBuilder sb = new StringBuilder();
		sb.append("--------------------\n");

		// Divide por coma y luego por dos puntos
		for (String par : puntuacionesStr.split(",")) {
			String[] datos = par.split(":");
			if (datos.length == 2) {
				sb.append(String.format("%-10s: %s\n", datos[0], datos[1]));
			}
		}
		sb.append("--------------------\n");

		txtPuntuaciones.setText(sb.toString());
	}

	// establece conexión con el servidor
	private void intentarConexion() {
		String usuario = txtUsuario.getText().trim();
		if (usuario.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Debe introducir un nombre de usuario.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			socket = new Socket(SERVER_IP, PUERTO);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			// envía el nombre de usuario al servidor
			out.println(usuario);

			// inicia el hilo de escucha para recibir respuestas del servidor
			// se hace en el hilo secundario al crear ClienteHiloEscucha, para que no se
			// quede bloqueado
			new Thread(new ClienteHiloEscucha(this, in)).start();

		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor: " + e.getMessage(),
					"Error de Conexión", JOptionPane.ERROR_MESSAGE);
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
		SwingUtilities.invokeLater(() -> {
			String[] partes = respuesta.split("\\|");
			String accion = partes[0];

			switch (accion) {
			case "LOGIN_OK":
				cardLayout.show(mainPanel, VISTA_MENU);
				setTitle("Dobble Online - " + txtUsuario.getText());
				break;
			case "ESPERA":
				logArea.append("[SALA] " + partes[1] + "\n");
				cardLayout.show(mainPanel, VISTA_MENU);
				break;
			case "INICIO_PARTIDA":
			case "NUEVA_RONDA":
				// NUEVA_RONDA|carta_jugador|carta_central|puntuaciones
				if (partes.length < 4) {
					logArea.append("ERROR: Datos de partida incompletos.\n");
					return;
				}
				String cartaJugador = partes[1];
				String cartaCentral = partes[2];
				String puntuacionesStr = partes[3];

				logArea.append("[JUEGO] Nueva Ronda Iniciada!\n");

				// vista del juego al principio
				if (accion.equals("INICIO_PARTIDA")) {
					cardLayout.show(mainPanel, VISTA_JUEGO);
				}

				// dibuja las cartas
				dibujarCarta(panelCartaJugador, cartaJugador);
				dibujarCarta(panelCartaCentral, cartaCentral);

				// actualiza las puntuaciones
				actualizarPuntuaciones(puntuacionesStr);

				break;
			case "PUNTO":
				// PUNTO|ganador|puntuacion_ganador|puntuaciones_totales
				logArea.append(">>>>> ¡" + partes[1].toUpperCase() + " GANA LA RONDA! <<<<<\n");
				actualizarPuntuaciones(partes[3]);
				break;
			case "ERROR_JUEGO":
				logArea.append("[ERROR JUEGO] " + partes[1] + "\n");
				break;
			case "FIN_PARTIDA":
				// FIN_PARTIDA|MensajeGanador|PuntuacionesTotales

				// mensaje de rendición/desconexión
				String mensaje = partes[1];
				String puntuaciones = partes[2];
				
				
				// muestra el mensaje de forma destacada
				JOptionPane.showMessageDialog(DobbleClient.this, mensaje + "\n\nPuntuaciones finales: " + puntuaciones,
						"Partida Finalizada", JOptionPane.INFORMATION_MESSAGE);

				logArea.append("--- FIN DE PARTIDA ---\n");
				logArea.append(mensaje + "\n");

				actualizarPuntuaciones(puntuaciones);

				// Vuelve al menú principal
				cardLayout.show(mainPanel, VISTA_MENU);

				break;
			// Caso para mostrar el historial
			case "HISTORIAL":
				cardLayout.show(mainPanel, VISTA_HISTORIAL);

				// Contiene todos los resúmenes unidos por '###'
				String datosHistorial = partes[1];

				if (datosHistorial.equals("NO_DATA")) {
					txtHistorial.setText("Aún no se ha jugado ninguna partida.");
				} else {
					// Separamos cada resumen de partida ('###')
					String[] partidas = datosHistorial.split("###");

					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < partidas.length; i++) {
						sb.append("--- PARTIDA ").append(i + 1).append(" ---\n");

						String resumenCompleto = partidas[i].trim();

						// reemplazamos el separador " @ " por un salto de línea
						String historialFormateado = resumenCompleto.replace(" @ ", "\n");

						// Imprimimos la cadena formateada
						sb.append(historialFormateado).append("\n");
						// Separador de partidas
						sb.append("---------------------------\n");
					}
					txtHistorial.setText(sb.toString());
				}
				break;
			default:
				logArea.append("[SERVIDOR] Respuesta: " + respuesta + "\n");
				break;
			}
		});
	}

}
