package servidor.threads;

import com.google.gson.Gson;
import entities.RegistroClimatico;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DatacenterAceita implements Runnable {
    private final Socket cliente;
    private final ConcurrentHashMap<String, KeyPair> chavesClientes;
    private final ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGlobais;
    private final List<Integer> portasRepasse;
    private final Map<Integer, ConcurrentHashMap<Integer, List<String>>> buffersRepasse;
    private final String nomeDataCenter;
    private boolean isActive = true;

    public DatacenterAceita(Socket cliente,
                            ConcurrentHashMap<String, KeyPair> chavesClientes,
                            ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGlobais,
                            List<Integer> portasRepasse,
                            Map<Integer, ConcurrentHashMap<Integer, List<String>>> buffersRepasse,
                            String nomeDataCenter) {
        this.cliente = cliente;
        this.chavesClientes = chavesClientes;
        this.dadosGlobais = dadosGlobais;
        this.portasRepasse = portasRepasse;
        this.buffersRepasse = buffersRepasse;
        this.nomeDataCenter = nomeDataCenter;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream entrada = new ObjectInputStream(cliente.getInputStream());
            ObjectOutputStream saida = new ObjectOutputStream(cliente.getOutputStream());
            saida.flush();

            // 1. Handshake
            Object objId = entrada.readObject();
            if (objId instanceof String) {
                String idCliente = (String) objId;

                KeyPair chaves = chavesClientes.computeIfAbsent(idCliente, k -> gerarParDeChavesRSA());
                saida.writeObject(chaves.getPublic());
                saida.flush();

                Object objKey = entrada.readObject();
                if (objKey instanceof byte[]) {
                    byte[] aesDecifrada = descriptografarRSA((byte[]) objKey, chaves.getPrivate());
                    SecretKey chaveAES = new SecretKeySpec(aesDecifrada, "AES");

                    System.out.println(nomeDataCenter + ": Conexão autenticada com " + idCliente);

                    while (isActive) {
                        try {
                            Object dadosCifrados = entrada.readObject();
                            if (dadosCifrados instanceof byte[]) {
                                byte[] dadosMap = descriptografarAES((byte[]) dadosCifrados, chaveAES);
                                ConcurrentHashMap<Integer, List<String>> lote = recriarHashMap(dadosMap);

                                processarLote(lote);
                            }
                        } catch (EOFException e) {
                            System.out.println("Cliente " + idCliente + " desconectou.");
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro na ingestão de dados: " + e.getMessage());
        } finally {
            try {
                cliente.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void processarLote(ConcurrentHashMap<Integer, List<String>> lote) {
        Gson gson = new Gson();
        boolean repassar = (portasRepasse != null && !portasRepasse.isEmpty());

        lote.forEach((idSensor, listaJson) -> {
            List<RegistroClimatico> listaDestino = dadosGlobais.computeIfAbsent(idSensor, k -> new CopyOnWriteArrayList<>());

            for (String json : listaJson) {
                try {
                    RegistroClimatico registro = gson.fromJson(json, RegistroClimatico.class);
                    listaDestino.add(registro);

                    if (repassar) {
                        for (ConcurrentHashMap<Integer, List<String>> buffer : buffersRepasse.values()) {
                            buffer.computeIfAbsent(idSensor, k -> new CopyOnWriteArrayList<>()).add(json);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("JSON inválido: " + e.getMessage());
                }
            }
        });
    }

    private KeyPair gerarParDeChavesRSA() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048, new SecureRandom());
            return kpg.generateKeyPair();
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] descriptografarRSA(byte[] msg, PrivateKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(msg);
    }

    private byte[] descriptografarAES(byte[] msg, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(msg);
    }

    private ConcurrentHashMap<Integer, List<String>> recriarHashMap(byte[] bytes) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (ConcurrentHashMap<Integer, List<String>>) ois.readObject();
        }
    }

    public void parar() {
        this.isActive = false;
    }
}