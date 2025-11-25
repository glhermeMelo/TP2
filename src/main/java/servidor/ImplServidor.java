package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ImplServidor {
    protected int porta;
    protected String ip;
    protected String nome;
    protected boolean isActive = true;

    protected ConcurrentHashMap<String, KeyPair> chavesClientes;
    protected ConcurrentHashMap<String, Integer> localizacaoServidoresDeBorda;

    private ServerSocket serverSocket;

    public ImplServidor(int porta, String ip, String nome, ConcurrentHashMap<String, Integer> localizacaoServidoresDeBorda) {
        this.porta = porta;
        this.ip = ip;
        this.nome = nome;
        this.chavesClientes = new ConcurrentHashMap<>();
        this.localizacaoServidoresDeBorda = localizacaoServidoresDeBorda;
        rodar();
    }

    private void rodar() {
        try {
            serverSocket = new ServerSocket(porta);
            System.out.println(nome + " escutando em " + ip + ":" + porta);

            while (isActive) {
                try {
                    Socket cliente = serverSocket.accept();
                    AceitaCliente aceitaCliente = new AceitaCliente(cliente, chavesClientes, localizacaoServidoresDeBorda);
                    Thread thread = new Thread(aceitaCliente);
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
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException ignored) {}
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

    public ConcurrentHashMap<String, KeyPair> getChavesClientes() {
        return chavesClientes;
    }

    public void setChavesClientes(ConcurrentHashMap<String, KeyPair> chavesClientes) {
        this.chavesClientes = chavesClientes;
    }

    public ConcurrentHashMap<String, Integer> getLocalizacaoServidoresDeBorda() {
        return localizacaoServidoresDeBorda;
    }

    public void setLocalizacaoServidoresDeBorda(ConcurrentHashMap<String, Integer> localizacaoServidoresDeBorda) {
        this.localizacaoServidoresDeBorda = localizacaoServidoresDeBorda;
    }
}
