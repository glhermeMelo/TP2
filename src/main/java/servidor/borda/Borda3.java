package servidor.borda;

public class Borda3 {
    public static void main(String[] args) {
        ServidorDeBorda servidorDeBorda =
                new ServidorDeBorda(
                        7003,
                        "192.168.0.8",
                        "Borda-3",
                        "192.168.0.8",
                        8000,
                        6500);
    }
}
