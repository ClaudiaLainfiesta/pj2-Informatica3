import java.io.*;
import java.util.*;

/**
 * Clase para representar un AFN y convertirlo a AFD.
 * NO debe cambiar los nombres de la clase ni de los métodos existentes.
 */
public class AFN{
    // Lectura AFN.
    private String direccionAFN;
    private String[] alfabeto;
    private int cantidadEstadosAFN;
    private int[] estadosFinalAFN;
    private List<List<Integer>> transicionesLambdaAFN;
    private List<List<List<Integer>>> transicionesEstadosAFN;

    // Algoritmo de clausura-λ.
    private Queue<Set<Integer>> conjuntoCreadosPendientes;          
    private Queue<Set<Integer>> conjuntoCreadosProcesados;           
    private Map<Integer, Set<Integer>> mapaClausuras;
    //private Map<Integer, ArrayList<Integer>> mapaAFD = new HashMap<>();
    private int contadorEstadosClausuras = 1;

    //Variables resultado AFD.
    private String direccionAFD;
    private int cantidadEstadosAFD;
    private List<Integer> estadosFinalAFD;
    private Map<Integer, List<List<Integer>>> transicionesEstadosAFD;

    int cantidadFilasMatrizAFD;
    ArrayList<TransicionAFD>[] transicionesEstadoAFD;

    //Lectura cuerdas.
    private Boolean cuerdaAceptada;

    public AFN(String path){
        this.direccionAFN = path;
        this.transicionesLambdaAFN = new ArrayList<>();
        this.transicionesEstadosAFN = new ArrayList<>();
        this.cuerdaAceptada = false;

        this.conjuntoCreadosPendientes = new LinkedList<>();
        this.conjuntoCreadosProcesados = new LinkedList<>();
        this.mapaClausuras = new HashMap<>();

        this.estadosFinalAFD = new ArrayList<>();
        this.transicionesEstadosAFD = new HashMap<>();


    }

    public boolean accept(String string) {
        if (transicionesEstadoAFD == null || transicionesEstadoAFD.length == 0) {
            System.err.println("Error: el AFD no ha sido construido. Ejecuta toAFD() primero.");
            return false;
        }
    
        int estadoActual = 1; // Estado inicial
    
        for (int i = 0; i < string.length(); i++) {
            char simbolo = string.charAt(i);
            int idxSimbolo = getIndiceSimbolo(simbolo);
    
            if (idxSimbolo == -1) {
                System.err.println("Símbolo '" + simbolo + "' no pertenece al alfabeto.");
                return false;
            }
    
            boolean transicionEncontrada = false;
            for (TransicionAFD t : transicionesEstadoAFD[idxSimbolo]) {
                if (t.estadoOrigen == estadoActual) {
                    estadoActual = t.estadoDestino;
                    transicionEncontrada = true;
                    break;
                }
            }
    
            if (!transicionEncontrada) {
                estadoActual = 0; // estado de error
                break;
            }
        }
    
        // Evaluamos si el estado final es aceptado
        return estadosFinalAFD.contains(estadoActual);
    }

    public void toAFD(String afdPath){
        this.direccionAFD = afdPath;
        lecturaAFN();
        Set<Integer> estadoInicial = new HashSet<>();
        estadoInicial.add(1);
        clausura_lambda(estadoInicial);
        
        ArrayList<TransicionAFD>[] transicionesEstadoAFD = new ArrayList[alfabeto.length];
        for (int i = 0; i < alfabeto.length; i++) {
            transicionesEstadoAFD[i] = new ArrayList<>();
        }
    
        while (!conjuntoCreadosPendientes.isEmpty()) {
            Set<Integer> estadoActual = conjuntoCreadosPendientes.poll();
            int nombreEstadoActual = obtenerEstado(estadoActual);
    
            for (int i = 0; i < alfabeto.length; i++) {
                String caracter = alfabeto[i];
                Set<Integer> conjuntoDestino = new HashSet<>();
    
                for (Integer estado : estadoActual) {
                    if (i < transicionesEstadosAFN.get(estado).size()) {
                        List<Integer> transiciones = transicionesEstadosAFN.get(estado).get(i);
                        for (Integer siguiente : transiciones) {
                            conjuntoDestino.add(siguiente);
                        }
                    }
                }
    
                clausura_lambda(conjuntoDestino);
    
                // Redirige a estado de error si no hay transición
                if (conjuntoDestino.isEmpty()) {
                    TransicionAFD transicionError = new TransicionAFD(nombreEstadoActual, caracter, 0, false);
                    transicionesEstadoAFD[i].add(transicionError);
                    continue;
                }
    
                Integer nombreEstadoDestino = obtenerEstado(conjuntoDestino);
    
                if (nombreEstadoDestino != null) {
                    boolean esEstadoFinal = false;
                    for (Integer estado : conjuntoDestino) {
                        if (Arrays.asList(estadosFinalAFN).contains(estado)) {
                            esEstadoFinal = true;
                            break;
                        }
                    }
    
                    TransicionAFD transicion = new TransicionAFD(nombreEstadoActual, caracter, nombreEstadoDestino, esEstadoFinal);
                    transicionesEstadoAFD[i].add(transicion);
    
                    if (esEstadoFinal && !estadosFinalAFD.contains(nombreEstadoDestino)) {
                        estadosFinalAFD.add(nombreEstadoDestino);
                    }
                }
            }
        }
    
        Collections.sort(estadosFinalAFD); // Ordena antes de imprimir
        escribirAFD(afdPath);
    }
    

