package microdispositivo;

import microdispositivo.entities.ImplMicrodispositivo;

public class Microdispositivo2 extends ImplMicrodispositivo {
    public Microdispositivo2(String ipMicrodispositivo, int portaMicrodispositivo, long intervaloMillisGeracao, String idDispositivo, int portaServidorDescoberta, String localizacaoMicrodispositivo) {
        super(ipMicrodispositivo, portaMicrodispositivo,  intervaloMillisGeracao, idDispositivo, portaServidorDescoberta, localizacaoMicrodispositivo);
    }

    public static void main(String[] args) {
        Microdispositivo2 m = new Microdispositivo2(
                "192.168.0.8",
                5002,
                2000,
                "2",
                6000,
                "Centro");
    }
}
