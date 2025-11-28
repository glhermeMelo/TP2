package microdispositivo;

public class Microdispositivo2 extends ImplMicrodispositivo {
    public Microdispositivo2(String ipServidorBorda, int portaMicrodispositivo, long intervaloMillisGeracao, String idDispositivo, int portaServidorDescoberta, String localizacaoMicrodispositivo) {
        super(ipServidorBorda, portaMicrodispositivo,  intervaloMillisGeracao, idDispositivo, portaServidorDescoberta, localizacaoMicrodispositivo);
    }

    public static void main(String[] args) {
        Microdispositivo2 m = new Microdispositivo2(
                "localhost",
                5002,
                2000,
                "microdispositivo-2",
                6000,
                "Centro");
    }
}
