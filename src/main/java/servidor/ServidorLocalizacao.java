package servidor;

import servidor.threads.ServidorDeLocalizacaoAceitaClientes;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorLocalizacao extends ImplServidor {
    private ConcurrentHashMap<String, String> localizacaoServidoresDeBorda;
    private ConcurrentHashMap<String, String> servicosRMI;

    public ServidorLocalizacao(int porta, String ip, String nome,
                               ConcurrentHashMap<String, String> servicosRMI) {
        super(porta, ip, nome);
        this.localizacaoServidoresDeBorda = new ConcurrentHashMap<>();
        this.servicosRMI = servicosRMI;
        rodar();
    }

    @Override
    protected void rodar() {
        System.out.println(nome + " iniciado em " + ip + ":" + porta);

        new Thread(this::ouvirTCP).start();

        new Thread(this::ouvirUDP).start();
    }

    private void ouvirTCP() {
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println(nome + " escutando TCP (Clientes) na porta " + porta);
            while (isActive) {
                Socket cliente = serverSocket.accept();
                ServidorDeLocalizacaoAceitaClientes aceitaCliente =
                        new ServidorDeLocalizacaoAceitaClientes(
                                cliente,
                                chavesClientes,
                                localizacaoServidoresDeBorda,
                                servicosRMI);
                new Thread(aceitaCliente).start();
            }
        } catch (IOException e) {
            System.err.println("Erro na thread TCP: " + e.getMessage());
        }
    }

    private void ouvirUDP() {
        try (DatagramSocket socketUDP = new DatagramSocket(porta)) {
            System.out.println(nome + " escutando UDP (Microdispositivos) na porta " + porta);
            byte[] buffer = new byte[8192];

            while (isActive) {
                DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                socketUDP.receive(pacote);

                // Processa cada pacote em uma nova thread para não bloquear o loop
                new Thread(() -> processarPacoteUDP(socketUDP, pacote)).start();
            }
        } catch (IOException e) {
            System.err.println("Erro na thread UDP: " + e.getMessage());
        }
    }

    private void processarPacoteUDP(DatagramSocket socket, DatagramPacket pacote) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(pacote.getData(), 0, pacote.getLength());
             ObjectInputStream ois = new ObjectInputStream(bais)) {

            // Protocolo UDP: Sempre envia ID primeiro
            Object entrada1 = ois.readObject();
            if (!(entrada1 instanceof String)) return;
            String idDispositivo = (String) entrada1;

            Object entrada2 = ois.readObject();

            // === CASO 1: Handshake ===
            if (entrada2 instanceof PublicKey) {
                System.out.println("UDP: Handshake solicitado por " + idDispositivo);
                KeyPair chaves = chavesClientes.computeIfAbsent(idDispositivo, k -> gerarParDeChavesRSA());
                responderUDP(socket, pacote, chaves.getPublic());

                // === CASO 2: Mensagem cifrada com localizacao ===
            } else if (entrada2 instanceof byte[]) {
                System.out.println("UDP: Localização recebida de " + idDispositivo);
                byte[] sessaoEnc = (byte[]) entrada2;

                byte[] nonce = (byte[]) ois.readObject();
                byte[] payloadEnc = (byte[]) ois.readObject();

                // Decifra e obtém o endereço IP:Porta
                String enderecoBorda = decifrarEObterEndereco(idDispositivo, sessaoEnc, nonce, payloadEnc);

                responderUDP(socket, pacote, enderecoBorda);
            }

        } catch (Exception e) {
            System.err.println("Erro processando UDP de " + pacote.getAddress() + ": " + e.getMessage());
            responderUDP(socket, pacote, "ERRO: Falha no processamento do pacote");
        }
    }

    private String decifrarEObterEndereco(String id, byte[] sessaoEnc, byte[] nonce, byte[] payloadEnc) {
        try {
            KeyPair kp = chavesClientes.get(id);
            if (kp == null) {
                System.err.println("Chave não encontrada para " + id);
                return "ERRO: Chave não encontrada (Faça Handshake)";
            }

            // 1. Decifra chave de sessão (RSA)
            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsa.init(Cipher.DECRYPT_MODE, kp.getPrivate());
            byte[] chaveSessao = rsa.doFinal(sessaoEnc);

            // 2. Decifra localização (ChaCha20)
            Cipher chacha = Cipher.getInstance("ChaCha20-Poly1305");
            chacha.init(Cipher.DECRYPT_MODE, new SecretKeySpec(chaveSessao, "ChaCha20"), new IvParameterSpec(nonce));
            String localizacao = new String(chacha.doFinal(payloadEnc));

            // Busca no mapa
            String endereco = localizacaoServidoresDeBorda.get(localizacao);
            if (endereco == null) {
                System.err.println("Localização '" + localizacao + "' não cadastrada.");
                return "ERRO: Localização desconhecida";
            }

            System.out.println("Localização '" + localizacao + "' mapeada para " + endereco);
            return endereco;

        } catch (Exception e) {
            System.err.println("Erro criptografia: " + e.getMessage());
            return "ERRO: Falha na descriptografia";
        }
    }

    private void responderUDP(DatagramSocket socket, DatagramPacket origem, Object resposta) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(resposta);
            oos.flush();
            byte[] dados = baos.toByteArray();

            DatagramPacket envio = new DatagramPacket(dados, dados.length, origem.getAddress(), origem.getPort());
            socket.send(envio);
        } catch (IOException e) {
            System.err.println("Erro ao responder UDP: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        /*
        ConcurrentHashMap<String, String> localizacaoServidoresDeBorda = new ConcurrentHashMap<>();
        localizacaoServidoresDeBorda.put("Alto", "192.168.0.8:7000");
        localizacaoServidoresDeBorda.put("Centro","192.168.0.8:7000");
        localizacaoServidoresDeBorda.put("Nova Betania", "192.168.0.8:7000");
        localizacaoServidoresDeBorda.put("Vingt Rosado", "192.168.0.8:7000");


         */
        ConcurrentHashMap<String, String> servicosRMI = new ConcurrentHashMap<>();
        servicosRMI.put("MonitoramentoClimatico", "localhost:1099");

        new ServidorLocalizacao(6000,
                "192.168.0.8",
                "ServidorLocalizacao", servicosRMI);
    }
}