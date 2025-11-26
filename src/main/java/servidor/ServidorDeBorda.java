package servidor;

import com.google.gson.Gson;
import entities.RegistroClimatico;
import servidor.threads.EnviaRegistrosAoDatacenter;
import servidor.threads.ServidorDeBordaAceitaMicrodispositivo;
import servidor.util.AnalisadorDeRegistrosClimaticos;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorDeBorda extends ImplServidor {
    private ConcurrentHashMap<Integer, List<String>> mapaDeRegistrosClimaticos;
    private String ipDatacenter;
    private int portaDatacenter;

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

            while (isActive) {
                try {
                    Socket cliente = serverSocket.accept();
                    ServidorDeBordaAceitaMicrodispositivo aceitaMicrodispositivo = new ServidorDeBordaAceitaMicrodispositivo(cliente, chavesClientes);
                    Thread thread = new Thread(aceitaMicrodispositivo);
                    thread.start();
                } catch (IOException acceptEx) {
                    if (isActive) {
                        System.err.println("Erro ao aceitar conexão: " + acceptEx.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao inicializar servidor de localização na porta " + porta + ": " + e.getMessage());
        } finally {
            // garantir fechamento do serverSocket ao sair
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
            int idDispositivo = Integer.parseInt(registro.idDispositivo());

            mapaDeRegistrosClimaticos.computeIfAbsent(idDispositivo, k -> new ArrayList<>()).add(registroClimatico);
        } catch (Exception e) {
            System.err.println("Erro ao salvar registro climatico: " + e.getMessage());
        }
    }

    private void ouvirUDP() {
        try (DatagramSocket socketUDP = new DatagramSocket(porta)) {
            byte[] buffer = new byte[65535];

            while (isActive) {
                DatagramPacket pacoteUDP = new DatagramPacket(buffer, buffer.length);
                socketUDP.receive(pacoteUDP);

                String registroClimatico = processarPacoteUDP(pacoteUDP);
                if (registroClimatico == null) {
                    System.err.println("Erro ao adicionar registro ao HashMap");
                    return;
                }

                salvarRegistroClimatico(registroClimatico);
            }
        } catch (IOException e) {
            System.err.println("Erro ao inicializar servidor de localização na porta " + porta + ": " + e.getMessage());
        } finally {
            System.out.println(nome + " finalizado.");
        }
    }

    public static void main(String[] args) {
        ServidorDeBorda servidorDeBorda =
                new ServidorDeBorda(7000, "localhost", "Borda1", "localhost", 8000);
    }
}
