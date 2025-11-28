package servidor.threads;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorDeLocalizacaoAceitaClientes implements Runnable {
    private final Socket cliente;
    protected final ConcurrentHashMap<String, KeyPair> chavesClientes;
    protected final ConcurrentHashMap<String, String> localizacaoServidoresDeBorda;
    protected final ConcurrentHashMap<String, String> servicosRMI;

    public ServidorDeLocalizacaoAceitaClientes(Socket cliente,
                                               ConcurrentHashMap<String, KeyPair> chavesClientes,
                                               ConcurrentHashMap<String, String> localizacaoServidoresDeBorda,
                                               ConcurrentHashMap<String, String> servicosRMI) {
        this.cliente = cliente;
        this.chavesClientes = chavesClientes;
        this.localizacaoServidoresDeBorda = localizacaoServidoresDeBorda;
        this.servicosRMI = servicosRMI;
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

            // 2 - Ler segundo objeto (Define se é cliente rmi ou microdispositivo)
            Object entrada2 = entrada.readObject();

            // Caso ClienteRMI
            if(entrada2 instanceof String ){
                String nomeServico = (String) entrada2;

                System.out.println("Cliente RMI '" + idDispositivo + "' solicitando serviço: " + nomeServico);

                tratarClienteRMI(nomeServico, saida);
                return;
            }

            // Caso Microdispositivo
            if (entrada2 instanceof PublicKey) {
                PublicKey chavePublicaCliente = (PublicKey) entrada2;

                // Gera ou recupera chaves do servidor para este cliente
                KeyPair chavesServidor = chavesClientes.computeIfAbsent(idDispositivo, k -> gerarParDeChavesRSA());

                // Envia chave pública do servidor de volta
                saida.writeObject(chavesServidor.getPublic());
                saida.flush();
                System.out.println("Troca de chaves RSA concluída com " + cliente.getInetAddress().getHostAddress() + ":" + cliente.getPort());
                // Caso RMI
            } else if (entrada2 instanceof byte[]) {
                byte[] bytesChaveSessao = (byte[]) entrada2;

                // 3 - ler nonce
                Object entrada3 = entrada.readObject();
                if (!(entrada3 instanceof byte[])) {
                    System.err.println("Ocorreu um erro ao receber nonce");
                    return;
                }
                byte[] nonce = (byte[]) entrada3;

                // 4 - Ler mensagemEncriptada
                Object entrada4 = entrada.readObject();
                if (!(entrada4 instanceof byte[])) {
                    System.err.println("Ocorreu um erro ao receber mensagem encriptada");
                    return;
                }
                byte[] mensagemEncriptada = (byte[]) entrada4;

                // Processa a localização com os dados coletados
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
            } catch (IOException ignored) {}
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

        // Decifrar chave de sessão (RSA)
        byte[] chaveSessao = descriptografarRSA(bytesChaveSessao, kpServidor.getPrivate());
        if (chaveSessao == null || chaveSessao.length != 32) {
            System.err.println("Falha ao decifrar chave de sessão.");
            saida.writeObject("ERRO: Falha na criptografia");
            saida.flush();
            return;
        }

        // Decifrar localização (ChaCha20-Poly1305)
        String localizacao = descriptografarLocalizacao(chaveSessao, nonce, payload);

        String enderecoBorda = "ERRO: Localização desconhecida";

        if (localizacao != null) {
            String encontrado = localizacaoServidoresDeBorda.get(localizacao);

            if (encontrado == null) {
                System.err.println("Localização não encontrada: " + localizacao);
            } else {
                System.out.println("Localização '" + localizacao + "' mapeada para " + encontrado);
                enderecoBorda = encontrado;
            }
        }

        saida.writeObject(enderecoBorda);
        saida.flush();
    }

    private void tratarClienteRMI(String nomeServico, ObjectOutputStream saida) {
        String portaServico = servicosRMI.get(nomeServico);
        try {
            if (portaServico != null) {
                saida.writeObject(portaServico);
                System.out.println("Endereco de: " + nomeServico + ", enviado ao cliente: " + cliente.getInetAddress().getHostAddress() + ":" + cliente.getPort());
            } else {
                saida.writeObject("ERRO: Serviço não encontrado");
                System.err.println("Serviço '" + nomeServico + "' não encontrado.");
            }
            saida.flush();
        } catch (IOException e) {
            System.err.println("Erro ao localizar servico: " + nomeServico + "! " + e.getMessage());
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