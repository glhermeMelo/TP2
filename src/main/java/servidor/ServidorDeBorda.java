package servidor;

import com.google.gson.Gson;
import entities.RegistroClimatico;
import servidor.threads.EnviaRegistrosAoDatacenter;
import servidor.threads.ServidorDeBordaAceitaMicrodispositivo;
import servidor.util.AnalisadorDeRegistrosClimaticos;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorDeBorda extends ImplServidor {
    private ConcurrentHashMap<Integer, List<String>> mapaDeRegistrosClimaticos;
    private final String ipDatacenter;
    private final int portaDatacenter;

    public ServidorDeBorda(int porta, String ip, String nome, String ipDatacenter, int portaDatacenter) {
        super(porta, ip, nome);
        mapaDeRegistrosClimaticos = new ConcurrentHashMap<>();
        this.ipDatacenter = ipDatacenter;
        this.portaDatacenter = portaDatacenter;
        rodar();
    }

    @Override
    protected void rodar() {
        EnviaRegistrosAoDatacenter enviarAoDatacenter =
                new EnviaRegistrosAoDatacenter(mapaDeRegistrosClimaticos, ipDatacenter,
                        portaDatacenter, nome, 5000);

        Thread enviar = new Thread(enviarAoDatacenter);
        enviar.start();

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
                } catch (IOException ignored) {
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

    private void analisarPacote(DatagramSocket socket, DatagramPacket pacoteUDP) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(pacoteUDP.getData(), 0, pacoteUDP.getLength()))) {

            Object entrada1 = ois.readObject();

            if (entrada1 instanceof String) {

                // 2. Lê o segundo objeto para distinguir Handshake de Envio de Dados
                Object entrada2 = null;
                try {
                    entrada2 = ois.readObject();
                } catch (Exception e) {
                    // Ignora se não houver segundo objeto ou erro de leitura (pacote inválido)
                }

                // === CASO 1: Handshake (ID + PublicKey) ===
                if (entrada2 instanceof PublicKey) {
                    // Cria uma cópia dos dados para a thread de handshake processar do início
                    byte[] dados = new byte[pacoteUDP.getLength()];
                    System.arraycopy(pacoteUDP.getData(), 0, dados, 0, pacoteUDP.getLength());
                    DatagramPacket copia = new DatagramPacket(dados, dados.length, pacoteUDP.getAddress(), pacoteUDP.getPort());

                    ServidorDeBordaAceitaMicrodispositivo trocaChaves =
                            new ServidorDeBordaAceitaMicrodispositivo(socket, copia, chavesClientes);

                    new Thread(trocaChaves).start();
                }
                // === CASO 2: Dados Cifrados (ID + byte[] chaveSessao + ...) ===
                else if (entrada2 instanceof byte[]) {
                    // processarPacoteUDP (na superclasse) já lê a sequência: ID -> ChaveEnc -> Nonce -> PayloadEnc
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
                mapaDeRegistrosClimaticos.computeIfAbsent(id, k -> new ArrayList<>()).add(registroClimatico);
            } catch (NumberFormatException e) {
                System.err.println("Erro ao converter ID do dispositivo para inteiro: " + registro.idDispositivo());
            }
        } catch (Exception e) {
            System.err.println("Erro ao salvar registro climatico: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ServidorDeBorda servidorDeBorda =
                new ServidorDeBorda(
                        7000,
                        "localhost",
                        "Borda-1",
                        "localhost",
                        8000);

    }
}
