package servidor.datacenter;

import java.util.ArrayList;
import java.util.List;

public class Datacenter1 {
    public static void main(String[] args) {
        List<Integer> portasRepasse = new ArrayList<>();
        portasRepasse.add(9001);
        portasRepasse.add(9002);

        DataCenter dataCenter = new DataCenter(
                9000,
                "192.168.0.7",
                "DT-1",
                8080,
                portasRepasse);
    }
}
