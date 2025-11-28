package servidor.threads;

import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.security.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorDeBordaAceitaMicrodispositivo implements Runnable {
    private final DatagramSocket cliente;
    private final DatagramPacket pacote;
    private ObjectInputStream entrada;
    protected final ConcurrentHashMap<String, KeyPair> chavesClientes;

    public ServidorDeBordaAceitaMicrodispositivo(DatagramSocket cliente, DatagramPacket pacote,ConcurrentHashMap<String, KeyPair> chavesClientes) {
        this.cliente = cliente;
        this.pacote = pacote;
        this.chavesClientes = chavesClientes;
    }

    @Override
    public void run() {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(pacote.getData(), 0, pacote.getLength());
            entrada = new ObjectInputStream(bais);

            if (autenticaCliente()) {
                System.out.println("Handshake UDP concluído com: " + pacote.getAddress().getHostAddress() + ":" + pacote.getPort());
            } else {
                System.err.println("Falha no Handshake UDP com: " + pacote.getAddress().getHostAddress());
            }
        } catch (EOFException e) {
            System.out.println("Conexão encerrada pelo cliente.");
        } catch (IOException e) {
            System.err.println("Erro de I/O com cliente: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro desconhecido: " + e.getMessage());
        }
    }

    private synchronized boolean autenticaCliente() {
        try {
            // 1 - Ler IdDispositivo
            Object entrada1 = entrada.readObject();
            if (!(entrada1 instanceof String)) {
                System.err.println("Erro ao receber id do cliente: " + cliente.getInetAddress().getHostAddress() + ":" + cliente.getPort());
                return false;
            }
            String idDispositivo = (String) entrada1;

            // 2 - Ler Chave Pública do Cliente
            Object entrada2 = entrada.readObject();
            if (!(entrada2 instanceof PublicKey)) {
                System.err.println("Resposta inesperada do cliente na troca de chaves (esperado PublicKey).");
                return false;
            }

            System.out.println("Recebida solicitação de UDP de: " + idDispositivo);

            // 3 - Gera ou recupera par de chaves do servidor para este ID
            chavesClientes.computeIfAbsent(idDispositivo, k -> {
                try {
                    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                    kpg.initialize(2048, new SecureRandom());
                    return kpg.generateKeyPair();
                } catch (NoSuchAlgorithmException e) {
                    System.err.println("Erro na autenticacao de chaves RSA: " + e.getMessage());
                }
                return null;
            });

            // 4 - Prepara resposta (Chave Pública do Servidor)
            KeyPair chavesServidor = chavesClientes.get(idDispositivo);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream saida = new ObjectOutputStream(baos);
            saida.writeObject(chavesServidor.getPublic());
            saida.flush();
            byte[] respostaBytes = baos.toByteArray();

            // 5 - Envia resposta UDP de volta para quem mandou o pacote
            DatagramPacket pacoteResposta = new DatagramPacket(
                    respostaBytes,
                    respostaBytes.length,
                    pacote.getAddress(),
                    pacote.getPort()
            );

            cliente.send(pacoteResposta);

            return true;

        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            System.err.println("Erro durante troca de chaves UDP: " + e.getMessage());
            return false;
        }
    }
}
