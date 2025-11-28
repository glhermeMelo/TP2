package microdispositivo;

import servidor.ImplServidor;

public class MicrodispositivoInvalido extends ImplMicrodispositivo {
    public MicrodispositivoInvalido(String ipServidorBorda, int portaMicrodispositivo, long intervaloMillisGeracao, String idDispositivo, int portaServidorDescoberta, String localizacaoMicrodispositivo) {
        super(ipServidorBorda, portaMicrodispositivo, intervaloMillisGeracao, idDispositivo, portaServidorDescoberta, localizacaoMicrodispositivo);
    }

    public static void main(String[] args) {
        // Tenta se conectar como se fosse o dispositivo 1, mas com chaves inválidas
        MicrodispositivoInvalido m = new MicrodispositivoInvalido(
                "localhost",
                5555,
                2000,
                "microdispositivo-invalido",
                6000,
                "Centro");
    }
}
