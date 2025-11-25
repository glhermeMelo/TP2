package servidor;

import java.net.ServerSocket;
import java.security.KeyPair;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ImplServidor {
    protected int porta;
    protected String ip;
    protected String nome;
    protected boolean isActive = true;
    protected ServerSocket serverSocket;
    protected ConcurrentHashMap<String, KeyPair> chavesClientes;


    protected ImplServidor(int porta, String ip, String nome) {
        this.porta = porta;
        this.ip = ip;
        this.nome = nome;
        this.chavesClientes = new ConcurrentHashMap<>();
    }

    protected abstract void rodar();

    protected void parar() {
        this.isActive = false;
    }

    protected int getPorta() {
        return porta;
    }

    protected void setPorta(int porta) {
        this.porta = porta;
    }

    protected String getIp() {
        return ip;
    }

    protected void setIp(String ip) {
        this.ip = ip;
    }

    protected String getNome() {
        return nome;
    }

    protected void setNome(String nome) {
        this.nome = nome;
    }

    protected boolean isActive() {
        return isActive;
    }

    protected void setActive(boolean active) {
        isActive = active;
    }

    protected ServerSocket getServerSocket() {
        return serverSocket;
    }

    protected void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    protected ConcurrentHashMap<String, KeyPair> getChavesClientes() {
        return chavesClientes;
    }

    protected void setChavesClientes(ConcurrentHashMap<String, KeyPair> chavesClientes) {
        this.chavesClientes = chavesClientes;
    }
}
