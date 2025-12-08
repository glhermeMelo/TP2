package servidor.threads;

import com.google.gson.Gson;
import entities.RegistroClimatico;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProxyAceitaServidoresDeBorda implements Runnable {
    private final Socket cliente;
    private ConcurrentHashMap<String, KeyPair> chavesClientes;
    private ConcurrentHashMap<String, KeyPair> chavesDatacenter;
    private ConcurrentHashMap<Integer, List<String>> bufferRegistros;

    private final String ipDatacenter;
    private final int portaDatacenter;
    private Socket socketDatacenter;
    private ObjectOutputStream outDatacenter;
    private ObjectInputStream inDatacenter;

    private boolean isActive = true;
    private ObjectInputStream entrada;
    private ObjectOutputStream saida;

    //Dados do cliente
    private String idCliente;
    private SecretKey chaveSecretaCliente;

    public ProxyAceitaServidoresDeBorda(
            Socket cliente,
            String ipDatacenter,
            int portaDatacenter,
            ConcurrentHashMap<String, KeyPair> chavesClientes,
            ConcurrentHashMap<String, KeyPair> chavesDatacenter) {

        this.ipDatacenter = ipDatacenter;
        this.portaDatacenter = portaDatacenter;
        this.chavesClientes = chavesClientes;
        this.chavesDatacenter = chavesDatacenter;
        this.cliente = cliente;
        this.bufferRegistros = new ConcurrentHashMap<>();

        EnviaRegistros threadEnviaRegistros = new EnviaRegistros(bufferRegistros, ipDatacenter, portaDatacenter, "Proxy", 5000);
        Thread thread = new Thread(threadEnviaRegistros);
        thread.start();
    }

    @Override
    public void run() {
        try {
            entrada = new ObjectInputStream(cliente.getInputStream());
            saida = new ObjectOutputStream(cliente.getOutputStream());
            saida.flush();

            if (autenticaCliente()) {
                System.out.println(cliente.getInetAddress().getHostName() + ":" + cliente.getPort() + " autenticado com sucesso");
                processarDados();
            } else {
                System.err.println(cliente.getInetAddress().getHostName() + ":" + cliente.getPort() + " é um cliente inválido");
            }
        } catch (EOFException e) {
            System.out.println("Conexão encerrada pelo cliente.");
        } catch (IOException e) {
            System.err.println("Erro de I/O com cliente: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro desconhecido: " + e.getMessage());
        } finally {
            try {
                if (cliente != null && !cliente.isClosed()) {
                    cliente.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    private synchronized boolean autenticaCliente() {
        try {
            // 1 - Ler nome do dispositivo
            Object entrada1 = entrada.readObject();
            if (!(entrada1 instanceof String)) {
                System.err.println("Erro ao receber id do cliente: " + cliente.getInetAddress().getHostAddress() + ":" + cliente.getPort());
                return false;
            }

            this.idCliente = (String) entrada1;

            // 2 - gera o recupera chaves para o cliente
            KeyPair chavesCliente = chavesClientes.computeIfAbsent(idCliente, k -> gerarParDeChavesRSA());
            if (chavesCliente == null) {
                System.err.println("Erro ao receber ou localizar chave do cliente: " + cliente.getInetAddress().getHostAddress() + ":" + cliente.getPort());
                return false;
            }

            // 3 - envia chave pública para o cliente
            saida.writeObject(chavesCliente.getPublic());
            saida.flush();

            // 4 - receber chave AES cifrada
            Object entrada2 = entrada.readObject();
            if (!(entrada2 instanceof byte[])) {
                System.err.println("Erro ao receber chave AES");
                return false;
            }

            byte[] bytesChaveAESCifrada = (byte[]) entrada2;

            byte[] bytesChaveAESDecifrada = descriptografarRSA(bytesChaveAESCifrada, chavesCliente.getPrivate());

            if (bytesChaveAESDecifrada == null) {
                System.err.println("Falha ao decifrar chave AES.");
                return false;
            }

            this.chaveSecretaCliente = new SecretKeySpec(bytesChaveAESDecifrada, "AES");
                return true;
        } catch (ClassNotFoundException e) {
            System.err.println("Erro ao descriptografar RSA: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erro ao conectar com cliente: " + e.getMessage());
        }
        return false;
    }

    private synchronized void processarDados() {
        while(isActive) {
            try {
                Object entrada3 = entrada.readObject();

                if (entrada3 instanceof byte[]) {

                    byte[] bytesHashMap = descriptografarAES((byte[]) entrada3, chaveSecretaCliente);

                    ConcurrentHashMap<Integer, List<String>> mapaRecebido = recriarHashMap(bytesHashMap);

                    if (mapaRecebido != null) {
                        System.out.println("Recebida atualização de " + idCliente + " com " + mapaRecebido.size() + " registros.");
                        enviarDadosAoDatacenter(mapaRecebido);
                    }

                }
            } catch (EOFException e) {
                System.out.println("Conexão encerrada pelo cliente " + this.idCliente);
                break;
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erro durante processamento de dados de " + this.idCliente + ": " + e.getMessage());
                break;
            }
        }
    }

    private void parar() {
        this.isActive = false;
    }

    private synchronized void enviarDadosAoDatacenter(ConcurrentHashMap<Integer, List<String>> mapaRecebido) {
        if (mapaRecebido == null || mapaRecebido.isEmpty()) {
            System.err.println("Mapa vazio");
            return;
        }

        Gson gson = new Gson();

        mapaRecebido.forEach((idDispositivo, lista) -> {
            List<RegistroClimatico> novaListaRegistro = new ArrayList<>();

            for(String registroJson : lista) {
                try {
                    RegistroClimatico registro = gson.fromJson(registroJson, RegistroClimatico.class);
                    novaListaRegistro.add(registro);

                    List<String> listaDestino = bufferRegistros.get(idDispositivo);

                    if (listaDestino == null) {
                        bufferRegistros.putIfAbsent(idDispositivo, new CopyOnWriteArrayList<>());

                        listaDestino = bufferRegistros.get(idDispositivo);
                    }

                    listaDestino.add(registroJson);

                } catch (Exception e) {
                    System.err.println("Erro ao converter de String para registro: " + registroJson + " - " + e.getMessage());
                }
            }
        });
    }

    private ConcurrentHashMap<Integer, List<String>> recriarHashMap(byte[] bytesHashMap) {
        try {
            ByteArrayInputStream bytesEntrada = new ByteArrayInputStream(bytesHashMap);
            ObjectInputStream entradaObjeto = new ObjectInputStream(bytesEntrada);
            return (ConcurrentHashMap<Integer, List<String>>) entradaObjeto.readObject();
        } catch (IOException e) {
            System.err.println("Erro ao receber hashmap: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Erro ao recriar hashmap: " + e.getMessage());
        }
        return null;
    }

    private KeyPair gerarParDeChavesRSA() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048, new SecureRandom());
            return  kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erro ao gerar par de chaves RSA: " + e.getMessage());
            return null;
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

    private byte[] descriptografarAES(byte[] dados, SecretKey chavePrivada) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, chavePrivada);
            return cipher.doFinal(dados);
        } catch (NoSuchPaddingException e) {
            System.err.println("Erro ao decifrar AES: " + e.getMessage());
        } catch (InvalidKeyException e) {
            System.err.println("Erro ao decifrar AES: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erro ao decifrar AES: " + e.getMessage());
        } catch (IllegalBlockSizeException e) {
            System.err.println("Erro ao decifrar AES: " + e.getMessage());
        } catch (BadPaddingException e) {
            System.err.println("Erro ao decifrar AES: " + e.getMessage());
        }
        return null;
    }
}
