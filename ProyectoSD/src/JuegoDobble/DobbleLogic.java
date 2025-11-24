package JuegoDobble;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
public class DobbleLogic {
    private List<List<Integer>> mazo; 
    private static final int ORDEN = 7; //para 8 símbolos por carta
    private static final int SIMBOLOS_POR_CARTA = ORDEN + 1; // 8 símbolos
    private static final int NUMERO_TOTAL_SIMBOLOS = ORDEN * ORDEN + ORDEN + 1; // 57 símbolos y 57 cartas

    public DobbleLogic() {
        this.mazo = generarMazoDobble();
        // se barajea el mazo después de generarlo
        Collections.shuffle(this.mazo); 
        
        System.out.println("Mazo Dobble generado. Cartas: " + this.mazo.size() + 
                           ", Símbolos por carta: " + SIMBOLOS_POR_CARTA);
    }

 
     //genera el mazo Dobble basado en el Plano Proyectivo de Orden 7.
   
    public List<List<Integer>> generarMazoDobble() {

        List<List<Integer>> mazoGenerado = new ArrayList<>(NUMERO_TOTAL_SIMBOLOS);

        for (int m = 0; m < ORDEN; m++) {

        	int simboloPendiente = NUMERO_TOTAL_SIMBOLOS - ORDEN + m; 

            for (int k = 0; k < ORDEN; k++) { 

                List<Integer> carta = new ArrayList<>(SIMBOLOS_POR_CARTA);
                carta.add(simboloPendiente); 

                for (int x = 0; x < ORDEN; x++) { 
                    int y = (m * x + k) % ORDEN;
                    
                    int simbolo = y * ORDEN + x + 1; 
                    carta.add(simbolo);
                }

                mazoGenerado.add(carta); 
            }
        }


      
        
        for (int k = 0; k < ORDEN; k++) { 

            List<Integer> carta = new ArrayList<>(SIMBOLOS_POR_CARTA);
            carta.add(NUMERO_TOTAL_SIMBOLOS); 

            for (int y = 0; y < ORDEN; y++) { 

            	int x = k; 
                int simbolo = y * ORDEN + x + 1; 
                carta.add(simbolo);
            }

            mazoGenerado.add(carta);
        }

       
        
        List<Integer> cartaFinal = new ArrayList<>(SIMBOLOS_POR_CARTA);
        cartaFinal.add(NUMERO_TOTAL_SIMBOLOS); 

        for (int m = 0; m < ORDEN; m++) {
            int simboloPendiente = NUMERO_TOTAL_SIMBOLOS - ORDEN + m;
            cartaFinal.add(simboloPendiente);
        }
        mazoGenerado.add(cartaFinal); 


        return mazoGenerado;
    }
    
  //saca y devuelve la siguiente carta del mazo.
    public List<Integer> repartirCarta() {
        if (!mazo.isEmpty()) {
            return mazo.remove(0);
        }
        return null;
    }

   // comprueba que el símbolo seleccionado esté en ambas cartas.
    public boolean esCoincidenciaValida(int simbolo, List<Integer> cartaJugador, List<Integer> cartaCentral) {
        if (cartaJugador == null || cartaCentral == null) {
            return false;
        }

        boolean enJugador = cartaJugador.contains(simbolo);
        boolean enCentral = cartaCentral.contains(simbolo);
        
        return enJugador && enCentral; 
    }
}