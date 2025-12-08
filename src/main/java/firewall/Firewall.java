package firewall;

import java.util.ArrayList;
import java.util.List;

public class Firewall {
    public static void main(String[] args) {
        List<String> whitelist = new ArrayList<>();
        whitelist.add("127.0.0");
        whitelist.add("192.0.0");
        whitelist.add("10.0.0");

        FirewallBorda fb = new FirewallBorda(
                7000,
                7001,
                "localhost",
                whitelist);

        FirewallLocalizacao fl = new FirewallLocalizacao(
                6000,
                6001,
                "localhost",
                whitelist);

        new Thread(fb).start();
        new Thread(fl).start();
    }
}
