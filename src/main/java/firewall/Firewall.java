package firewall;

import java.util.ArrayList;
import java.util.List;

public class Firewall {
    public static void main(String[] args) {
        List<String> whitelist = new ArrayList<>();
        whitelist.add("127.0.0");
        whitelist.add("192.0.0");
        whitelist.add("10.0.0");

        FirewallBorda fb1 = new FirewallBorda(
                7500,
                7001,
                "localhost",
                whitelist);

        FirewallBorda fb2 = new FirewallBorda(
                7501,
                7002,
                "localhost",
                whitelist);

        FirewallBorda fb3 = new FirewallBorda(
                7502,
                7003,
                "localhost",
                whitelist);

        FirewallLocalizacao fl = new FirewallLocalizacao(
                6000,
                6001,
                "localhost",
                whitelist);

        new Thread(fb1).start();
        new Thread(fb2).start();
        new Thread(fb3).start();

        new Thread(fl).start();
    }
}
