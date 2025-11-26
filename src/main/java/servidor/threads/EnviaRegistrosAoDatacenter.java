package servidor.threads;

import javax.crypto.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.c;

public class EnviaRegistrosAoDatacenter implements Runnable {
    private final ConcurrentHashMap<Integer, List<String>> mapaDeRegistrosClimaticos;
    private final String ipDatacenter;
    private final int portaDatacenter;
    private final String nomeServidorDeBorda;
    private boolean isActive = true;
    private long escritaMillis;

    public EnviaRegistrosAoDatacenter(ConcurrentHashMap<Integer, List<String>> mapaDeRegistrosClimaticos,
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

                // 1 - enviar nome do servidor
                saida.writeObject(nomeServidorDeBorda);

                // 2 -  receber chave pública do servidor
                Object entrada1 = entrada.readObject();
                if (!(entrada1 instanceof PublicKey)) {
                    System.err.println("Erro ao receber chave pública do Datacenter");
                    return;
                }

                PublicKey chavePublicaDatacenter = (PublicKey) entrada1;

                // 3 - Gerar chave de sessão
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(128);
                SecretKey chaveSecretaAES = keyGen.generateKey();

                // 4 - envia chave AES cifrada ao Datacenter
                byte[] chaveAES = cifrarRSA(chaveSecretaAES.getEncoded(), chavePublicaDatacenter);
                saida.writeObject(chaveAES);
                saida.flush();

                while (isActive) {
                    if (!mapaDeRegistrosClimaticos.isEmpty()) {
                        byte[] bytesHashMap = cifrarMapa();

                        byte[] byesHashMapCifrado = cifrarAES(bytesHashMap, chaveSecretaAES);

                        saida.writeObject(byesHashMapCifrado);
                        saida.flush();

                        System.out.println("HashMap enviado ao Datacenter");
                    }

                    Thread.sleep(escritaMillis);
                }

            } catch (IOException e) {
                System.err.println("Erro ao conectar ao DataCenter: " + e.getMessage());
                try {
                    Thread.sleep(escritaMillis);
                } catch (InterruptedException e1) {
                    System.err.println("Erro ao conectar ao DataCenter: " + e1.getMessage());
                }
            } catch (ClassNotFoundException e) {
                System.err.println("Erro ao receber chave do servidor! " + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                System.err.println("Erro ao gerar chaves RSA! " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private byte[] cifrarMapa() {
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
            System.err.println("Erro: algoritmo criptográfico não disponível no ambiente (provavelmente falta suporte ao algoritmo solicitado). Detalhe: " + e.getMessage());
        } catch (NoSuchPaddingException e) {
            System.err.println("Erro: esquema de padding não disponível (verifique o nome do algoritmo de Cipher). Detalhe: " + e.getMessage());
        } catch (InvalidKeyException e) {
            System.err.println("Erro: chave inválida ao inicializar o cifrador — verifique se a chave pública do servidor está correta e corresponde à chave privada do servidor.");
        } catch (IllegalBlockSizeException e) {
            System.err.println("Erro: tamanho de bloco ilegal ao cifrar dados com AES — possivelmente os dados (chave de sessão) são maiores que o permitido pela chave AES.");
        } catch (BadPaddingException e) {
            System.err.println("Erro de padding durante a cifragem — possível incompatibilidade de padding entre cliente e servidor (ex.: OAEP vs PKCS1).");
        } catch (Exception e) {
            System.err.println("Erro inesperado ao cifrar/enviar a localização: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private byte[] cifrarRSA(byte[] bytesMensagem, PublicKey chavePublica) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, chavePublica);
            return cipher.doFinal(bytesMensagem);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erro: algoritmo criptográfico não disponível no ambiente (provavelmente falta suporte ao algoritmo solicitado). Detalhe: " + e.getMessage());
        } catch (NoSuchPaddingException e) {
            System.err.println("Erro: esquema de padding não disponível (verifique o nome do algoritmo de Cipher). Detalhe: " + e.getMessage());
        } catch (InvalidKeyException e) {
            System.err.println("Erro: chave inválida ao inicializar o cifrador — verifique se a chave pública do servidor está correta e corresponde à chave privada do servidor.");
        } catch (IllegalBlockSizeException e) {
            System.err.println("Erro: tamanho de bloco ilegal ao cifrar dados com RSA — possivelmente os dados (chave de sessão) são maiores que o permitido pela chave RSA.");
        } catch (BadPaddingException e) {
            System.err.println("Erro de padding durante a cifragem — possível incompatibilidade de padding entre cliente e servidor (ex.: OAEP vs PKCS1).");
        } catch (Exception e) {
            System.err.println("Erro inesperado ao cifrar/enviar a localização: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}
