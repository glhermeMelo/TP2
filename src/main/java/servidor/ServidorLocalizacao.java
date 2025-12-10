package servidor;

import entities.InfoServidorBorda;
import servidor.threads.ServidorDeLocalizacaoAceitaClientes;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorLocalizacao extends ImplServidor {
    private ConcurrentHashMap<String, List<InfoServidorBorda>> localizacaoServidoresDeBorda;
    private ConcurrentHashMap<String, Integer> contadoresRoundRobin;
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

        new Thread(this::monitorarBordas).start();
    }

    private void monitorarBordas() {
        System.out.println("Iniciando monitoramento dos servidores de borda");

        while (isActive) {
            try {
                localizacaoServidoresDeBorda.forEach((local, lista) -> {
                    for (InfoServidorBorda infoServidorBorda : lista) {
                        boolean estadoInicial = infoServidorBorda.isActive();
                        boolean testarServidor = testarConexao(infoServidorBorda.getEndereco());

                        infoServidorBorda.setActive(testarServidor);

                        if (estadoInicial != testarServidor) {
                            System.out.println("[MONITOR] Servidor " + local + " (" + infoServidorBorda.getEndereco() + ") mudou status para: " + (testarServidor ? "ONLINE" : "OFFLINE"));
                        }
                    }
                });
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Erro no monitoramento: " + e.getMessage());
            }
        }
    }

    private boolean testarConexao(String endereco) {
        String[] partes = endereco.split(":");
        if (partes.length != 2)
            return false;

        String ip = partes[0];
        int porta = Integer.parseInt(partes[1]);

        try (Socket socket = new Socket()) {

            socket.connect(new InetSocketAddress(ip, porta), 2000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private synchronized String pegarRoundRobin(String endereco) {
        List<InfoServidorBorda> lista = localizacaoServidoresDeBorda.get(endereco);

        if (lista == null || lista.isEmpty()) {
            return null;
        }

        List<InfoServidorBorda> ativos = new ArrayList<>();

        for (InfoServidorBorda infoServidorBorda : lista) {
            if (infoServidorBorda.isActive()) {
                ativos.add(infoServidorBorda);
            }
        }

        if (ativos.isEmpty()) {
            return null;
        }

        int indice = contadoresRoundRobin.getOrDefault(endereco, 0);

        if (indice >= ativos.size()) {
            indice = 0;
        }

        String escolhido = ativos.get(indice).getEndereco();

        contadoresRoundRobin.put(escolhido, (indice + 1) % ativos.size());

        return escolhido;
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

            //Handshake ===
            if (entrada2 instanceof PublicKey) {
                System.out.println("UDP: Handshake solicitado por " + idDispositivo);
                KeyPair chaves = chavesClientes.computeIfAbsent(idDispositivo, k -> gerarParDeChavesRSA());
                responderUDP(socket, pacote, chaves.getPublic());

                //Mensagem cifrada com localizacao
            } else if (entrada2 instanceof byte[]) {
                System.out.println("UDP: Localização recebida de " + idDispositivo);
                byte[] sessaoEnc = (byte[]) entrada2;

                byte[] nonce = (byte[]) ois.readObject();
                byte[] payloadEnc = (byte[]) ois.readObject();

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

            String endereco = pegarRoundRobin(localizacao);

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
        ConcurrentHashMap<String, String> servicosRMI = new ConcurrentHashMap<>();
        servicosRMI.put("MonitoramentoClimatico", "localhost:1099");

        new ServidorLocalizacao(6001,
                "192.168.0.8",
                "ServidorLocalizacao", servicosRMI);
    }
}