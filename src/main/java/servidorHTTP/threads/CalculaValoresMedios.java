package servidorHTTP.threads;

import entities.RegistroClimatico;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;

public class CalculaValoresMedios implements Runnable {
    private ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGLobais;
    private ConcurrentHashMap<String, Double> medias;
    private long intervaloMilis;
    private boolean isActive = true;

    public  CalculaValoresMedios(ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGlobais, long intervaloMilis) {
        this.medias = new ConcurrentHashMap<>();
        this.dadosGLobais = dadosGlobais;
        this.intervaloMilis = intervaloMilis;
    }

    @Override
    public void run() {
        while (isActive) {
            try {
                calculaMedia();
            } catch (Exception e) {
                System.err.println("Erro ao calcular medias: " + e.getMessage());
            }
            try {
                Thread.sleep(intervaloMilis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void calculaMedia() {
        int registros = 0;
        double mediaTemperatura = 0.0;
        double mediaUmidade = 0.0;
        double mediaCO2 = 0.0;
        double mediaCO = 0.0;
        double mediaNO2 = 0.0;
        double mediaSO2 = 0.0;
        double mediaPM2 = 0.0;
        double mediaPM10 = 0.0;
        double mediaRuido = 0.0;
        double mediaRadiacaoUV = 0.0;

        for (List<RegistroClimatico> lista : dadosGLobais.values()) {
            if (lista == null)
                continue;

            for (RegistroClimatico r : lista) {
                if (r == null)
                    continue;

                try {
                    mediaTemperatura += Double.parseDouble(r.temperatura());
                    mediaUmidade += Double.parseDouble(r.umidade());
                    mediaCO2 += Double.parseDouble(r.cO2());
                    mediaCO += Double.parseDouble(r.cO());
                    mediaNO2 += Double.parseDouble(r.nO2());
                    mediaSO2 += Double.parseDouble(r.sO2());
                    mediaPM2 += Double.parseDouble(r.pM2_5());
                    mediaPM10 += Double.parseDouble(r.pM10());
                    mediaRuido += Double.parseDouble(r.ruido());
                    mediaRadiacaoUV += Double.parseDouble(r.radiacaoUV());

                    registros++;

                } catch (NumberFormatException e) {
                    System.err.println("Erro ao converter valores : " + e.getMessage());
                }
            }
        }

        if (registros == 0) {
            medias.put("temperatura", -1.0);
            medias.put("umidade", -1.0);
            medias.put("co2", -1.0);
            medias.put("co", -1.0);
            medias.put("no2", -1.0);
            medias.put("so2", -1.0);
            medias.put("pm2_5", -1.0);
            medias.put("pm10", -1.0);
            medias.put("ruido", -1.0);
            medias.put("radiacaoUV", -1.0);
            return;
        }

        medias.put("temperatura", mediaTemperatura / registros);
        medias.put("umidade", mediaUmidade / registros);
        medias.put("co2", mediaCO2 / registros);
        medias.put("co", mediaCO / registros);
        medias.put("no2", mediaNO2 / registros);
        medias.put("so2", mediaSO2 / registros);
        medias.put("pm2_5", mediaPM2 / registros);
        medias.put("pm10", mediaPM10 / registros);
        medias.put("ruido", mediaRuido / registros);
        medias.put("radiacaoUV", mediaRadiacaoUV / registros);
    }

    public void parar() {
        isActive = false;
    }

    public synchronized Double getTemperaturaMedia() {
        return medias.getOrDefault("temperatura", 0.0);
    }

    public synchronized Double getCO2Medio() {
        return medias.getOrDefault("co2", 0.0);
    }

    public synchronized Double getCOMedio() {
        return medias.getOrDefault("co", 0.0);
    }

    public synchronized Double getNO2Medio() {
        return medias.getOrDefault("no2", 0.0);
    }

    public synchronized Double getSO2Medio() {
        return medias.getOrDefault("so2", 0.0);
    }

    public synchronized Double getPM2_5Medio() {
        return medias.getOrDefault("pm2_5", 0.0);
    }

    public synchronized Double getPM10Medio() {
        return medias.getOrDefault("pm10", 0.0);
    }

    public synchronized Double getUmidadeMedia() {
        return medias.getOrDefault("umidade", 0.0);
    }

    public synchronized Double getRuidoMedio() {
        return medias.getOrDefault("ruido", 0.0);
    }

    public synchronized Double getRadiacaoUVMedio() {
        return medias.getOrDefault("radiacaoUV", 0.0);
    }
}
