package firewall;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

public class FirewallBorda implements Runnable {
    private int portaEscuta;
    private int portaSaidaBorda;
    private String ipDestino;
    private List<String> whitelist;
    private boolean isActive = true;

    public FirewallBorda(int portaEscuta, int portaSaidaBorda, String ipDestino, List<String> whitelist) {
        this.portaEscuta = portaEscuta;
        this.portaSaidaBorda = portaSaidaBorda;
        this.ipDestino = ipDestino;
        this.whitelist = whitelist;
    }

    @Override
    public void run() {
        System.out.println("Firewall Borda iniciado na porta " + portaEscuta + " -> Encaminhando para " + portaSaidaBorda);
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] buffer = new byte[65535];

            while (isActive) {
                DatagramPacket pacoteRecebido = new DatagramPacket(buffer, buffer.length);
                socket.receive(pacoteRecebido);

                byte[] dados = new byte[pacoteRecebido.getLength()];
                System.arraycopy(pacoteRecebido.getData(), 0, dados, 0, pacoteRecebido.getLength());

                String supostoIp = extrairIp(pacoteRecebido);

                if (supostoIp != null && verificarIp(supostoIp)) {
                    System.out.println("Pacote válido de: " + supostoIp);
                    encaminharMensagem(socket, pacoteRecebido);
                } else {
                    System.err.println("Pacote rejeitado de: " + supostoIp);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao inicializar firewall: " + e.getMessage());
        }
    }

    private String extrairIp(DatagramPacket pacote) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(pacote.getData(), 0, pacote.getLength());
             ObjectInputStream ois = new ObjectInputStream(bais)) {

            Object objeto = ois.readObject();

            if (objeto instanceof String) {
                return (String) objeto;
            }

            return null;

        } catch (Exception e) {
            System.err.println("Erro ao obter ip do dispositivo: " + e.getMessage());
            return null;

        }
    }

    //firewall funciona no modo bloquear tudo que nao for permitido
    private boolean verificarIp(String ip) {
        for (String s : whitelist) {
            if (ip.startsWith(s)) {
                return true;
            }
        }

        return false;
    }

    private void encaminharMensagem(DatagramSocket socket, DatagramPacket pacote) {
        try {
            InetAddress enderecoDestino = InetAddress.getByName(ipDestino);

            DatagramPacket pacoteResposta = new DatagramPacket(pacote.getData(), pacote.getLength(), enderecoDestino, portaSaidaBorda);

            socket.send(pacoteResposta);
        } catch (IOException e) {
            System.err.println("Erro ao encaminhar pacote: " + e.getMessage());
        }
    }

    private void analisarDados() {

    }
}
