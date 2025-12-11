package servidor.datacenter;

import java.util.ArrayList;

public class Datacenter2 {
    public static void main(String[] args) {

        DataCenter dataCenter = new DataCenter(
                9001,
                "192.168.0.7",
                "DT-2",
                8081,
                new ArrayList<>()
        );
    }
}
