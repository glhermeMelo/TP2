package servidorRMI.threads;

import entities.RegistroClimatico;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;

public class CalculaMediasPorSensor implements Runnable {
    private ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGLobais;
    private ConcurrentHashMap<String, Double> medias;
    private long intervaloMilis;
    private boolean isActive = true;

    public CalculaMediasPorSensor(ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGlobais, long intervaloMilis) {
        this.medias = new ConcurrentHashMap<>();
        this.dadosGLobais = dadosGlobais;
        this.intervaloMilis = intervaloMilis;
    }

    @Override
    public void run() {
        while (isActive) {
            try {
                // Itera sobre cada sensor (ID) e sua lista de registros
                dadosGLobais.forEach((idSensor, listaRegistros) -> {
                    if (listaRegistros != null && !listaRegistros.isEmpty()) {
                        calcularMediaPorSensor(idSensor, listaRegistros, "temperatura", r -> Double.parseDouble(r.temperatura()));
                        calcularMediaPorSensor(idSensor, listaRegistros, "umidade", r -> Double.parseDouble(r.umidade()));
                        calcularMediaPorSensor(idSensor, listaRegistros, "co2", r -> Double.parseDouble(r.cO2()));
                        calcularMediaPorSensor(idSensor, listaRegistros, "co", r -> Double.parseDouble(r.cO()));
                        calcularMediaPorSensor(idSensor, listaRegistros, "no2", r -> Double.parseDouble(r.nO2()));
                        calcularMediaPorSensor(idSensor, listaRegistros, "so2", r -> Double.parseDouble(r.sO2()));
                        calcularMediaPorSensor(idSensor, listaRegistros, "pm2_5", r -> Double.parseDouble(r.pM2_5()));
                        calcularMediaPorSensor(idSensor, listaRegistros, "pm10", r -> Double.parseDouble(r.pM10()));
                        calcularMediaPorSensor(idSensor, listaRegistros, "ruido", r -> Double.parseDouble(r.ruido()));
                        calcularMediaPorSensor(idSensor, listaRegistros, "radiacaoUV", r -> Double.parseDouble(r.radiacaoUV()));
                    }
                });

                Thread.sleep(intervaloMilis);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Erro ao calcular médias por sensor: " + e.getMessage());
            }
        }
    }

    private void calcularMediaPorSensor(Integer idSensor, List<RegistroClimatico> lista, String tipoDado, ToDoubleFunction<RegistroClimatico> funcao) {
        try {
            double media = lista.stream()
                    .filter(Objects::nonNull)
                    .mapToDouble(funcao)
                    .average()
                    .orElse(0.0);

            String chaveUnica = idSensor + "_" + tipoDado;
            medias.put(chaveUnica, media);

        } catch (NumberFormatException e) {
            System.err.println("Erro ao converter valor para sensor " + idSensor + " (" + tipoDado + "): " + e.getMessage());
        }
    }

    public void parar() {
        this.isActive = false;
    }

    public Double getTemperaturaMedia(int idSensor) {
        return medias.getOrDefault(idSensor + "_temperatura", 0.0);
    }

    public Double getUmidadeMedia(int idSensor) {
        return medias.getOrDefault(idSensor + "_umidade", 0.0);
    }

    public Double getCO2Medio(int idSensor) {
        return medias.getOrDefault(idSensor + "_co2", 0.0);
    }

    public Double getCOMedio(int idSensor) {
        return medias.getOrDefault(idSensor + "_co", 0.0);
    }

    public Double getNO2Medio(int idSensor) {
        return medias.getOrDefault(idSensor + "_no2", 0.0);
    }

    public Double getSO2Medio(int idSensor) {
        return medias.getOrDefault(idSensor + "_so2", 0.0);
    }

    public Double getPM2_5Medio(int idSensor) {
        return medias.getOrDefault(idSensor + "_pm2_5", 0.0);
    }

    public Double getPM10Medio(int idSensor) {
        return medias.getOrDefault(idSensor + "_pm10", 0.0);
    }

    public Double getRuidoMedio(int idSensor) {
        return medias.getOrDefault(idSensor + "_ruido", 0.0);
    }

    public Double getRadiacaoUVMedia(int idSensor) {
        return medias.getOrDefault(idSensor + "_radiacaoUV", 0.0);
    }
}
