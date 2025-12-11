package microdispositivo;

public class Microdispositivo3 extends ImplMicrodispositivo {
    public Microdispositivo3(String ipServidorBorda, int portaMicrodispositivo, long intervaloMillisGeracao, String idDispositivo, int portaServidorDescoberta, String localizacaoMicrodispositivo) {
        super(ipServidorBorda, portaMicrodispositivo,  intervaloMillisGeracao, idDispositivo, portaServidorDescoberta, localizacaoMicrodispositivo);
    }

    public static void main(String[] args) {
        Microdispositivo3 m = new Microdispositivo3(
                "192.168.0.7",
                5003,
                2000,
                "127.0.0.4",
                6000,
                "Nova Betania");
    }
}