    private void lecturaAFN(){
        try(BufferedReader reader = new BufferedReader(new FileReader(direccionAFN))){
            alfabeto = reader.readLine().split(",");
            cantidadEstadosAFN = Integer.parseInt(reader.readLine());
            String[] estadosFinal = reader.readLine().split(",");
            estadosFinalAFN = new int[estadosFinal.length];
            for (int i = 0; i < estadosFinal.length; i++) {
                estadosFinalAFN[i] = Integer.parseInt(estadosFinal[i].trim());
            }

            String[] lambdas = reader.readLine().split(",");
            transicionesLambdaAFN = new ArrayList<>();
            for (String cell : lambdas) {
                List<Integer> transicionLambda = new ArrayList<>();
                for (String p : cell.split(";")) {
                    if (!p.isEmpty()) {
                        transicionLambda.add(Integer.parseInt(p.trim()));
                    }
                }
                transicionesLambdaAFN.add(transicionLambda);
            }

            String line;
            transicionesEstadosAFN = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                String[] cells = line.split(",");
                List<List<Integer>> row = new ArrayList<>();
                for (String cell : cells) {
                    List<Integer> vals = new ArrayList<>();
                    for (String p : cell.split(";")) {
                        if (!p.isEmpty()) {
                            vals.add(Integer.parseInt(p.trim()));
                        }
                    }
                    row.add(vals);
                }
                transicionesEstadosAFN.add(row);
            }
        } catch(IOException e){
            System.err.println("Error leyendo AFN: " + direccionAFN);
        }
    }


    public void clausura_lambda(Set<Integer> conjuntoActual){
        Set<Integer> conjuntoNuevo = new LinkedHashSet<>(conjuntoActual);
        Queue<Integer> cola = new LinkedList<>(conjuntoActual);
        while (!cola.isEmpty()) {
            int estado = cola.poll();
            List<Integer> transiciones = transicionesLambdaAFN.get(estado);
            if (transiciones != null) {
                for (Integer siguiente : transiciones) {
                    if (conjuntoNuevo.add(siguiente)) {
                        cola.add(siguiente);
                    }
                }
            }
        }
        Set<Integer> estadoNuevo = new HashSet<>(conjuntoNuevo);
        boolean yaExiste = mapaClausuras.values().stream()
        .anyMatch(lista -> new HashSet<>(lista).equals(estadoNuevo));

        if (!yaExiste) {
            mapaClausuras.put(contadorEstadosClausuras, estadoNuevo);
            contadorEstadosClausuras++;
            conjuntoCreadosPendientes.add(estadoNuevo);
        }
        
    }

    public Integer obtenerEstado(Set<Integer> conjuntoABuscar) {
        for (Map.Entry<Integer, Set<Integer>> entry : mapaClausuras.entrySet()) {
            if (entry.getValue().equals(conjuntoABuscar)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    private int getIndiceSimbolo(char simbolo) {
        for (int i = 0; i < alfabeto.length; i++) {
            if (alfabeto[i].equals(String.valueOf(simbolo))) {
                return i;
            }
        }
        return -1;
    }
    
    
    private void escribirAFD(String afdPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(afdPath))) {
            // Línea 1: alfabeto
            writer.write(String.join(",", alfabeto));
            writer.newLine();
    
            // Línea 2: cantidad de estados del AFD (incluye el estado 0 como estado de error)
            writer.write(String.valueOf(mapaClausuras.size()));
            writer.newLine();
    
            // Línea 3: estados finales ordenados
            List<Integer> finalesOrdenados = new ArrayList<>(estadosFinalAFD);
            Collections.sort(finalesOrdenados);
            for (int i = 0; i < finalesOrdenados.size(); i++) {
                writer.write(finalesOrdenados.get(i).toString());
                if (i < finalesOrdenados.size() - 1) {
                    writer.write(",");
                }
            }
            writer.newLine();
    
            // Transiciones (una fila por estado desde 1 hasta el número total)
            for (int estado = 1; estado <= mapaClausuras.size(); estado++) {
                List<String> fila = new ArrayList<>();
                for (int i = 0; i < alfabeto.length; i++) {
                    boolean agregado = false;
                    for (TransicionAFD t : transicionesEstadoAFD[i]) {
                        if (t.estadoOrigen == estado) {
                            fila.add(String.valueOf(t.estadoDestino));
                            agregado = true;
                            break;
                        }
                    }
                    if (!agregado) {
                        fila.add("0"); // Estado de error
                    }
                }
                writer.write(String.join(",", fila));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo AFD: " + afdPath);
        }
    }
    
    


    public static void main(String[] args) {
        String rutaAFN = "pruebas/afn/prueba1.afn";        // Ruta de entrada del archivo AFN
        String rutaAFD = "pruebas/afd/prueba1.afd"; // Ruta donde se guardará el AFD
    
        AFN automata = new AFN(rutaAFN);
        automata.toAFD(rutaAFD);
    
        System.out.println("Conversión completada. AFD guardado en: " + rutaAFD);
    }
    

    //***************************************************
    private class TransicionAFD {
        int estadoOrigen;
        String caracter;
        int estadoDestino;
        boolean finalOno;

        public TransicionAFD(int origen, String caracter, int destino, boolean finalOno) {
            this.estadoOrigen = origen;
            this.caracter = caracter;
            this.estadoDestino = destino;
            this.finalOno = finalOno;
        }
        public int getEstadoOrigen(){
            return this.estadoOrigen;
        }
        public int getEstadoDestino(){
            return this.estadoDestino;
        }
        public String getCaracter(){
            return this.caracter;
        }
        public boolean getFinaloNo(){
            return this.finalOno;
        }

    }
}
/*NOTAS:
 * Falta imprimir AFD
 * Algoritmo de conversion
 * main
 * accept cuerda
 */
