package servidorRMI.threads;

import entities.RegistroClimatico;

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
                /*
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
                 */
                atualizarMaximo();
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
    public synchronized void atualizarMaximo() {
        RegistroClimatico maxRegistroTemperatura = null;
        RegistroClimatico maxRegistroUmidade = null;
        RegistroClimatico maxRegistroCO2 = null;
        RegistroClimatico maxRegistroCO = null;
        RegistroClimatico maxRegistroNO2 = null;
        RegistroClimatico maxRegistroSO2 = null;
        RegistroClimatico maxRegistroPM2_5 = null;
        RegistroClimatico maxRegistroPM10 = null;
        RegistroClimatico maxRegistroRuido = null;
        RegistroClimatico maxRegistroRadiacaoUV = null;

        Double temperaturaMaxima = 0D;
        Double umidadeMaxima = 0D;
        Double cO2Maximo = 0D;
        Double cOMaximo = 0D;
        Double nO2Maximo = 0D;
        Double sO2Maximo = 0D;
        Double pM2_5Maximo = 0D;
        Double pM10Maximo = 0D;
        Double ruidoMaximo = 0D;
        Double radiacaoUVMaxima = 0D;

        for (List<RegistroClimatico> lista : dadosGLobais.values()) {
            if (lista == null)
                continue;

            for (RegistroClimatico r : lista) {
                if (r == null)
                    continue;

                try {
                    double temp = Double.parseDouble(r.temperatura());
                    if (temp > temperaturaMaxima) {
                        temperaturaMaxima = temp;
                        maxRegistroTemperatura = r;
                    }

                    double umidade = Double.parseDouble(r.umidade());
                    if (umidade > umidadeMaxima) {
                        umidadeMaxima = umidade;
                        maxRegistroUmidade = r;
                    }

                    double cO2 = Double.parseDouble(r.cO2());
                    if (cO2 > cO2Maximo) {
                        cO2Maximo = cO2;
                        maxRegistroCO2 = r;
                    }

                    double co = Double.parseDouble(r.cO());
                    if (co > cOMaximo) {
                        cOMaximo = co;
                        maxRegistroCO = r;
                    }

                    double nO2 = Double.parseDouble(r.nO2());
                    if (nO2 > nO2Maximo) {
                        nO2Maximo = nO2;
                        maxRegistroNO2 = r;
                    }

                    double sO2 = Double.parseDouble(r.sO2());
                    if (sO2 > sO2Maximo) {
                        sO2Maximo = sO2;
                        maxRegistroSO2 = r;
                    }

                    double pM2_5 = Double.parseDouble(r.pM2_5());
                    if (pM2_5 > pM2_5Maximo) {
                        pM2_5Maximo = pM2_5;
                        maxRegistroPM2_5 = r;
                    }

                    double pm10 = Double.parseDouble(r.pM10());
                    if (pm10 > pM10Maximo) {
                        pM10Maximo = pm10;
                        maxRegistroPM10 = r;
                    }

                    double ruido = Double.parseDouble(r.ruido());
                    if (ruido >  ruidoMaximo) {
                        ruidoMaximo = ruido;
                        maxRegistroRuido = r;
                        }

                    double radicacaoUV = Double.parseDouble(r.radiacaoUV());
                    if (radicacaoUV > radiacaoUVMaxima) {
                        radiacaoUVMaxima = radicacaoUV;
                        maxRegistroRadiacaoUV = r;
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao calcular máximo para sensor: " + e.getMessage());
                }
            }

            if (temperaturaMaxima != null)
                maximos.put("temperatura", maxRegistroTemperatura);

            if (umidadeMaxima != null)
                maximos.put("umidade", maxRegistroUmidade);

            if (cO2Maximo != null)
                maximos.put("co2", maxRegistroCO2);

            if (cOMaximo != null)
                maximos.put("co", maxRegistroCO);

            if (nO2Maximo != null)
                maximos.put("no2", maxRegistroNO2);

            if (sO2Maximo != null)
                maximos.put("so2", maxRegistroSO2);

            if (pM2_5Maximo != null)
                maximos.put("pm2_5", maxRegistroPM2_5);

            if (pM10Maximo != null)
                maximos.put("pm10", maxRegistroPM10);

            if (ruidoMaximo != null)
                maximos.put("ruido", maxRegistroRuido);

            if (radiacaoUVMaxima != null)
                maximos.put("radiacaoUV", maxRegistroRadiacaoUV);
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
