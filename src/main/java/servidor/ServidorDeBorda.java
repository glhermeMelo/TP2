package servidor;

public class ServidorDeBorda extends ImplServidorBorda {
    private int porta;
    private String ip;


    public ServidorDeBorda(int porta, String ip, String nome) {
        super(porta, ip, nome);
    }

    public static void main(String[] args) {
        ServidorDeBorda servidorDeBorda = new ServidorDeBorda(7000, "localhost", "ServidorDeBorda");
    }
}
