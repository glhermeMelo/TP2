import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class IDS {
    private int porta;
    private String ip;
    private List<Integer> portasBorda;
    private boolean isActive = true;

    public IDS(int porta, String ip, List<Integer> portasBorda) {
        this.porta = porta;
        this.ip = ip;
        this.portasBorda = portasBorda;
        rodar();
    }

    public void rodar() {
        System.out.println("IDS iniciado. Monitorizando alertas na porta " + porta);

        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            while (isActive) {
                Socket socket = serverSocket.accept();
                new Thread(() -> processarAlerta(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no IDS: " + e.getMessage());
        }
    }

    private void processarAlerta(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String alerta = reader.readLine();

            if (alerta != null) {
                registrarIntrusao(alerta);

                if (alerta.contains("CRITICO")) {
                    String idDispositivo = extrairId(alerta);
                    if (idDispositivo != null) {
                        enviarComandoBloqueio(idDispositivo);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao processar alerta: " + e.getMessage());
        }
    }

    private void registrarIntrusao(String log) {
        System.out.println("\nANOMALIA: " + LocalDateTime.now());
        System.out.println("      Detalhes: " + log);
    }

    private void enviarComandoBloqueio(String idDispositivo) {
        System.out.println("Iniciando protocolo de contenção para: " + idDispositivo);

        for (Integer porta : portasBorda) {
            try (Socket socketBorda = new Socket(ip, porta);
                 PrintWriter writer = new PrintWriter(socketBorda.getOutputStream(), true)) {

                writer.println("BLOQUEAR:" + idDispositivo);
                System.out.println("Comando de bloqueio enviado ao Servidor de Borda com sucesso.");

            } catch (IOException e) {
                System.err.println("FALHA ao contactar Servidor de Borda: " + e.getMessage());
            }
        }
    }

    private String extrairId(String alerta) {
        String[] partes = alerta.split("\\|");
        if (partes.length >= 3) {
            return partes[2];
        }
        return null;
    }

    public static void main(String[] args) {
        List<Integer> portasAdmin = new ArrayList<>();
        portasAdmin.add(6601);
        portasAdmin.add(6602);
        portasAdmin.add(6603);

        IDS ids = new IDS(6500, "localhost", portasAdmin);
    }
}
