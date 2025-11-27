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

public class DataCenterAceitaServidoresDeBorda implements Runnable {
    private final Socket cliente;
    private ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGlobais;
    private ConcurrentHashMap<String, KeyPair> chavesClientes;
    private boolean isActive = true;

    public DataCenterAceitaServidoresDeBorda(
            Socket cliente,
            ConcurrentHashMap<String,KeyPair> chavesClientes,
            ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGlobais) {
        this.cliente = cliente;
        this.dadosGlobais = dadosGlobais;
        this.chavesClientes = chavesClientes;
    }

    @Override
    public void run() {
        ObjectInputStream entrada = null;
        ObjectOutputStream saida = null;

        try {
            entrada = new ObjectInputStream(cliente.getInputStream());
            saida = new ObjectOutputStream(cliente.getOutputStream());

            // 1 - Ler nome do dispositivo
            Object entrada1 = entrada.readObject();
            if (!(entrada1 instanceof String)) {
                System.err.println("Erro ao receber entrada");
            }

            String idCliente = (String) entrada1;
            System.out.println("Conexao iniciada com: " + idCliente);

            // 2 - gera o recupera chaves para o cliente
            KeyPair chavesCliente = chavesClientes.computeIfAbsent(idCliente, k -> gerarParDeChavesRSA());

            // 3 - envia chave pública para o cliente
            saida.writeObject(chavesCliente.getPublic());
            saida.flush();

            // 4 - receber chave AES cifrada
            Object entrada2 = entrada.readObject();
            if (!(entrada2 instanceof byte[])) {
                System.err.println("Erro ao receber chave AES (byte[])");
                return;
            }

            byte[] bytesChaveAESCifrada = (byte[]) entrada2;

            byte[] bytesChaveAESDecifrada = descriptografarRSA(bytesChaveAESCifrada, chavesCliente.getPrivate());
            SecretKey chaveAES = new SecretKeySpec(bytesChaveAESDecifrada, "AES");

            while(isActive) {
                try {
                    Object entrada3 = entrada.readObject();

                    if (entrada3 instanceof byte[]) {

                        byte[] bytesHashMap = descriptografarAES((byte[]) entrada3, chaveAES);

                        // recriando HashMap
                        ConcurrentHashMap<Integer, List<String>> mapaRecebido = recriarHashMap(bytesHashMap);

                        if (mapaRecebido != null) {
                            System.out.println("Recebida atualização de " + idCliente + " com " + mapaRecebido.size() + " registros.");
                            atualizarDadosGlobais(mapaRecebido);

                            dadosGlobais.forEach((id, lista) -> {
                                System.out.println("Dispositivo: " + id);
                                System.out.println(lista);
                            });
                        }

                    }
                } catch (EOFException e) {
                    System.out.println("Conexão encerrada pelo cliente " + idCliente);
                    break;
                }
            }
        } catch (EOFException e) {
            System.out.println("Conexão encerrada pelo cliente " + cliente.getInetAddress().getHostAddress() + ":" + cliente.getPort());
        } catch (IOException e) {
            System.err.println("Erro ao estabelecer conexao com o cliente: " + cliente.getInetAddress() + ":" + cliente.getPort() + " - " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro inesperado ao cifrar/enviar a localização: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private synchronized void atualizarDadosGlobais(ConcurrentHashMap<Integer, List<String>> mapaRecebido) {
        Gson gson = new Gson();

        mapaRecebido.forEach((idDispositivo, lista) -> {
            List<RegistroClimatico> novaListaRegistro = new ArrayList<>();

            for(String registroJson : lista) {
                try {
                    RegistroClimatico registro = gson.fromJson(registroJson, RegistroClimatico.class);
                    novaListaRegistro.add(registro);
                } catch (Exception e) {
                    System.err.println("Erro ao converter de String para registro: " + registroJson + " - " + e.getMessage());
                }
            }

            dadosGlobais.merge(idDispositivo, novaListaRegistro, (listaRegistro, novaLista) -> {
                listaRegistro.addAll(novaLista);
                return listaRegistro;
            });

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
