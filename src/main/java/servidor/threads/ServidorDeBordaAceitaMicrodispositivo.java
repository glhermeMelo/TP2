package servidor.threads;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorDeBordaAceitaMicrodispositivo implements Runnable {
    private final Socket cliente;
    protected final ConcurrentHashMap<String, KeyPair> chavesClientes;

    public ServidorDeBordaAceitaMicrodispositivo(Socket cliente, ConcurrentHashMap<String, KeyPair> chavesClientes) {
        this.cliente = cliente;
        this.chavesClientes = chavesClientes;
    }

    @Override
    public void run() {
        ObjectInputStream entrada = null;
        ObjectOutputStream saida = null;
        try {
            entrada = new ObjectInputStream(cliente.getInputStream());
            saida = new ObjectOutputStream(cliente.getOutputStream());
            saida.flush();

            // 1 - Ler IdDispositivo
            Object entrada1 = entrada.readObject();
            if (!(entrada1 instanceof String)) {
                System.err.println("Protocolo inválido: esperado idDispositivo.");
                return;
            }

            String idDispositivo = (String) entrada1;

            // 2 - Ler Chave Pública do Cliente
            Object entrada2 = entrada.readObject();

            if (entrada2 instanceof PublicKey) {
                PublicKey chavePublicaCliente = (PublicKey) entrada2;

                // Gera par de chaves para este cliente se não existir
                if (!chavesClientes.containsKey(idDispositivo)) {
                    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                    kpg.initialize(2048, new SecureRandom());
                    chavesClientes.put(idDispositivo, kpg.generateKeyPair());
                }

                // Envia chave pública do servidor
                saida.writeObject(chavesClientes.get(idDispositivo).getPublic());
                saida.flush();
            }
        } catch (EOFException e) {
            // Conexão encerrada normalmente
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (cliente != null) cliente.close();
            } catch (IOException ignored) {
            }
        }
    }
}
