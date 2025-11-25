package servidor;

import java.util.concurrent.ConcurrentHashMap;

public class ServidorLocalizacao extends ImplServidor {
    private int porta;
    private String ip;
    private ConcurrentHashMap<String, Integer> localizacaoServidoresDeBorda;

    public ServidorLocalizacao(int porta, String ip,  ConcurrentHashMap<String, Integer> localizacaoServidoresDeBorda) {
        super(porta, ip, "Servidor1", localizacaoServidoresDeBorda);
    }

    public static void main(String[] args) {
        ConcurrentHashMap<String, Integer> localizacaoServidoresDeBorda = new ConcurrentHashMap<>();
        localizacaoServidoresDeBorda.put("Alto", 7000);
        localizacaoServidoresDeBorda.put("Centro", 7000);
        localizacaoServidoresDeBorda.put("Nova Betania", 7000);
        localizacaoServidoresDeBorda.put("Vingt Rosado", 7000);

        ServidorLocalizacao servidorLocalizacao = new ServidorLocalizacao(6000, "localhost",  localizacaoServidoresDeBorda);

    }
}
