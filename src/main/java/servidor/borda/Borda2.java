package servidor.borda;

public class Borda2 {
    public static void main(String[] args) {
        ServidorDeBorda servidorDeBorda =
                new ServidorDeBorda(
                        7002,
                        "192.168.0.7",
                        "Borda-2",
                        "192.168.0.8",
                        6001,
                        9001,
                        6502,
                        10000,
                        7501);
    }
}
