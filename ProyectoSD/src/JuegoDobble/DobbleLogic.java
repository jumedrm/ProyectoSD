package JuegoDobble;


import java.util.*;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DobbleLogic {
 private List<List<Integer>> mazo; 

 public DobbleLogic() {
     this.mazo = new ArrayList<>();
     
     
     // 7 cartas, 4 símbolos, prueeeebaaaaa
     mazo.add(Arrays.asList(1, 2, 3, 4));    
     mazo.add(Arrays.asList(1, 5, 6, 7));    
     mazo.add(Arrays.asList(2, 5, 8, 9));    
     mazo.add(Arrays.asList(3, 6, 8, 10));   
     mazo.add(Arrays.asList(4, 7, 9, 11));   
     mazo.add(Arrays.asList(1, 8, 11, 12));  
     mazo.add(Arrays.asList(2, 6, 11, 13));  
     
 }

 //Saca y devuelve la siguiente carta del mazo
 public List<Integer> repartirCarta() {
     if (!mazo.isEmpty()) {
         return mazo.remove(0);
     }
     // deeevuelve null si no hay más cartas.
     return null;
 }


  //Verifica que el símbolo seleccionado esté en ambas cartas
 public boolean esCoincidenciaValida(int simbolo, List<Integer> cartaJugador, List<Integer> cartaCentral) {
     if (cartaJugador == null || cartaCentral == null) {
         return false;
     }

     
     boolean enJugador = cartaJugador.contains(simbolo);
     boolean enCentral = cartaCentral.contains(simbolo);
     
     return enJugador && enCentral; 
 }
}