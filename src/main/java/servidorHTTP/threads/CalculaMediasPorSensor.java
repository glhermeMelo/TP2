package servidorHTTP.threads;

import entities.RegistroClimatico;

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
                dadosGLobais.forEach((idSensor, listaRegistros) -> {
                    if (listaRegistros != null && !listaRegistros.isEmpty()) {
                        calcularMediaPorSensor(idSensor, listaRegistros, "temperatura");
                        calcularMediaPorSensor(idSensor, listaRegistros, "umidade");
                        calcularMediaPorSensor(idSensor, listaRegistros, "co2");
                        calcularMediaPorSensor(idSensor, listaRegistros, "co");
                        calcularMediaPorSensor(idSensor, listaRegistros, "no2");
                        calcularMediaPorSensor(idSensor, listaRegistros, "so2");
                        calcularMediaPorSensor(idSensor, listaRegistros, "pm2_5");
                        calcularMediaPorSensor(idSensor, listaRegistros, "pm10");
                        calcularMediaPorSensor(idSensor, listaRegistros, "ruido");
                        calcularMediaPorSensor(idSensor, listaRegistros, "radiacaoUV");
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

    private void calcularMediaPorSensor(Integer idSensor, List<RegistroClimatico> lista, String tipoDado) {
        double soma = 0.0;
        int quantidade = 0;

        for (RegistroClimatico r : lista) {
            if (r != null) {
                try {
                    double valor = obterValor(r, tipoDado);
                    soma += valor;
                    quantidade++;
                } catch (NumberFormatException e) {
                    System.err.println("Erro ao converter valor para sensor " + idSensor + " (" + tipoDado + "): " + e.getMessage());
                }
            }
        }

        if (quantidade > 0) {
            double media = soma / quantidade;
            String chaveUnica = idSensor + "_" + tipoDado;
            medias.put(chaveUnica, media);
        }
    }

    private double obterValor(RegistroClimatico r, String tipo) {
        switch (tipo) {
            case "temperatura": return Double.parseDouble(r.temperatura());
            case "umidade": return Double.parseDouble(r.umidade());
            case "co2": return Double.parseDouble(r.cO2());
            case "co": return Double.parseDouble(r.cO());
            case "no2": return Double.parseDouble(r.nO2());
            case "so2": return Double.parseDouble(r.sO2());
            case "pm2_5": return Double.parseDouble(r.pM2_5());
            case "pm10": return Double.parseDouble(r.pM10());
            case "ruido": return Double.parseDouble(r.ruido());
            case "radiacaoUV": return Double.parseDouble(r.radiacaoUV());
            default: return 0.0;
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
