package servidor.borda;

public class Borda1 {
    public static void main(String[] args) {
        ServidorDeBorda servidorDeBorda =
                new ServidorDeBorda(
                        7001,
                        "192.168.0.8",
                        "Borda-1",
                        "192.168.0.8",
                        6000,
                        8000,
                        6500);
    }
}
