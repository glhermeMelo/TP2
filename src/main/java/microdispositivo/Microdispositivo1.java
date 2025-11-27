package microdispositivo;

public class Microdispositivo1 extends ImplMicrodispositivo {
    public Microdispositivo1(String ipServidorBorda, int portaMicrodispositivo, long intervaloMillisGeracao, String idDispositivo, int portaServidorDescoberta, String localizacaoMicrodispositivo) {
        super(ipServidorBorda, portaMicrodispositivo,  intervaloMillisGeracao, idDispositivo, portaServidorDescoberta, localizacaoMicrodispositivo);
    }

    public static void main(String[] args) {
        Microdispositivo1 m = new Microdispositivo1(
                "10.215.4.132",
                5001,
                2000,
                "1",
                6000,
                "Alto");
    }
}
