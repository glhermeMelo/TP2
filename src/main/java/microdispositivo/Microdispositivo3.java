package microdispositivo;

public class Microdispositivo3 extends ImplMicrodispositivo {
    public Microdispositivo3(String ipServidorBorda, int portaMicrodispositivo, long intervaloMillisGeracao, String idDispositivo, int portaServidorDescoberta, String localizacaoMicrodispositivo) {
        super(ipServidorBorda, portaMicrodispositivo,  intervaloMillisGeracao, idDispositivo, portaServidorDescoberta, localizacaoMicrodispositivo);
    }

    public static void main(String[] args) {
        Microdispositivo3 m = new Microdispositivo3(
                "192.168.0.8",
                5003,
                2000,
                "microdispositivo-3",
                6000,
                "Nova Betania");
    }
}
