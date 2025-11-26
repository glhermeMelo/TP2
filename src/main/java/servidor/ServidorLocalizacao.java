package servidor;

import servidor.threads.ServidorDeLocalizacaoAceitaMicrodispositivos;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorLocalizacao extends ImplServidor {
    protected ConcurrentHashMap<String, Integer> localizacaoServidoresDeBorda;

    public ServidorLocalizacao(int porta, String ip, String nome, ConcurrentHashMap<String, Integer> localizacaoServidoresDeBorda) {
        super(porta, ip, nome);
        this.localizacaoServidoresDeBorda = localizacaoServidoresDeBorda;
        rodar();
    }

    @Override
    protected void rodar() {
        try {
            serverSocket = new ServerSocket(porta);
            System.out.println(nome + " escutando em " + ip + ":" + porta);

            while (isActive) {
                try {
                    Socket cliente = serverSocket.accept();
                    ServidorDeLocalizacaoAceitaMicrodispositivos aceitaCliente = new ServidorDeLocalizacaoAceitaMicrodispositivos(cliente, chavesClientes, localizacaoServidoresDeBorda);
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

    public static void main(String[] args) {
        ConcurrentHashMap<String, Integer> localizacaoServidoresDeBorda = new ConcurrentHashMap<>();
        localizacaoServidoresDeBorda.put("Alto", 7000);
        localizacaoServidoresDeBorda.put("Centro", 7000);
        localizacaoServidoresDeBorda.put("Nova Betania", 7000);
        localizacaoServidoresDeBorda.put("Vingt Rosado", 7000);

        ServidorLocalizacao servidorLocalizacao = new ServidorLocalizacao(
                6000, "localhost",  "ServidorLocalizacao", localizacaoServidoresDeBorda);
    }
}
