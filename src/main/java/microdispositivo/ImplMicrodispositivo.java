package microdispositivo;

import microdispositivo.threads.GeradorDeLeituras;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    //HashMap para armazenar a porta dos servidores de localizacao e as chaves
    protected ConcurrentHashMap<Integer, KeyPair> chavesServidorLocalizacao;

    //HashMap para armazenar a porta dos servidores de borda e as chaves
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

    //A threadGeradora já para em intervalos por causa do sleep no Runnable
    protected void rodar() {
        //Busca por servidor de Localizacao, troca as chaves e recebe a porta do servidor de borda
        trocarChavesRSA(portaServidorLocalizacao, chavesServidorLocalizacao);
        criptografarLocalizacao();

        if (portaServidorDeBorda <= 0) {
            System.err.println("Porta do servidor de borda inválida!");
            return;
        }

        //Troca as chaves com o servidor de borda
        trocarChavesRSA(portaServidorDeBorda, chavesServidorDeBorda);

        if (threadGeradora == null || !threadGeradora.isAlive()) {
            threadGeradora = new Thread(geradorDeLeituras);
            threadGeradora.start();
        } else {
            System.out.println("Thread geradora rodando");
        }

        //Nova Thread Deamon para ler os valores gerados pelo Gerador e criptografar
        Thread monitora = geraThreadMonitora();
        monitora.start();
    }

    private Thread geraThreadMonitora() {
        Thread monitora = new Thread(() -> {
            try {
                while (geradorDeLeituras.isActive()) {
                    String leituraJson = geradorDeLeituras.getLeitura();
                    System.out.println(leituraJson);

                    enviarLeituraAoServidorDeBorda(leituraJson);

                    Thread.sleep(geradorDeLeituras.getIntervaloMillis());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        monitora.setDaemon(true);
        return monitora;
    }

    //Para o Runnable e depois interrompe a Thread
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

    }

    protected void criptografarLocalizacao() {
        KeyPair kp = chavesServidorLocalizacao.get(portaServidorLocalizacao);

        if (kp == null) {
            System.err.println("Nenhuma chave registrada para a porta de localização: " + portaServidorLocalizacao);
            return;
        }

        PublicKey chavePublicaServidor = kp.getPublic();

        try {
            // 1) gera chave de sessão ChaCha20 (32 bytes)
            SecureRandom sr = new SecureRandom();

            byte[] bytesChaveSessao = new byte[32];

            sr.nextBytes(bytesChaveSessao);

            SecretKey chaveSessao = new SecretKeySpec(bytesChaveSessao, "ChaCha20");

            // 2) cifra a chave de sessão com RSA/OAEP usando a public key do servidor
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");

            cipher.init(Cipher.ENCRYPT_MODE, chavePublicaServidor);

            byte[] bytesEncriptados = cipher.doFinal(bytesChaveSessao);

            // 3) cifra o texto com ChaCha20-Poly1305
            byte[] nonce = new byte[12]; // 96 bits nonce para ChaCha20-Poly1305
            sr.nextBytes(nonce);

            IvParameterSpec parameterSpec = new IvParameterSpec(nonce);
            Cipher chacha = Cipher.getInstance("ChaCha20-Poly1305");

            chacha.init(Cipher.ENCRYPT_MODE, chaveSessao, parameterSpec);

            byte[] bytesTextoPlano = localizacao.getBytes();
            byte[] bytesEncriptadosTextoPlano = chacha.doFinal(bytesTextoPlano);

            // DeviceId — assumimos getter no gerador
            String deviceId = geradorDeLeituras.getIdDispositivo();

            // 4) envia deviceId + sessionKeyEncrypted + nonce + payloadEncrypted ao servidor de localização
            try (Socket socket = new Socket(ip, portaServidorLocalizacao)) {
                ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
                saida.flush(); // envia cabeçalho
                ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());

                // Envia na ordem esperada pelo servidor
                saida.writeObject(deviceId);
                saida.writeObject(bytesEncriptados);              // sessionKeyEncrypted
                saida.writeObject(nonce);
                saida.writeObject(bytesEncriptadosTextoPlano);    // payload
                saida.flush();

                System.out.println("Localização (cifrada) enviada com sucesso para " + ip + ":" + portaServidorLocalizacao);

                //5) recebe resposta do servidor (porta do servidor de borda)
                Object resposta = null;
                try {
                    resposta = entrada.readObject();

                    if (resposta instanceof Integer) {
                        portaServidorDeBorda =  (Integer) resposta;
                        System.out.println("Porta de borda recebida: " + portaServidorDeBorda);
                    } else {
                        System.err.println("Erro ao receber porta do servidor: resposta inválida");
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Erro ao ler resposta do servidor: " + e.getMessage());
                }
            } catch (IOException e) {
                System.err.println("Erro de I/O ao conectar/enviar para o servidor " + ip + ":" + portaServidorLocalizacao + " — verifique se o servidor está rodando e a porta está correta. - " + e.getMessage());
            }
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
        } catch (InvalidAlgorithmParameterException e) {
            System.err.println("Erro: parâmetros inválidos ao inicializar ChaCha20-Poly1305 (nonce/parâmetros). Verifique o tamanho do nonce (deve ser 12 bytes).");
        } catch (Exception e) {
            System.err.println("Erro inesperado ao cifrar/enviar a localização: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected void trocarChavesRSA(int portaServidor, ConcurrentHashMap<Integer, KeyPair> chavesServidor) {
        // 1 - Gerando chaves para o servidor se não existirem
        if (!chavesServidor.containsKey(portaServidor)) {
            Security.addProvider(new BouncyCastleProvider());

            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                SecureRandom sr = new SecureRandom();
                kpg.initialize(2048, sr);
                KeyPair kp = kpg.generateKeyPair();
                chavesServidor.put(portaServidor, kp);
            } catch (NoSuchAlgorithmException e) {
                System.err.println("Erro ao gerar chaves RSA! " + e.getMessage());
            }
        }

        // 2 - Pega chave publica e a chave privada (do par local)
        PublicKey chavePublicaLocal = chavesServidor.get(portaServidor).getPublic();
        PrivateKey chavePrivadaLocal = chavesServidor.get(portaServidor).getPrivate();

        // DeviceId — assumimos getter no gerador
        String deviceId = geradorDeLeituras.getIdDispositivo();

        // 3 - Estabelece conexao (criar output primeiro para evitar deadlock)
        try (Socket socket = new Socket(ip, portaServidor)) {
            ObjectOutputStream saida  = new ObjectOutputStream(socket.getOutputStream());
            saida.flush(); // envia cabeçalho imediatamente
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());

            // 4 - Envia deviceId e a chave publica do cliente para o servidor
            saida.writeObject(deviceId);
            saida.writeObject(chavePublicaLocal);
            saida.flush();

            // 5 - Recebe chave publica do servidor
            Object obj = entrada.readObject();
            if (!(obj instanceof PublicKey)) {
                System.err.println("Resposta inesperada do servidor na troca de chaves (esperado PublicKey).");
                return;
            }
            PublicKey chavePublicaServidor = (PublicKey) obj;

            // 6 - Adiciona a chave publica do servidor e a sua chave privada ao HashMap
            KeyPair kp = new KeyPair(chavePublicaServidor, chavePrivadaLocal);
            chavesServidor.put(portaServidor, kp);

            System.out.println("Troca de chaves RSA concluída com o servidor na porta " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
        }  catch (ClassNotFoundException e) {
            System.err.println("Erro ao receber chave do servidor! " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Erro ao estabelecer conexao com servidor " + portaServidor + "! " + e.getMessage());
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

            //Gerar chave da sessao
            byte[] bytesChaveSessao = new byte[32];
            sr.nextBytes(bytesChaveSessao);
            SecretKey chaveSessao = new SecretKeySpec(bytesChaveSessao, "ChaCha20");

            //Gerar nonce
            byte[] nonce = new byte[12];
            sr.nextBytes(nonce);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(nonce);

            //Cifrar mensagem
            Cipher chacha = Cipher.getInstance("ChaCha20-Poly1305");
            chacha.init(Cipher.ENCRYPT_MODE, chaveSessao, ivParameterSpec);
            byte[] bytesMensagemEncriptada = chacha.doFinal(textoPlano.getBytes());

            //Cifrar a chave da sessao
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

            InetAddress enderecoBorda = InetAddress.getByName(ip);
            DatagramPacket pacote = new DatagramPacket(mensagem, mensagem.length, enderecoBorda, portaServidorDeBorda);

            socketUDP.send(pacote);

        } catch (IOException e) {
            System.err.println("Erro ao enviar leitura ao servidor de borda: " + portaServidorDeBorda + " - " + e.getMessage());
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
        } catch (InvalidAlgorithmParameterException e) {
            System.err.println("Erro: parâmetros inválidos ao inicializar ChaCha20-Poly1305 (nonce/parâmetros). Verifique o tamanho do nonce (deve ser 12 bytes).");
        } catch (Exception e) {
            System.err.println("Erro inesperado ao cifrar/enviar a localização: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
}
