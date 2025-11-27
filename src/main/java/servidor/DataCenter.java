package servidor;

import entities.RegistroClimatico;
import servidor.threads.DataCenterAceitaServidoresDeBorda;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DataCenter extends ImplServidor {
    private ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGlobais;
    private List<Thread> listaConexoes;

    public DataCenter(int porta, String ip, String nome) {
        super(porta, ip, nome);
        dadosGlobais = new ConcurrentHashMap<>();
        listaConexoes = new ArrayList<>();
        rodar();
    }

    @Override
    public void rodar() {
        System.out.println("DataCenter iniciado na porta " + this.porta);
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            while (isActive) {
                Socket cliente = serverSocket.accept();
                Thread thread = new Thread(new DataCenterAceitaServidoresDeBorda(cliente, chavesClientes, dadosGlobais));
                thread.start();
                listaConexoes.add(thread);
            }
        } catch (IOException e) {
            if (isActive) {
                System.err.println("Erro ao aceitar conexão: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        DataCenter dataCenter = new DataCenter(8000, "localhost", "DT1");
    }
}
