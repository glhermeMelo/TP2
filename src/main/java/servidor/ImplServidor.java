package servidor;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.security.*;
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

    protected String processarPacoteUDP(DatagramPacket pacote) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(pacote.getData());
            ObjectInputStream ois = new ObjectInputStream(bais);

            // 1 - ler dados
            String idDispositivo = (String) ois.readObject();
            byte[] bytesChaveSessaoCifrada = (byte[]) ois.readObject();
            byte[] nonce = (byte[]) ois.readObject();
            byte[] bytesMensagemEncriptada = (byte[]) ois.readObject();

            // 2 - recuperar chaves
            KeyPair kp = chavesClientes.get(idDispositivo);
            if (kp == null) {
                System.err.println("Chave não encontrada para dispositivo: " + idDispositivo + ". Handshake foi feito?");
                return null;
            }

            // 3 - descriptografar chave RSA
            byte[] chaveSessao = descriptografarRSA(bytesChaveSessaoCifrada, kp.getPrivate());

            // 4 - descriptografar mensagem ChaCha20
            return descriptografarChaCha20(chaveSessao, nonce,  bytesMensagemEncriptada);

        } catch (IOException e) {
            System.err.println("Erro ao processar pacoteUDP: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Erro ao ler mensagem! " + e.getMessage());
        }
        return null;
    }

    protected KeyPair gerarParDeChavesRSA() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048, new SecureRandom());
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erro ao gerar par de chaves RSA: " + e.getMessage());
            return null;
        }
    }

    protected byte[] descriptografarRSA(byte[] bytesChaveSessao, PrivateKey chavePrivada) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, chavePrivada);
            return cipher.doFinal(bytesChaveSessao);
        } catch (Exception e) {
            System.err.println("Erro ao decifrar RSA: " + e.getMessage());
            return null;
        }
    }

    private String descriptografarChaCha20(byte[] bytesChaveSessao, byte[] nonce, byte[] mensagemCifrada) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(bytesChaveSessao, "ChaCha20");
            IvParameterSpec param = new IvParameterSpec(nonce);

            Cipher chacha = Cipher.getInstance("ChaCha20-Poly1305");
            chacha.init(Cipher.DECRYPT_MODE, secretKey, param);

            byte[] bytesMensagem = chacha.doFinal(mensagemCifrada);

            return new String(bytesMensagem);

        } catch (Exception e) {
            System.err.println("Erro ao decifrar ChaCha20 (" + e.getClass().getSimpleName() + "): " + e.getMessage());
            return null;
        }
    }
}
