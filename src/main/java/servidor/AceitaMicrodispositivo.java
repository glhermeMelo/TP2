package servidor;

import microdispositivo.util.GeradorDeLeituras;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AceitaMicrodispositivo implements Runnable {
    private final Socket cliente;
    private final ConcurrentHashMap<Integer, List<GeradorDeLeituras>> mapaDeRegistrosClimaticos;
    private final ConcurrentHashMap<String, KeyPair> chavesClientes;
    private int portaDatacenter;

    public AceitaMicrodispositivo(Socket cliente, ConcurrentHashMap<Integer, List<GeradorDeLeituras>> mapaDeRegistrosClimaticos,
                                  ConcurrentHashMap<String, KeyPair> chavesClientes) {
        this.cliente = cliente;
        this.chavesClientes = chavesClientes;
        this.mapaDeRegistrosClimaticos = mapaDeRegistrosClimaticos;
    }

    @Override
    public void run() {
        ObjectInputStream entrada = null;
        ObjectOutputStream saida = null;

        try {

            entrada = new ObjectInputStream(cliente.getInputStream());
            saida = new ObjectOutputStream(cliente.getOutputStream());
            saida.flush();

            while (true) {
            // 1 - Ler IdDispositivo
            Object entrada1 = entrada.readObject();

            if (!(entrada1 instanceof String)) {
                System.err.println("Ocorreu um erro ao receber entrada");
                return;
            }

            String idDispositivo =  (String) entrada1;

            // 2 - Ler bytes chave sessao
            Object entrada2 = entrada.readObject();
            if ((entrada2 instanceof PublicKey)) {
                PublicKey chavePublicaCliente = (PublicKey) entrada2;

                if (!chavesClientes.contains(chavePublicaCliente)) {
                    KeyPair chaves = gerarParDeChavesRSA();
                    chavesClientes.put(idDispositivo, chaves);
                }

                KeyPair chavesServidor = chavesClientes.get(idDispositivo);

                saida.writeObject(chavesServidor.getPublic());
                saida.flush();

            } else if (entrada2 instanceof byte[]) {
                byte[] bytesChaveSessao = (byte[]) entrada2;

                // 3 - Ler nonce
                Object entrada3 = entrada.readObject();
                if (!(entrada3 instanceof byte[])) {
                    System.err.println("Erro: Esperado nonce (byte[])");
                    return;
                }
                byte[] nonce = (byte[]) entrada3;

                // 4 - Ler mensagemEncriptada
                Object entrada4 = entrada.readObject();
                if (!(entrada4 instanceof byte[])) {
                    System.err.println("Erro: Esperado payload cifrado (byte[])");
                    return;
                }
                byte[] mensagemEncriptada = (byte[]) entrada4;

                // Processar a leitura
                descriptografarRegistro(idDispositivo, bytesChaveSessao, nonce, mensagemEncriptada);
            } else {
                System.err.println("Objeto inesperado recebido: " + entrada2.getClass().getSimpleName());
            }
            }
        } catch (EOFException e) {
            System.out.println("Conexão encerrada pelo cliente " + cliente.getPort());
        } catch (IOException e) {
            System.err.println("Erro ao estabelecer conexao com o cliente: " + cliente.getPort() + " - " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro inesperado ao cifrar/enviar a localização: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void analisaRegistroClimatico() {

    }

    private void descriptografarRegistro(String idDispositivo,byte[] chaveSessao, byte[] nonce, byte[] mensagemCifrada) {
        try {

            KeyPair chavesCliente = chavesClientes.get(idDispositivo);
            if (chavesCliente == null) {
                System.err.println("Nenhuma chave identificada para o dispositivo: " + idDispositivo + ". O handshake foi realizado?");
                return;
            }

            byte[] bytesChaveSessao = descriptografarRSA(chaveSessao, chavesCliente.getPrivate());
            if (bytesChaveSessao == null || bytesChaveSessao.length != 32) {
                System.err.println("Falha ao decifrar chave de sessão (RSA).");
                return;
            }

            SecretKeySpec secretKey = new SecretKeySpec(bytesChaveSessao, "ChaCha20");
            IvParameterSpec param = new IvParameterSpec(nonce);

            Cipher chacha = Cipher.getInstance("ChaCha20-Poly1305");
            chacha.init(Cipher.DECRYPT_MODE, secretKey, param);

            byte[] bytesRegistroClimatico = chacha.doFinal(mensagemCifrada);

            String registroClimatico = new String(bytesRegistroClimatico);
            System.out.println(registroClimatico);
        } catch (Exception e) {
            System.err.println("Erro ao decifrar ChaCha20 (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    private KeyPair gerarParDeChavesRSA() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048, new SecureRandom());
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("RSA não disponível", e);
        }
    }

    private byte[] descriptografarRSA(byte[] bytesChaveSessao, PrivateKey chavePrivada) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, chavePrivada);
            return cipher.doFinal(bytesChaveSessao);
        } catch (Exception e) {
            System.err.println("Erro ao decifrar RSA: " + e.getMessage());
            return null;
        }
    }
}
