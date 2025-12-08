package servidor;

import com.google.gson.Gson;
import servidor.threads.ProxyAceitaServidoresDeBorda;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.util.concurrent.ConcurrentHashMap;

public class Proxy extends ImplServidor {
    private ConcurrentHashMap<String, KeyPair> chavesClientes;
    private ConcurrentHashMap<String, KeyPair> chavesDatacenter;

    private final int portaDatacenter;
    private final String ipDatacenter;
    private final Gson gson;

    public Proxy(int portEscuta, String ipProxy, String nome, int portaDatacenter, String ipDatacenter) {
        super(portEscuta, ipProxy, nome);
        this.portaDatacenter = portaDatacenter;
        this.ipDatacenter = ipDatacenter;
        this.chavesClientes = new ConcurrentHashMap<>();
        this.chavesDatacenter = new ConcurrentHashMap<>();
        this.gson = new Gson();
        rodar();
    }

    @Override
    protected void rodar() {
        System.out.println("Proxy iniciado na porta " + this.porta);

        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            while (isActive) {
                Socket cliente = serverSocket.accept();
                Thread aceitadora = new Thread(new ProxyAceitaServidoresDeBorda(
                        cliente,
                        ipDatacenter,
                        portaDatacenter,
                        chavesClientes,
                        chavesDatacenter));

                aceitadora.start();
            }
        } catch (IOException e) {
            if (isActive) {
                System.err.println("Erro ao aceitar conexão no Proxy: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        Proxy proxy = new Proxy(
                8000,
                "localhost",
                "Proxy",
                9000,
                "localhost");
    }
}
