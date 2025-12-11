package servidor.datacenter;

import java.util.ArrayList;

public class Datacenter3 {
    public static void main(String[] args) {
        DataCenter dataCenter = new DataCenter(
                9002,
                "192.168.0.7",
                "DT-3",
                8082,
                new ArrayList<>());
    }
}
