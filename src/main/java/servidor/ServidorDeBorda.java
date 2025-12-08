package servidor;

import com.google.gson.Gson;
import entities.RegistroClimatico;
import servidor.threads.EnviaRegistros;
import servidor.threads.ServidorDeBordaAceitaMicrodispositivo;
import servidor.util.AnalisadorDeRegistrosClimaticos;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServidorDeBorda extends ImplServidor {
    private ConcurrentHashMap<Integer, List<String>> mapaDeRegistrosClimaticos;
    private final String ipDatacenter;
    private final int portaDatacenter;
    private List<String> blacklist;
    private int portaIDS;

    public ServidorDeBorda(int porta, String ip, String nome, String ipDatacenter, int portaDatacenter, int portaIDS) {
        super(porta, ip, nome);
        mapaDeRegistrosClimaticos = new ConcurrentHashMap<>();
        this.ipDatacenter = ipDatacenter;
        this.portaDatacenter = portaDatacenter;
        blacklist = new CopyOnWriteArrayList<>();
        this.portaIDS = portaIDS;
        rodar();
    }

    @Override
    protected void rodar() {
        EnviaRegistros enviarAoDatacenter =
                new EnviaRegistros(mapaDeRegistrosClimaticos, ipDatacenter,
                        portaDatacenter, nome, 5000);

        new Thread(enviarAoDatacenter).start();

        new Thread(this::ouvidIDS).start();

        new Thread(this::ouvirUDP).start();

        try {
            serverSocket = new ServerSocket(porta);
            System.out.println(nome + " escutando em " + ip + ":" + porta);
        } catch (IOException e) {
            System.err.println("Erro ao inicializar servidor de localização na porta " + porta + ": " + e.getMessage());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(nome + " finalizado.");
        }
    }

    public void parar() {
        this.isActive = false;
    }

    private void ouvirUDP() {
        System.out.println(nome + " ouvindo dados (UDP) na porta " + porta);

        try (DatagramSocket socketUDP = new DatagramSocket(porta)) {
            byte[] buffer = new byte[65535];

            while (isActive) {
                DatagramPacket pacoteUDP = new DatagramPacket(buffer, buffer.length);
                socketUDP.receive(pacoteUDP);

                analisarPacote(socketUDP, pacoteUDP);
            }
        } catch (IOException e) {
            System.err.println("Erro ao inicializar servidor de localização na porta " + porta + ": " + e.getMessage());
        } finally {
            System.out.println(nome + " finalizado.");
        }
    }

    private void ouvidIDS() {
        System.out.println(nome + " ouvindo comandos do seguranca.IDS na porta " + portaIDS);
        try (ServerSocket serverSocketAdmin = new ServerSocket(portaIDS)) {
            while (isActive) {
                Socket socket = serverSocketAdmin.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String comando = reader.readLine();

                if (comando != null && comando.startsWith("BLOQUEAR:")) {
                    String idParaBloquear = comando.split(":")[1];
                    blacklist.add(idParaBloquear);
                    System.err.println("!!! [BORDA] COMANDO RECEBIDO: Dispositivo " + idParaBloquear + " adicionado à BLACKLIST !!!");
                }
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro na thread de gerência do seguranca.IDS: " + e.getMessage());
        }
    }

    private void analisarPacote(DatagramSocket socket, DatagramPacket pacoteUDP) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(pacoteUDP.getData(), 0, pacoteUDP.getLength()))) {

            Object entrada1 = ois.readObject();

            if (entrada1 instanceof String) {
                String idDispositivo = (String) entrada1;

                if (blacklist.contains(idDispositivo)) {
                    System.out.println("[BLOCKED] Pacote ignorado de dispositivo banido: " + idDispositivo);
                    return;
                }

                // 2. Lê o segundo objeto para distinguir Handshake de Envio de Dados
                Object entrada2 = null;
                try {
                    entrada2 = ois.readObject();
                } catch (Exception e) {

                }

                // === CASO 1: Handshake ===
                if (entrada2 instanceof PublicKey) {
                    byte[] dados = new byte[pacoteUDP.getLength()];
                    System.arraycopy(pacoteUDP.getData(), 0, dados, 0, pacoteUDP.getLength());
                    DatagramPacket copia = new DatagramPacket(dados, dados.length, pacoteUDP.getAddress(), pacoteUDP.getPort());

                    ServidorDeBordaAceitaMicrodispositivo trocaChaves =
                            new ServidorDeBordaAceitaMicrodispositivo(socket, copia, chavesClientes);

                    new Thread(trocaChaves).start();
                    // === CASO 2: Mensagem cifrada ===
                } else if (entrada2 instanceof byte[]) {
                    String registroClimatico = processarPacoteUDP(pacoteUDP);

                    if (registroClimatico != null) {
                        salvarRegistroClimatico(registroClimatico);
                    } else {
                        System.err.println("Pacote UDP ignorado ou falha na decifragem.");
                    }
                } else {
                    System.err.println("Objeto inesperado após ID: " + (entrada2 == null ? "null" : entrada2.getClass().getSimpleName()));
                }

            } else {
                System.err.println("Pacote UDP inválido: Primeiro objeto não é String (ID).");
            }

        } catch (IOException e) {
            System.err.println("Erro ao ler pacote UDP: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Erro de classe não encontrada no pacote UDP: " + e.getMessage());
        }
    }

    private synchronized void salvarRegistroClimatico(String registroClimatico) {
        try {
            Gson gson = new Gson();

            //Instancia novo registro
            RegistroClimatico registro = gson.fromJson(registroClimatico, RegistroClimatico.class);

            if (registro != null) {
                AnalisadorDeRegistrosClimaticos.analisarRegistroClimatico(registro);
            } else {
                System.err.println("Erro ao instanciar o registro climatico");
                return;
            }

            //Pega id e checa se existe no mapa ou nao
            int idDispositivo = Integer.parseInt(registro.idDispositivo().replaceAll("[^0-9]", ""));
            try {
                int id = Integer.parseInt(registro.idDispositivo().replaceAll("\\D+", ""));
                mapaDeRegistrosClimaticos.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(registroClimatico);
            } catch (NumberFormatException e) {
                System.err.println("Erro ao converter ID do dispositivo para inteiro: " + registro.idDispositivo());
            }
        } catch (Exception e) {
            System.err.println("Erro ao salvar registro climatico: " + e.getMessage());
        }
    }

    public int getPorta() {
        return porta;
    }

    public void setPorta(int porta) {
        this.porta = porta;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public ConcurrentHashMap<Integer, List<String>> getMapaDeRegistrosClimaticos() {
        return mapaDeRegistrosClimaticos;
    }

    public void setMapaDeRegistrosClimaticos(ConcurrentHashMap<Integer, List<String>> mapaDeRegistrosClimaticos) {
        this.mapaDeRegistrosClimaticos = mapaDeRegistrosClimaticos;
    }

    public ConcurrentHashMap<String, KeyPair> getChavesClientes() {
        return chavesClientes;
    }

    public void setChavesClientes(ConcurrentHashMap<String, KeyPair> chavesClientes) {
        this.chavesClientes = chavesClientes;
    }

    public static void main(String[] args) {
        ServidorDeBorda servidorDeBorda =
                new ServidorDeBorda(
                        7001,
                        "192.168.0.8",
                        "Borda-1",
                        "192.168.0.8",
                        8000,
                        6500);

    }
}
