package microdispositivo;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

public class MicrodispositivoInvalido extends ImplMicrodispositivo {

    public MicrodispositivoInvalido(String ipServidorBorda, int portaMicrodispositivo, long intervaloMillisGeracao, String idDispositivo, int portaServidorDescoberta, String localizacaoMicrodispositivo) {
        super(ipServidorBorda, portaMicrodispositivo, intervaloMillisGeracao, idDispositivo, portaServidorDescoberta, localizacaoMicrodispositivo);
    }

    @Override
    protected void realizarHandshakeUDP(String ipDestino, int portaServidor, ConcurrentHashMap<Integer, KeyPair> chavesServidor) {
        // 1. Executa o handshake padrão para garantir que o socket está aberto e o fluxo iniciou
        super.realizarHandshakeUDP(ipDestino, portaServidor, chavesServidor);

        if (!chavesServidor.containsKey(portaServidor)) {
            return;
        }

        try {
            // 2. Gera um par de chaves RSA aleatório (que não pertence ao servidor real)
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048, new SecureRandom());
            KeyPair chaveFalsa = kpg.generateKeyPair();

            chavesServidor.put(portaServidor, chaveFalsa);

        } catch (Exception e) {
            System.err.println("Erro ao gerar chaves falsas: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        MicrodispositivoInvalido m = new MicrodispositivoInvalido(
                "192.168.0.8",
                5005,
                2000,
                "microdispositivo-invalido",
                6000,
                "Centro");
    }
}