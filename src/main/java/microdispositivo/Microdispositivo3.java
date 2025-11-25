package microdispositivo;

import microdispositivo.entities.ImplMicrodispositivo;

public class Microdispositivo3 extends ImplMicrodispositivo {
    public Microdispositivo3(String ipMicrodispositivo, int portaMicrodispositivo, long intervaloMillisGeracao, String idDispositivo, int portaServidorDescoberta, String localizacaoMicrodispositivo) {
        super(ipMicrodispositivo, portaMicrodispositivo,  intervaloMillisGeracao, idDispositivo, portaServidorDescoberta, localizacaoMicrodispositivo);
    }

    public static void main(String[] args) {
        Microdispositivo3 m = new Microdispositivo3(
                "127.0.0.3",
                5003,
                2000,
                "3",
                6000,
                "Nova Betania");
    }
}
