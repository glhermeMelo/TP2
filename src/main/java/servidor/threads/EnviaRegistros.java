package servidor.threads;

import javax.crypto.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class EnviaRegistros implements Runnable {
    private final ConcurrentHashMap<Integer, List<String>> mapaDeRegistrosClimaticos;
    private final String ipDatacenter;
    private final int portaDatacenter;
    private final String nomeServidorDeBorda;
    private boolean isActive = true;
    private long escritaMillis;

    public EnviaRegistros(ConcurrentHashMap<Integer, List<String>> mapaDeRegistrosClimaticos,
                          String ipDatacenter,
                          int portaDatacenter,
                          String nomeServidorDeBorda,
                          long escritaMillis) {
        this.mapaDeRegistrosClimaticos = mapaDeRegistrosClimaticos;
        this.ipDatacenter = ipDatacenter;
        this.portaDatacenter = portaDatacenter;
        this.nomeServidorDeBorda = nomeServidorDeBorda;
        this.escritaMillis = escritaMillis;
    }

    @Override
    public void run() {
        while (isActive) {
            System.out.println(nomeServidorDeBorda + ": Tentando conectar ao DataCenter em " + ipDatacenter + ":" + portaDatacenter);

            try (Socket socket = new Socket(ipDatacenter, portaDatacenter)) {
                ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
                saida.flush();
                ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());

                System.out.println(socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + " conectado ao Datacenter! ");

                saida.writeObject(nomeServidorDeBorda);

                Object entrada1 = entrada.readObject();
                if (!(entrada1 instanceof PublicKey)) {
                    System.err.println("Erro ao receber chave pública do Datacenter");
                    continue;
                }

                PublicKey chavePublicaDatacenter = (PublicKey) entrada1;

                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(128);
                SecretKey chaveSecretaAES = keyGen.generateKey();

                byte[] chaveAES = cifrarRSA(chaveSecretaAES.getEncoded(), chavePublicaDatacenter);
                saida.writeObject(chaveAES);
                saida.flush();

                if (!mapaDeRegistrosClimaticos.isEmpty()) {
                    byte[] bytesHashMap = mapaToBytes();
                    byte[] byesHashMapCifrado = cifrarAES(bytesHashMap, chaveSecretaAES);

                    if (byesHashMapCifrado != null) {
                        saida.writeObject(byesHashMapCifrado);
                        saida.flush();

                        mapaDeRegistrosClimaticos.clear();
                        System.out.println(nomeServidorDeBorda + " enviando HashMap ao Datacenter " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                    }
                }

                Thread.sleep(escritaMillis);

            } catch (Exception e) {
                System.err.println("Erro na thread de envio: " + e.getMessage());
                try {
                    Thread.sleep(escritaMillis);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (Throwable t) {
                System.err.println("Erro fatal na thread de envio: " + t.getMessage());
                t.printStackTrace();
            }
        }
    }

    private byte[] mapaToBytes() {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(mapaDeRegistrosClimaticos);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] cifrarAES(byte[] dados, SecretKey chaveSecreta) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, chaveSecreta);
            return cipher.doFinal(dados);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erro: algoritmo criptográfico não disponível no ambiente. Detalhe: " + e.getMessage());
        } catch (NoSuchPaddingException e) {
            System.err.println("Erro: esquema de padding não disponível. Detalhe: " + e.getMessage());
        } catch (InvalidKeyException e) {
            System.err.println("Erro: chave inválida ao inicializar o cifrador.");
        } catch (IllegalBlockSizeException e) {
            System.err.println("Erro: tamanho de bloco ilegal ao cifrar dados com AES.");
        } catch (BadPaddingException e) {
            System.err.println("Erro de padding durante a cifragem.");
        } catch (Exception e) {
            System.err.println("Erro inesperado ao cifrar/enviar: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        return null;
    }

    private byte[] cifrarRSA(byte[] bytesMensagem, PublicKey chavePublica) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, chavePublica);
            return cipher.doFinal(bytesMensagem);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erro: algoritmo criptográfico não disponível no ambiente. Detalhe: " + e.getMessage());
        } catch (NoSuchPaddingException e) {
            System.err.println("Erro: esquema de padding não disponível. Detalhe: " + e.getMessage());
        } catch (InvalidKeyException e) {
            System.err.println("Erro: chave inválida ao inicializar o cifrador.");
        } catch (IllegalBlockSizeException e) {
            System.err.println("Erro: tamanho de bloco ilegal ao cifrar dados com RSA.");
        } catch (BadPaddingException e) {
            System.err.println("Erro de padding durante a cifragem.");
        } catch (Exception e) {
            System.err.println("Erro inesperado ao cifrar/enviar: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        return null;
    }
}