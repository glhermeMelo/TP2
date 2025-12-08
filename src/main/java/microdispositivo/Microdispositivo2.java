package microdispositivo;

public class Microdispositivo2 extends ImplMicrodispositivo {
    public Microdispositivo2(String ipServidorBorda, int portaMicrodispositivo, long intervaloMillisGeracao, String idDispositivo, int portaServidorDescoberta, String localizacaoMicrodispositivo) {
        super(ipServidorBorda, portaMicrodispositivo,  intervaloMillisGeracao, idDispositivo, portaServidorDescoberta, localizacaoMicrodispositivo);
    }

    public static void main(String[] args) {
        Microdispositivo2 m = new Microdispositivo2(
                "192.168.0.8",
                5002,
                2000,
                "127.0.0.3",
                6000,
                "Centro");
    }
}
