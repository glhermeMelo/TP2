package servidor.borda;

public class Borda1 {
    public static void main(String[] args) {
        ServidorDeBorda servidorDeBorda =
                new ServidorDeBorda(
                        7001,
                        "192.168.0.7",
                        "Borda-1",
                        "192.168.0.7",
                        6001,
                        8000,
                        6501,
                        999999,
                        7500);
    }
}
