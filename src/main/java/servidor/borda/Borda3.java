package servidor.borda;

public class Borda3 {
    public static void main(String[] args) {
        ServidorDeBorda servidorDeBorda =
                new ServidorDeBorda(
                        7003,
                        "192.168.0.7",
                        "Borda-3",
                        "192.168.0.8",
                        6001,
                        8000,
                        6503,
                        2000,
                        7502);
    }
}
