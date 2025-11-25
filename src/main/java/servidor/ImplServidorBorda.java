package servidor;

import microdispositivo.util.GeradorDeLeituras;
import servidor.threads.AceitaMicrodispositivo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ImplServidorBorda {
    protected int porta;
    protected String ip;
    protected String nome;
    protected boolean isActive = true;
    private ServerSocket serverSocket;

    protected ConcurrentHashMap<Integer, List<GeradorDeLeituras>> mapaDeRegistrosClimaticos;
    protected ConcurrentHashMap<String, KeyPair> chavesClientes;

    public ImplServidorBorda(int porta, String ip, String nome) {
        this.porta = porta;
        this.ip = ip;
        this.nome = nome;
        this.chavesClientes = new ConcurrentHashMap<>();
        this.mapaDeRegistrosClimaticos = new ConcurrentHashMap<>();
        rodar();
    }
    
        private void rodar() {
            try {
                serverSocket = new ServerSocket(porta);
                System.out.println(nome + " escutando em " + ip + ":" + porta);

                while (isActive) {
                    try {
                        Socket cliente = serverSocket.accept();
                        AceitaMicrodispositivo aceitaMicrodispositivo = new AceitaMicrodispositivo(cliente, mapaDeRegistrosClimaticos, chavesClientes);
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

    public ConcurrentHashMap<Integer, List<GeradorDeLeituras>> getMapaDeRegistrosClimaticos() {
        return mapaDeRegistrosClimaticos;
    }

    public void setMapaDeRegistrosClimaticos(ConcurrentHashMap<Integer, List<GeradorDeLeituras>> mapaDeRegistrosClimaticos) {
        this.mapaDeRegistrosClimaticos = mapaDeRegistrosClimaticos;
    }

    public ConcurrentHashMap<String, KeyPair> getChavesClientes() {
        return chavesClientes;
    }

    public void setChavesClientes(ConcurrentHashMap<String, KeyPair> chavesClientes) {
        this.chavesClientes = chavesClientes;
    }
}
