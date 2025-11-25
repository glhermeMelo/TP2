package microdispositivo;

import entities.ImplMicrodispositivo;

public class Microdispositivo4 extends ImplMicrodispositivo {
    public Microdispositivo4(String ipMicrodispositivo, int portaMicrodispositivo, long intervaloMillisGeracao, String idDispositivo, int portaServidorDescoberta, String localizacaoMicrodispositivo) {
        super(ipMicrodispositivo, portaMicrodispositivo,  intervaloMillisGeracao, idDispositivo, portaServidorDescoberta, localizacaoMicrodispositivo);
    }

    public static void main(String[] args) {
        Microdispositivo4 m = new Microdispositivo4(
                "192.168.0.8",
                5004,
                2000,
                "4",
                6000,
                "Vingt Rosado");
    }
}
