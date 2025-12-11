package microdispositivo;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

public class MicrodispositivoInvalido extends ImplMicrodispositivo {

    public MicrodispositivoInvalido(String ipServidorBorda, int portaMicrodispositivo, long intervaloMillisGeracao, String idDispositivo, int portaServidorDescoberta, String localizacaoMicrodispositivo) {
        super(ipServidorBorda, portaMicrodispositivo, intervaloMillisGeracao, idDispositivo, portaServidorDescoberta, localizacaoMicrodispositivo);
    }

    public static void main(String[] args) {
        MicrodispositivoInvalido m = new MicrodispositivoInvalido(
                "192.168.0.7",
                5005,
                2000,
                "185.0.0.1",
                6000,
                "Centro");
    }
}