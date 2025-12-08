package seguranca;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;

public class IDS {
    private int porta;
    private String ip;
    private int borda;
    private boolean isActive = true;

    public IDS(int porta, String ip, int borda) {
        this.porta = porta;
        this.ip = ip;
        this.borda = borda;
        rodar();
    }

    public void rodar() {
        System.out.println("seguranca.IDS iniciado. Monitorizando alertas na porta " + porta);

        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            while (isActive) {
                Socket socket = serverSocket.accept();
                new Thread(() -> processarAlerta(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no seguranca.IDS: " + e.getMessage());
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
        System.out.println("\n[seguranca.IDS] DETECÇÃO DE INTRUSÃO/ANOMALIA: " + LocalDateTime.now());
        System.out.println("      Detalhes: " + log);
    }

    private void enviarComandoBloqueio(String idDispositivo) {
        System.out.println("[seguranca.IDS] Iniciando protocolo de contenção para: " + idDispositivo);
        try (Socket socketBorda = new Socket(ip, borda);
             PrintWriter writer = new PrintWriter(socketBorda.getOutputStream(), true)) {

            writer.println("BLOQUEAR:" + idDispositivo);
            System.out.println("[seguranca.IDS] Comando de bloqueio enviado ao Servidor de Borda com sucesso.");

        } catch (IOException e) {
            System.err.println("[seguranca.IDS] FALHA ao contactar Servidor de Borda: " + e.getMessage());
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
        IDS ids = new IDS(6500, "localhost", 7002);
    }
}
