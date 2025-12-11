package microdispositivo;

import microdispositivo.threads.GeradorDeLeituras;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.security.*;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ImplMicrodispositivo {
    protected String ip = "localhost";
    protected int porta = 5000;
    protected String localizacao;

    protected LocalDateTime data;
    protected GeradorDeLeituras geradorDeLeituras;
    private Thread threadGeradora;
    private DatagramSocket socketUDP;

    private final int portaServidorLocalizacao;
    private int portaServidorDeBorda;
    private String enderecoBorda;

    protected ConcurrentHashMap<Integer, KeyPair> chavesServidorLocalizacao;

    protected ConcurrentHashMap<Integer, KeyPair> chavesServidorDeBorda;

    public ImplMicrodispositivo(String ip, int porta, long intervaloMillis, String idDispositivo, int servidorDescoberta, String localizacao) {
        this.ip = ip;
        this.porta = porta;
        this.portaServidorLocalizacao = servidorDescoberta;
        this.localizacao = localizacao;

        chavesServidorLocalizacao = new ConcurrentHashMap<>();
        chavesServidorDeBorda = new ConcurrentHashMap<>();

        geradorDeLeituras = new GeradorDeLeituras(idDispositivo, intervaloMillis, localizacao);

        try {
            this.socketUDP = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println("Erro ao abrir socket UDP! " + e.getMessage());
        }

        rodar();
    }

    protected void rodar() {
        realizarHandshakeUDP(this.ip, portaServidorLocalizacao, chavesServidorLocalizacao);

        criptografarLocalizacao();

        if (portaServidorDeBorda <= 0 || this.enderecoBorda == null) {
            System.err.println("Dados do servidor de borda inválidos! Abortando.");
            return;
        }

        realizarHandshakeUDP(this.enderecoBorda, portaServidorDeBorda, chavesServidorDeBorda);

        if (threadGeradora == null || !threadGeradora.isAlive()) {
            threadGeradora = new Thread(geradorDeLeituras);
            threadGeradora.start();
        }

        Thread monitora = geraThreadMonitora();
        monitora.start();
    }

    private Thread geraThreadMonitora() {
        Thread monitora = new Thread(() -> {
            try {
                while (geradorDeLeituras.isActive()) {
                    criptografarLocalizacao();

                    if (portaServidorDeBorda > 0 && enderecoBorda != null) {
                        realizarHandshakeUDP(enderecoBorda, portaServidorDeBorda, chavesServidorDeBorda);

                        String leituraJson = geradorDeLeituras.getLeitura();
                        enviarLeituraAoServidorDeBorda(leituraJson);
                    }

                    Thread.sleep(geradorDeLeituras.getIntervaloMillis());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        monitora.setDaemon(true);
        return monitora;
    }

    protected void parar() {
        geradorDeLeituras.parar();
        if (threadGeradora != null && threadGeradora.isAlive()) {
            threadGeradora.interrupt();
            try {
                threadGeradora.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (socketUDP != null && !socketUDP.isClosed()) {
            socketUDP.close();
        }
    }

    protected void criptografarLocalizacao() {
        KeyPair kp = chavesServidorLocalizacao.get(portaServidorLocalizacao);

        if (kp == null) {
            System.err.println("Nenhuma chave registrada para a porta de localização: " + portaServidorLocalizacao);
            return;
        }

        PublicKey chavePublicaServidor = kp.getPublic();

        try {
            SecureRandom sr = new SecureRandom();

            byte[] bytesChaveSessao = new byte[32];
            sr.nextBytes(bytesChaveSessao);
            SecretKey chaveSessao = new SecretKeySpec(bytesChaveSessao, "ChaCha20");

            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, chavePublicaServidor);
            byte[] bytesEncriptados = cipher.doFinal(bytesChaveSessao);

            byte[] nonce = new byte[12];
            sr.nextBytes(nonce);

            IvParameterSpec parameterSpec = new IvParameterSpec(nonce);
            Cipher chacha = Cipher.getInstance("ChaCha20-Poly1305");

            chacha.init(Cipher.ENCRYPT_MODE, chaveSessao, parameterSpec);

            byte[] bytesEncriptadosTextoPlano = chacha.doFinal(localizacao.getBytes());

            String deviceId = geradorDeLeituras.getIdDispositivo();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream saida = new ObjectOutputStream(baos);

            saida.writeObject(deviceId);
            saida.writeObject(bytesEncriptados);
            saida.writeObject(nonce);
            saida.writeObject(bytesEncriptadosTextoPlano);
            saida.flush();
            byte[] dadosEnviar = baos.toByteArray();

            InetAddress enderecoServidor = InetAddress.getByName(this.ip);

            DatagramPacket pacoteEnvio = new DatagramPacket(dadosEnviar, dadosEnviar.length, enderecoServidor, portaServidorLocalizacao);
            socketUDP.send(pacoteEnvio);
            System.out.println("Localização (cifrada) enviada com sucesso para " + ip + ":" + portaServidorLocalizacao);

            try {
                socketUDP.setSoTimeout(5000);

                byte[] bufferResp = new byte[4096];
                DatagramPacket pacoteResposta = new DatagramPacket(bufferResp, bufferResp.length);
                socketUDP.receive(pacoteResposta);

                ByteArrayInputStream bais = new ByteArrayInputStream(pacoteResposta.getData(), 0, pacoteResposta.getLength());
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object resposta = ois.readObject();

                if (resposta instanceof String) {
                    String endereco = (String) resposta;
                    if (endereco.startsWith("ERRO")) {
                        System.err.println("Servidor retornou erro: " + endereco);
                        realizarHandshakeUDP(this.ip, portaServidorLocalizacao, chavesServidorLocalizacao);

                        criptografarLocalizacao();

                        if (portaServidorDeBorda <= 0 || this.enderecoBorda == null) {
                            System.err.println("Dados do servidor de borda inválidos! Abortando.");
                            return;
                        }

                        realizarHandshakeUDP(this.enderecoBorda, portaServidorDeBorda, chavesServidorDeBorda);
                        return;
                    }
                    String[] partes = endereco.split(":");
                    if (partes.length == 2) {
                        this.enderecoBorda = partes[0];
                        this.portaServidorDeBorda = Integer.parseInt(partes[1]);
                        System.out.println("Redirecionado para Servidor de Borda em: " + enderecoBorda + ":" + portaServidorDeBorda);
                    }
                }
            } catch (SocketTimeoutException e) {
                System.err.println("ERRO: O servidor não respondeu a tempo (Timeout).");
            }
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erro: algoritmo criptográfico não disponível: " + e.getMessage());
        } catch (NoSuchPaddingException e) {
            System.err.println("Erro: esquema de padding não disponível: " + e.getMessage());
        } catch (InvalidKeyException e) {
            System.err.println("Erro: chave inválida ao inicializar o cifrador: " + e.getMessage());
        } catch (IllegalBlockSizeException e) {
            System.err.println("Erro: tamanho de bloco ilegal (RSA): " + e.getMessage());
        } catch (BadPaddingException e) {
            System.err.println("Erro de padding: " + e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            System.err.println("Erro: parâmetros inválidos (ChaCha20): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro inesperado ao cifrar/enviar a localização: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected void realizarHandshakeUDP(String ipDestino, int portaServidor, ConcurrentHashMap<Integer, KeyPair> chavesServidor) {
        try {
            if (!chavesServidor.containsKey(portaServidor)) {
                Security.addProvider(new BouncyCastleProvider());
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                SecureRandom sr = new SecureRandom();
                kpg.initialize(2048, sr);
                chavesServidor.put(portaServidor, kpg.generateKeyPair());
            }

            KeyPair kpLocal = chavesServidor.get(portaServidor);
            String deviceId = geradorDeLeituras.getIdDispositivo();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(deviceId);
            oos.writeObject(kpLocal.getPublic());
            oos.flush();
            byte[] dados = baos.toByteArray();

            InetAddress endereco = InetAddress.getByName(ipDestino);
            DatagramPacket pacoteEnvio = new DatagramPacket(dados, dados.length, endereco, portaServidor);
            this.socketUDP.send(pacoteEnvio);

            this.socketUDP.setSoTimeout(5000);
            byte[] buffer = new byte[4096];
            DatagramPacket pacoteResp = new DatagramPacket(buffer, buffer.length);
            this.socketUDP.receive(pacoteResp);

            ByteArrayInputStream bais = new ByteArrayInputStream(pacoteResp.getData(), 0, pacoteResp.getLength());
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object obj = ois.readObject();

            if (obj instanceof PublicKey) {
                PublicKey chavePublicaServidor = (PublicKey) obj;

                KeyPair kp = new KeyPair(chavePublicaServidor, kpLocal.getPrivate());
                chavesServidor.put(portaServidor, kp);

                System.out.println("Handshake UDP concluído com " + ipDestino + ":" + portaServidor);
            } else {
                System.err.println("Resposta inválida do servidor " + ipDestino + " durante handshake.");
            }

        } catch (SocketTimeoutException e) {
            System.err.println("Timeout: O servidor " + ipDestino + ":" + portaServidor + " não respondeu ao handshake.");
        } catch (Exception e) {
            System.err.println("Erro no handshake UDP com " + ipDestino + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected void enviarLeituraAoServidorDeBorda(String textoPlano) {
        KeyPair chaveBorda = chavesServidorDeBorda.get(portaServidorDeBorda);

        if (chaveBorda == null) {
            System.err.println("Chave publica do servidor de borda nao encontrada");
            return;
        }

        PublicKey chavePublicaBorda = chaveBorda.getPublic();
        String idDispositivo = geradorDeLeituras.getIdDispositivo();

        try {
            SecureRandom sr = new SecureRandom();

            byte[] bytesChaveSessao = new byte[32];
            sr.nextBytes(bytesChaveSessao);
            SecretKey chaveSessao = new SecretKeySpec(bytesChaveSessao, "ChaCha20");

            byte[] nonce = new byte[12];
            sr.nextBytes(nonce);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(nonce);

            Cipher chacha = Cipher.getInstance("ChaCha20-Poly1305");
            chacha.init(Cipher.ENCRYPT_MODE, chaveSessao, ivParameterSpec);
            byte[] bytesMensagemEncriptada = chacha.doFinal(textoPlano.getBytes());

            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsa.init(Cipher.ENCRYPT_MODE, chavePublicaBorda);
            byte[] bytesChaveSessaoEncriptada = rsa.doFinal(bytesChaveSessao);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream saida = new ObjectOutputStream(baos);

            saida.writeObject(idDispositivo);
            saida.writeObject(bytesChaveSessaoEncriptada);
            saida.writeObject(nonce);
            saida.writeObject(bytesMensagemEncriptada);
            saida.flush();

            byte[] mensagem = baos.toByteArray();

            if (this.enderecoBorda == null) {
                System.err.println("Erro: Endereço do servidor de borda não foi definido.");
                return;
            }

            InetAddress enderecoDestino = InetAddress.getByName(this.enderecoBorda);

            DatagramPacket pacote = new DatagramPacket(mensagem, mensagem.length, enderecoDestino, portaServidorDeBorda);

            System.out.println("Dispositivo: " + idDispositivo + ", enviando leitura ao servidor de borda: "
                    + enderecoDestino.getHostAddress() + ":" + portaServidorDeBorda);

            socketUDP.send(pacote);

        } catch (IOException e) {
            System.err.println("Erro ao enviar leitura ao servidor de borda: " + portaServidorDeBorda + " - " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erro: algoritmo criptográfico não disponível: " + e.getMessage());
        } catch (NoSuchPaddingException e) {
            System.err.println("Erro: esquema de padding não disponível: " + e.getMessage());
        } catch (InvalidKeyException e) {
            System.err.println("Erro: chave inválida ao inicializar o cifrador: " + e.getMessage());
        } catch (IllegalBlockSizeException e) {
            System.err.println("Erro: tamanho de bloco ilegal (RSA): " + e.getMessage());
        } catch (BadPaddingException e) {
            System.err.println("Erro de padding: " + e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            System.err.println("Erro: parâmetros inválidos (ChaCha20): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro inesperado ao cifrar/enviar a leitura: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
}