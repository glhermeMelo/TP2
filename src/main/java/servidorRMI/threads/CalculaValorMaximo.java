package servidorRMI.threads;

import entities.RegistroClimatico;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;

public class CalculaValorMaximo implements Runnable {
    private ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGLobais;
    private ConcurrentHashMap<String, RegistroClimatico> maximos;
    private long intervaloMilis;
    private boolean isActive = true;

    public  CalculaValorMaximo(ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGlobais, long intervaloMilis) {
        this.maximos = new ConcurrentHashMap<>();
        this.dadosGLobais = dadosGlobais;
        this.intervaloMilis = intervaloMilis;
    }

    @Override
    public void run() {
        while (isActive) {
            try {
                atualizarMaximo("temperatura", r -> Double.parseDouble(r.temperatura()));
                atualizarMaximo("umidade", r -> Double.parseDouble(r.umidade()));
                atualizarMaximo("co2", r -> Double.parseDouble(r.cO2()));
                atualizarMaximo("co", r -> Double.parseDouble(r.cO()));
                atualizarMaximo("no2", r -> Double.parseDouble(r.nO2()));
                atualizarMaximo("so2", r -> Double.parseDouble(r.sO2()));
                atualizarMaximo("pm2_5", r -> Double.parseDouble(r.pM2_5()));
                atualizarMaximo("pm10", r -> Double.parseDouble(r.pM10()));
                atualizarMaximo("ruido", r -> Double.parseDouble(r.ruido()));
                atualizarMaximo("radiacaoUV", r -> Double.parseDouble(r.radiacaoUV()));

            } catch (Exception e) {
                System.err.println("Erro ao calcular maximos: " + e.getMessage());
            }
            try {
                Thread.sleep(intervaloMilis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

    }

    public synchronized void atualizarMaximo(String chave, ToDoubleFunction<RegistroClimatico> funcao) {
        RegistroClimatico registro = dadosGLobais.values().stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .max(Comparator.comparingDouble(funcao))
                .orElse(null);

        if (registro != null) {
            maximos.put(chave, registro);
        }
    }

    public void parar() {
        this.isActive = false;
    }

    public synchronized RegistroClimatico getTemperaturaMaxima() {
        return maximos.get("temperatura");
    }

    public synchronized RegistroClimatico getCO2Maximo() {
        return maximos.get("co2");
    }

    public synchronized RegistroClimatico getCOMaximo() {
        return maximos.get("co");
    }

    public synchronized RegistroClimatico getNO2Maximo() {
        return maximos.get("no2");
    }

    public synchronized RegistroClimatico getSO2Maximo() {
        return maximos.get("so2");
    }

    public synchronized RegistroClimatico getPM2_5Maximo() {
        return maximos.get("pm2_5");
    }

    public synchronized RegistroClimatico getPM10Maximo() {
        return maximos.get("pm10");
    }

    public synchronized RegistroClimatico getUmidadeMaxima() {
        return maximos.get("umidade");
    }

    public synchronized RegistroClimatico getRuidoMaximo() {
        return maximos.get("ruido");
    }

    public synchronized RegistroClimatico getRadiacaoUVMaximo() {
        return maximos.get("radiacaoUV");
    }
}
