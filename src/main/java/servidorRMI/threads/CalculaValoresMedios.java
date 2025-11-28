package servidorRMI.threads;

import entities.RegistroClimatico;

import java.time.LocalDateTime;
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
                calculaMedia("temperatura", r -> Double.parseDouble(r.temperatura()));
                calculaMedia("umidade", r -> Double.parseDouble(r.umidade()));
                calculaMedia("co2", r -> Double.parseDouble(r.cO2()));
                calculaMedia("co", r -> Double.parseDouble(r.cO()));
                calculaMedia("no2", r -> Double.parseDouble(r.nO2()));
                calculaMedia("so2", r -> Double.parseDouble(r.sO2()));
                calculaMedia("pm2_5", r -> Double.parseDouble(r.pM2_5()));
                calculaMedia("pm10", r -> Double.parseDouble(r.pM10()));
                calculaMedia("ruido", r -> Double.parseDouble(r.ruido()));
                calculaMedia("radiacaoUV", r -> Double.parseDouble(r.radiacaoUV()));

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

    private void calculaMedia(String chave, ToDoubleFunction<RegistroClimatico> funcao) {
        double media = dadosGLobais.values().stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .mapToDouble(funcao)
                .average()
                .orElse(-1);

        medias.put(chave, media);
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
