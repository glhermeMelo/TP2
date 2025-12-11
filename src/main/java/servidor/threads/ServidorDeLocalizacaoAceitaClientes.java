package servidor.threads;

import entities.InfoServidorBorda;
import servidor.ServidorLocalizacao;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServidorDeLocalizacaoAceitaClientes implements Runnable {
    private final Socket cliente;
    protected final ConcurrentHashMap<String, KeyPair> chavesClientes;
    protected final ConcurrentHashMap<String, List<InfoServidorBorda>> localizacaoServidoresDeBorda;

    public ServidorDeLocalizacaoAceitaClientes(Socket cliente,
                                               ConcurrentHashMap<String, KeyPair> chavesClientes,
                                               ConcurrentHashMap<String, List<InfoServidorBorda>> localizacaoServidoresDeBorda) {
        this.cliente = cliente;
        this.chavesClientes = chavesClientes;
        this.localizacaoServidoresDeBorda = localizacaoServidoresDeBorda;
    }

    @Override
    public void run() {
        ObjectInputStream entrada = null;
        ObjectOutputStream saida = null;
        try {
            entrada = new ObjectInputStream(cliente.getInputStream());
            saida = new ObjectOutputStream(cliente.getOutputStream());
            saida.flush();

            Object entrada1 = entrada.readObject();
            if (!(entrada1 instanceof String)) {
                System.err.println("Protocolo inválido: esperado idDispositivo ou mensagem do servidor.");
                return;
            }

            String idDispositivo = (String) entrada1;

            if (idDispositivo.contains("BORDA")) {
                String[] mensagemBorda = idDispositivo.split("\\|");

                if (mensagemBorda.length >= 3) {
                    String local = mensagemBorda[1];
                    String ipPorta = mensagemBorda[2];

                    List<InfoServidorBorda> lista = localizacaoServidoresDeBorda.get(local);
                    if (lista == null) {
                        localizacaoServidoresDeBorda.putIfAbsent(local, new CopyOnWriteArrayList<>());
                        lista = localizacaoServidoresDeBorda.get(local);
                    }

                    boolean existe = false;
                    for (InfoServidorBorda info : lista) {
                        if (info.getEndereco().equals(ipPorta)) {
                            existe = true;
                            break;
                        }
                    }

                    if (!existe) {
                        lista.add(new InfoServidorBorda(ipPorta));
                    }
                    return;
                }
            }

            Object entrada2 = entrada.readObject();


            if (entrada2 instanceof PublicKey) {
                PublicKey chavePublicaCliente = (PublicKey) entrada2;

                KeyPair chavesServidor = chavesClientes.computeIfAbsent(idDispositivo, k -> gerarParDeChavesRSA());

                saida.writeObject(chavesServidor.getPublic());
                saida.flush();
                System.out.println("Troca de chaves RSA concluída com " + cliente.getInetAddress().getHostAddress() + ":" + cliente.getPort());

            } else if (entrada2 instanceof byte[]) {
                byte[] bytesChaveSessao = (byte[]) entrada2;

                Object entrada3 = entrada.readObject();
                if (!(entrada3 instanceof byte[])) {
                    System.err.println("Ocorreu um erro ao receber nonce");
                    return;
                }
                byte[] nonce = (byte[]) entrada3;

                Object entrada4 = entrada.readObject();
                if (!(entrada4 instanceof byte[])) {
                    System.err.println("Ocorreu um erro ao receber mensagem encriptada");
                    return;
                }
                byte[] mensagemEncriptada = (byte[]) entrada4;

                processarLocalizacao(idDispositivo, bytesChaveSessao, nonce, mensagemEncriptada, saida);
            } else {
                System.err.println("Objeto inesperado recebido do cliente: " + entrada2.getClass().getSimpleName());
            }

        } catch (EOFException e) {
            e.printStackTrace();
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

    private void processarLocalizacao(String idDispositivo, byte[] bytesChaveSessao, byte[] nonce, byte[] payload, ObjectOutputStream saida) throws IOException {
        KeyPair kpServidor = chavesClientes.get(idDispositivo);
        if (kpServidor == null) {
            System.err.println("Nenhuma chave identificada para o cliente: " + idDispositivo);
            saida.writeObject("ERRO: Chave não encontrada");
            saida.flush();
            return;
        }

        byte[] chaveSessao = descriptografarRSA(bytesChaveSessao, kpServidor.getPrivate());
        if (chaveSessao == null || chaveSessao.length != 32) {
            System.err.println("Falha ao decifrar chave de sessão.");
            saida.writeObject("ERRO: Falha na criptografia");
            saida.flush();
            return;
        }

        String localizacao = descriptografarLocalizacao(chaveSessao, nonce, payload);

        String enderecoBorda = "ERRO: Localização desconhecida";

        if (localizacao != null) {
            List<InfoServidorBorda> lista = localizacaoServidoresDeBorda.get(localizacao);

            if (lista == null || lista.isEmpty()) {
                System.err.println("Localização não encontrada: " + localizacao);
            } else {
                List<InfoServidorBorda> ativos = new ArrayList<>();
                for (InfoServidorBorda info : lista) {
                    if (info.isActive()) {
                        ativos.add(info);
                    }
                }

                if (!ativos.isEmpty()) {
                    int index = new Random().nextInt(ativos.size());
                    String escolhido = ativos.get(index).getEndereco();
                    System.out.println("Localização '" + localizacao + "' mapeada para " + escolhido);
                    enderecoBorda = escolhido;
                } else {
                    System.err.println("Nenhum servidor ativo para: " + localizacao);
                    enderecoBorda = "ERRO: Indisponivel";
                }
            }
        }

        saida.writeObject(enderecoBorda);
        saida.flush();
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

    private String descriptografarLocalizacao(byte[] chaveSessao, byte[] nonce, byte[] textoPlanoEncriptado) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(chaveSessao, "ChaCha20");
            IvParameterSpec param = new IvParameterSpec(nonce);

            Cipher chacha = Cipher.getInstance("ChaCha20-Poly1305");
            chacha.init(Cipher.DECRYPT_MODE, secretKey, param);

            byte[] textoPlano = chacha.doFinal(textoPlanoEncriptado);
            return new String(textoPlano);
        } catch (Exception e) {
            System.err.println("Erro ao decifrar ChaCha20 (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
        return null;
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
}