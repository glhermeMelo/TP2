package microdispositivo;

import entities.ImplMicrodispositivo;

public class Microdispositivo1 extends ImplMicrodispositivo {
    public Microdispositivo1(String ipMicrodispositivo, int portaMicrodispositivo, long intervaloMillisGeracao, String idDispositivo, int portaServidorDescoberta, String localizacaoMicrodispositivo) {
        super(ipMicrodispositivo, portaMicrodispositivo,  intervaloMillisGeracao, idDispositivo, portaServidorDescoberta, localizacaoMicrodispositivo);
    }

    public static void main(String[] args) {
        Microdispositivo1 m = new Microdispositivo1(
                "localhost",
                5001,
                2000,
                "1",
                6000,
                "Alto");
    }
}
