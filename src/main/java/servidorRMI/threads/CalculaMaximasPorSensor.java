package servidorRMI.threads;

import entities.RegistroClimatico;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;

public class CalculaMaximasPorSensor implements Runnable {
    private ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGlobais;
    private ConcurrentHashMap<String, RegistroClimatico> maximas;
    private long intervaloMilis;
    private boolean isActive = true;

    public CalculaMaximasPorSensor(ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGlobais, long intervaloMilis) {
        this.maximas = new ConcurrentHashMap<>();
        this.dadosGlobais = dadosGlobais;
        this.intervaloMilis = intervaloMilis;
    }

    @Override
    public void run() {
        while (isActive) {
            try {
                dadosGlobais.forEach((idSensor, listaRegistros) -> {
                    if (listaRegistros != null && !listaRegistros.isEmpty()) {
                        calcularMaximoPorSensor(idSensor, listaRegistros, "temperatura", r -> Double.parseDouble(r.temperatura()));
                        calcularMaximoPorSensor(idSensor, listaRegistros, "umidade", r -> Double.parseDouble(r.umidade()));
                        calcularMaximoPorSensor(idSensor, listaRegistros, "co2", r -> Double.parseDouble(r.cO2()));
                        calcularMaximoPorSensor(idSensor, listaRegistros, "co", r -> Double.parseDouble(r.cO()));
                        calcularMaximoPorSensor(idSensor, listaRegistros, "no2", r -> Double.parseDouble(r.nO2()));
                        calcularMaximoPorSensor(idSensor, listaRegistros, "so2", r -> Double.parseDouble(r.sO2()));
                        calcularMaximoPorSensor(idSensor, listaRegistros, "pm2_5", r -> Double.parseDouble(r.pM2_5()));
                        calcularMaximoPorSensor(idSensor, listaRegistros, "pm10", r -> Double.parseDouble(r.pM10()));
                        calcularMaximoPorSensor(idSensor, listaRegistros, "ruido", r -> Double.parseDouble(r.ruido()));
                        calcularMaximoPorSensor(idSensor, listaRegistros, "radiacaoUV", r -> Double.parseDouble(r.radiacaoUV()));
                    }
                });

                System.out.println("Máximas por sensor atualizadas em: " + LocalDateTime.now());
                Thread.sleep(intervaloMilis);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Erro ao calcular máximas por sensor: " + e.getMessage());
            }
        }
    }

    private void calcularMaximoPorSensor(Integer idSensor, List<RegistroClimatico> lista, String tipoDado, ToDoubleFunction<RegistroClimatico> funcao) {
        try {
            RegistroClimatico maxRegistro = lista.stream()
                    .filter(Objects::nonNull)
                    .max(Comparator.comparingDouble(funcao))
                    .orElse(null);

            if (maxRegistro != null) {
                String chaveUnica = idSensor + "_" + tipoDado;
                maximas.put(chaveUnica, maxRegistro);
            }

        } catch (Exception e) {
            System.err.println("Erro ao calcular máximo para sensor " + idSensor + " (" + tipoDado + "): " + e.getMessage());
        }
    }

    public void parar() {
        this.isActive = false;
    }


    public RegistroClimatico getTemperaturaMaxima(int idSensor) {
        return maximas.get(idSensor + "_temperatura");
    }

    public RegistroClimatico getUmidadeMaxima(int idSensor) {
        return maximas.get(idSensor + "_umidade");
    }

    public RegistroClimatico getCO2Maximo(int idSensor) {
        return maximas.get(idSensor + "_co2");
    }

    public RegistroClimatico getCOMaximo(int idSensor) {
        return maximas.get(idSensor + "_co");
    }

    public RegistroClimatico getNO2Maximo(int idSensor) {
        return maximas.get(idSensor + "_no2");
    }

    public RegistroClimatico getSO2Maximo(int idSensor) {
        return maximas.get(idSensor + "_so2");
    }

    public RegistroClimatico getPM2_5Maximo(int idSensor) {
        return maximas.get(idSensor + "_pm2_5");
    }

    public RegistroClimatico getPM10Maximo(int idSensor) {
        return maximas.get(idSensor + "_pm10");
    }

    public RegistroClimatico getRuidoMaximo(int idSensor) {
        return maximas.get(idSensor + "_ruido");
    }

    public RegistroClimatico getRadiacaoUVMaxima(int idSensor) {
        return maximas.get(idSensor + "_radiacaoUV");
    }
}