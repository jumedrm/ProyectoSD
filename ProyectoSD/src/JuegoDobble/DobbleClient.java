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
	
	private static final long serialVersionUID = 1L;
	
	// constantes fijas y para pasarle el mismo puerto al server
	private static final String SERVER_IP = "127.0.0.1";
	private static final int PUERTO = 12345;

	// Contenedor para la carta central
	private JPanel panelCartaCentral;
	// Contenedor para la carta del jugador
	private JPanel panelCartaJugador;

	private JTextArea txtPuntuaciones;
	private JTextArea txtHistorial;
	private JTextArea txtRanking;

	// el socket, para enviar y recibir
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;

	// contenedor principal mainPanel
	private JPanel mainPanel;
	private JTextField txtUsuario;
	private JTextArea logArea;

	// para elegir numero de jugadores por partida
	private JComboBox<Integer> numJugadoresSelector;

	// Estados de la interfaz para saber qué pantalla mostrar
	private static final String VISTA_LOGIN = "Login";
	private static final String VISTA_MENU = "Menu";
	private static final String VISTA_JUEGO = "Juego";
	private static final String VISTA_HISTORIAL = "Historial";
	private static final String VISTA_RANKING = "Ranking";
	private CardLayout cardLayout;

	// Pre: Ninguna.
	// Post: Se lanza el constructor de DobbleClient dentro del Event Dispatch
	// Thread (EDT) de Swing, iniciando la interfaz gráfica.
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

	// Pre: Se llama desde el EDT (Event Dispatch Thread).
	// Post: Se inicializa la ventana principal (JFrame), se configura el
	// CardLayout, se crean e integran las vistas de Login, Menú, Juego e Historial.
	// La vista inicial es VISTA_LOGIN.
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
		// vista del ranking
		mainPanel.add(crearVistaRanking(), VISTA_RANKING);

		// Empezar en la vista de login
		cardLayout.show(mainPanel, VISTA_LOGIN);
		setVisible(true);
	}

	// Pre: Ninguna.
	// Post: Retorna un JPanel configurado para el inicio de sesión, que contiene el
	// campo 'txtUsuario' y el botón "Conectar" asociado a la acción
	// 'intentarConexion()'.
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

	// Pre: Ninguna.
	// Post: Retorna un JPanel que contiene el selector del número de jugadores, el
	// botón "Unirse a Partida" (envía JUGAR|N), "Ver Historial" (envía HISTORIAL) y
	// "Desconectar" (envía DESCONECTAR y sale del sistema), todos con sus Listeners
	// asociados.
	private JPanel crearVistaMenu() {
		// Definición del panel principal del menú con 4 filas
		JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
		panel.setBorder(BorderFactory.createTitledBorder("Menú Principal"));

		// Contenedor de la primera fila: Selector de jugadores y botón Jugar
		JPanel panelJugar = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panelJugar.add(new JLabel("Nº de Jugadores:"));

		// Selector para 2 a 8 jugadores
		Integer[] jugadoresOpciones = { 2, 3, 4, 5, 6, 7, 8 };
		numJugadoresSelector = new JComboBox<>(jugadoresOpciones);
		numJugadoresSelector.setSelectedIndex(0);
		panelJugar.add(numJugadoresSelector);

		JButton btnJugar = new JButton("Unirse a Partida");
		panelJugar.add(btnJugar);

		JButton btnHistorial = new JButton("Ver Historial");
		JButton btnRanking = new JButton("Ver Ranking");
		JButton btnDesconectar = new JButton("Desconectar");

		// AÑADIMOS LOS PANELES Y BOTONES AL GRIDLAYOUT (panel):
		// Fila 1: Selector + Botón Jugar
		panel.add(panelJugar);
		// Fila 2: Botón Historial
		panel.add(btnHistorial);
		// Fila 3: Botón Ranking
		panel.add(btnRanking);
		// Fila 4: Botón Desconectar
		panel.add(btnDesconectar);
		

		// Acción para solicitar jugar (usa el selector)
		btnJugar.addActionListener(e -> {
			int numJugadores = (Integer) numJugadoresSelector.getSelectedItem();
			enviarComando("JUGAR|" + numJugadores);
		});

		btnDesconectar.addActionListener(e -> {
			enviarComando("DESCONECTAR");
			System.exit(0);
		});

		// Acción para solicitar el historial
		btnHistorial.addActionListener(e -> enviarComando("HISTORIAL"));
		
		// Acción para solicitar el ranking
		btnRanking.addActionListener(e -> enviarComando("RANKING"));

		return panel;
	}

	// Pre: Ninguna.
	// Post: Retorna un JPanel que contiene el área de texto 'txtHistorial' y el
	// botón "Volver al Menú" para la navegación.
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
	
	// Pre: Ninguna.
	// Post: Retorna un JPanel que contiene el área de texto 'txtRanking' y el
	// botón "Volver al Menú" para la navegación.
	private JPanel crearVistaRanking() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Ranking de Victorias"));

		txtRanking = new JTextArea();
		txtRanking.setEditable(false);

		// Acción para solicitar volver al menú
		JButton btnVolver = new JButton("Volver al Menú");
		btnVolver.addActionListener(e -> cardLayout.show(mainPanel, VISTA_MENU));

		panel.add(new JScrollPane(txtRanking), BorderLayout.CENTER);
		panel.add(btnVolver, BorderLayout.SOUTH);
		return panel;
	}

	// Pre: Ninguna.
	// Post: Retorna un JPanel con el layout de juego que incluye: el área de
	// 'logArea', el área de 'txtPuntuaciones', los contenedores para las cartas
	// ('panelCartaCentral' y 'panelCartaJugador') y el botón "Rendirse y Salir"
	// (envía RENDIRSE).
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

	// Pre: 'panel' es un contenedor de carta (panelCartaCentral o
	// panelCartaJugador) y 'simbolosStr' es una cadena de símbolos separados por
	// comas.
	// Post: Se eliminan todos los componentes del 'panel', y se añaden nuevos
	// JButtons, uno por cada símbolo. Cada botón está configurado para enviar el
	// comando "INTENTO|Simbolo" al servidor al ser pulsado. El panel se repinta.
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

	// Pre: 'puntuacionesStr' es una cadena de texto en formato serializado
	// "nombre1:puntos1,nombre2:puntos2,...".
	// Post: El área de texto 'txtPuntuaciones' es actualizada con una
	// representación legible y formateada de las puntuaciones de los jugadores.
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

	// Pre: El campo 'txtUsuario' contiene el nombre de usuario que el cliente desea
	// usar.
	// Post: Si el nombre de usuario no está vacío, se intenta establecer una
	// conexión Socket con el servidor. Si es exitosa, se inicializan los streams
	// 'out' y 'in', se envía el nombre de usuario y se inicia un nuevo hilo
	// (ClienteHiloEscucha) para procesar las respuestas del servidor. Si falla, se
	// muestra un mensaje de error.
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

	// Pre: 'comando' es una cadena de texto que sigue el protocolo del servidor
	// (ej: JUGAR|3, INTENTO|15).
	// Post: Si el stream 'out' está inicializado (hay conexión), el 'comando' se
	// envía al servidor para su procesamiento.
	public void enviarComando(String comando) {
		if (out != null) {
			out.println(comando);
		}
	}

	// Pre: 'respuesta' es una cadena de texto recibida del servidor, siguiendo el
	// protocolo COMMAND|DATOS. Este método se ejecuta en el EDT.
	// Post: Se analiza el comando recibido ('accion') y se actualiza la GUI de
	// acuerdo al estado del juego (cambio de vista a menú/juego/historial,
	// actualización de logs, dibujo de cartas, o visualización de mensajes de
	// error/fin de partida).
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
			case "EVENTO_ABANDONO":
				// EVENTO_ABANDONO|TIPO|NOMBRE_JUGADOR
				String tipo = partes[1]; // RENDICION o DESCONEXION
				String nombreJugador = partes[2];

				logArea.append(String.format(">>>>> [ALERTA] %s se ha %s. La partida continúa. <<<<<\n", nombreJugador,
						tipo.equals("RENDICION") ? "rendido" : "desconectado"));

				// No hay que hacer nada más, la partida sigue.
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
			case "RANKING": // <--- NUEVO CASO
				cardLayout.show(mainPanel, VISTA_RANKING);

				// Contiene todos los resúmenes unidos por ','
				String datosRanking = partes[1];

				if (datosRanking.equals("NO_DATA")) {
					txtRanking.setText("Aún no se ha registrado ninguna victoria.");
				} else {
					// El formato es "nombre1:victorias1,nombre2:victorias2,..."
					String[] rankingJugadores = datosRanking.split(",");
					StringBuilder sb = new StringBuilder();

					sb.append("---------------------------------\n");
					sb.append(String.format("%-4s %-20s %s\n", "POS", "JUGADOR", "VICTORIAS"));
					sb.append("---------------------------------\n");

					for (int i = 0; i < rankingJugadores.length; i++) {
						String[] datos = rankingJugadores[i].split(":");
						if (datos.length == 2) {
							String nombre = datos[0];
							String victorias = datos[1];
							sb.append(String.format("%-4d %-20s %s\n", (i + 1), nombre, victorias));
						}
					}
					sb.append("---------------------------------\n");
					txtRanking.setText(sb.toString());
				}
				break;
			default:
				logArea.append("[SERVIDOR] Respuesta: " + respuesta + "\n");
				break;
			}
		});
	}

}
