package servidor.borda;

import com.google.gson.Gson;
import entities.RegistroClimatico;
import servidor.ImplServidor;
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
    private int portaServidorLocalizacao;
    private long tempoFalha;
    private int portaPublica;

    public ServidorDeBorda(int porta, String ip, String nome, String ipDatacenter,int portaServidorLocalizacao, int portaDatacenter, int portaIDS,
                           long tempoFalha, int portaPublica) {
        super(porta, ip, nome);
        mapaDeRegistrosClimaticos = new ConcurrentHashMap<>();
        this.ipDatacenter = ipDatacenter;
        this.portaDatacenter = portaDatacenter;
        blacklist = new CopyOnWriteArrayList<>();
        this.portaIDS = portaIDS;
        this.portaServidorLocalizacao = portaServidorLocalizacao;
        this.tempoFalha = tempoFalha;
        this.portaPublica = portaPublica;
        rodar();
    }

    @Override
    protected void rodar() {
        reportarAtivo();

        EnviaRegistros enviarAoDatacenter =
                new EnviaRegistros(mapaDeRegistrosClimaticos, ipDatacenter,
                        portaDatacenter, nome, 5000);

        new Thread(enviarAoDatacenter).start();

        new Thread(this::ouvidIDS).start();

        new Thread(this::ouvirUDP).start();

        new Thread(() -> {
            System.err.println("Simulador de Falhas iniciado!");
            if(isActive) abrirServerSocket();

            while (true) {
                try {
                    Thread.sleep(tempoFalha);
                    isActive = !isActive;

                    if (isActive) {
                        System.err.println("\n[" + nome + "] LIGANDO SERVIDOR DE BORDA");
                        abrirServerSocket();
                        reportarAtivo();
                    } else {
                        System.err.println("\n[" + nome + "] FECHANDO SERVIDOR DE BORDA");
                        fecharServerSocket();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    private void abrirServerSocket() {
        try {
            if (serverSocket == null || serverSocket.isClosed()) {
                serverSocket = new ServerSocket(porta);
                System.out.println(nome + " escutando TCP (Health Check) em " + ip + ":" + porta);
            }
        } catch (IOException e) {
            System.err.println("Erro ao abrir ServerSocket: " + e.getMessage());
        }
    }

    private void fecharServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar ServerSocket: " + e.getMessage());
        }
    }

    public void parar() {
        this.isActive = false;
        fecharServerSocket();
    }

    private void ouvirUDP() {
        System.out.println(nome + " ouvindo dados (UDP) na porta " + porta);
        try (DatagramSocket socketUDP = new DatagramSocket(porta)) {
            byte[] buffer = new byte[65535];
            while (true) {
                if (!isActive) {
                    try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                    continue;
                }
                DatagramPacket pacoteUDP = new DatagramPacket(buffer, buffer.length);
                socketUDP.receive(pacoteUDP);
                analisarPacote(socketUDP, pacoteUDP);
            }
        } catch (IOException e) {
            System.err.println("Erro ao inicializar UDP na porta " + porta + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro no loop UDP: " + e.getMessage());
        }
    }

    private void ouvidIDS() {
        System.out.println(nome + " ouvindo comandos do seguranca.IDS na porta " + portaIDS);
        try (ServerSocket serverSocketAdmin = new ServerSocket(portaIDS)) {
            while (true) {
                if (!isActive) {
                    try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                    continue;
                }

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

    private void reportarAtivo() {
        try(Socket socket = new Socket(ip, portaServidorLocalizacao)) {
            try (ObjectOutputStream dataOutputStream = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream dataInputStream = new ObjectInputStream(socket.getInputStream())) {

                String mensagem = "BORDA|" + nome + "|" + ip + ":" + portaPublica;
                dataOutputStream.writeObject(mensagem);
            }catch (IOException e) {
                System.err.println("Erro ao enviar mensagem para o servidor de localizacao: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Erro ao contactar servidor de localizacao para declarar eligibilidade: " + e.getMessage());
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
                Object entrada2 = null;
                try { entrada2 = ois.readObject(); } catch (Exception e) {}

                if (entrada2 instanceof PublicKey) {
                    byte[] dados = new byte[pacoteUDP.getLength()];
                    System.arraycopy(pacoteUDP.getData(), 0, dados, 0, pacoteUDP.getLength());
                    DatagramPacket copia = new DatagramPacket(dados, dados.length, pacoteUDP.getAddress(), pacoteUDP.getPort());
                    ServidorDeBordaAceitaMicrodispositivo trocaChaves = new ServidorDeBordaAceitaMicrodispositivo(socket, copia, chavesClientes);
                    new Thread(trocaChaves).start();
                } else if (entrada2 instanceof byte[]) {
                    String registroClimatico = processarPacoteUDP(pacoteUDP);
                    if (registroClimatico != null) {
                        salvarRegistroClimatico(registroClimatico);
                    } else {
                        System.err.println("Pacote UDP ignorado ou falha na decifragem.");
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao ler pacote UDP: " + e.getMessage());
        }
    }

    private synchronized void salvarRegistroClimatico(String registroClimatico) {
        try {
            Gson gson = new Gson();
            RegistroClimatico registro = gson.fromJson(registroClimatico, RegistroClimatico.class);
            if (registro != null) {
                AnalisadorDeRegistrosClimaticos.analisarRegistroClimatico(registro);
            }
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

    // Getters e Setters
    public int getPorta() { return porta; }
    public void setPorta(int porta) { this.porta = porta; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public ServerSocket getServerSocket() { return serverSocket; }
    public void setServerSocket(ServerSocket serverSocket) { this.serverSocket = serverSocket; }
    public ConcurrentHashMap<Integer, List<String>> getMapaDeRegistrosClimaticos() { return mapaDeRegistrosClimaticos; }
    public void setMapaDeRegistrosClimaticos(ConcurrentHashMap<Integer, List<String>> mapaDeRegistrosClimaticos) { this.mapaDeRegistrosClimaticos = mapaDeRegistrosClimaticos; }
    public ConcurrentHashMap<String, KeyPair> getChavesClientes() { return chavesClientes; }
    public void setChavesClientes(ConcurrentHashMap<String, KeyPair> chavesClientes) { this.chavesClientes = chavesClientes; }
}