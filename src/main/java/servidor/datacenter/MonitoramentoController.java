package servidor.datacenter;

import entities.RegistroClimatico;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import servidorHTTP.ImplMonitoramentoClimatico;

import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootApplication
@RestController
@RequestMapping("/monitoramento")
public class MonitoramentoController {

    @GetMapping("/sensor/{id}/medias")
    public Map<String, Double> getTodasMediasPorSensor(@PathVariable int id) {
        Map<String, Double> medias = new LinkedHashMap<>();
        medias.put("temperatura", getService().getTemperaturaMediaPorSensor(id));
        medias.put("umidade", getService().getUmidadeMediaPorSensor(id));
        medias.put("co2", getService().getCO2MedioPorSensor(id));
        medias.put("co", getService().getCOMedioPorSensor(id));
        medias.put("no2", getService().getNO2MedioPorSensor(id));
        medias.put("so2", getService().getSO2MedioPorSensor(id));
        medias.put("pm2_5", getService().getPM2_5MedioPorSensor(id));
        medias.put("pm10", getService().getPM10MedioPorSensor(id));
        medias.put("ruido", getService().getRuidoMedioPorSensor(id));
        medias.put("radiacaouv", getService().getRadiacaoUVMediaPorSensor(id));
        return medias;
    }

    @GetMapping("/sensor/{id}/maximos")
    public Map<String, RegistroClimatico> getTodosMaximosPorSensor(@PathVariable int id) {
        Map<String, RegistroClimatico> maximos = new LinkedHashMap<>();
        maximos.put("temperatura", getService().getTemperaturaMaximaPorSensor(id));
        maximos.put("umidade", getService().getUmidadeMaximaPorSensor(id));
        maximos.put("co2", getService().getCO2MaximoPorSensor(id));
        maximos.put("co", getService().getCOMaximoPorSensor(id));
        maximos.put("no2", getService().getNO2MaximoPorSensor(id));
        maximos.put("so2", getService().getSO2MaximoPorSensor(id));
        maximos.put("pm2_5", getService().getPM2_5MaximoPorSensor(id));
        maximos.put("pm10", getService().getPM10MaximoPorSensor(id));
        maximos.put("ruido", getService().getRuidoMaximoPorSensor(id));
        maximos.put("radiacaouv", getService().getRadiacaoUVMaximaPorSensor(id));
        return maximos;
    }

    private ImplMonitoramentoClimatico getService() {
        return DataCenter.getServicoMonitoramento();
    }

    @GetMapping("/maximos/temperatura")
    public RegistroClimatico getTemperaturaMaxima() {
        return getService().getTemperaturaMaxima();
    }

    @GetMapping("/maximos/umidade")
    public RegistroClimatico getUmidadeMaxima() {
        return getService().getUmidadeMaxima();
    }

    @GetMapping("/maximos/co2")
    public RegistroClimatico getCO2Maximo() {
        return getService().getCO2Maximo();
    }

    @GetMapping("/maximos/co")
    public RegistroClimatico getCOMaximo() {
        return getService().getCOMaximo();
    }

    @GetMapping("/maximos/no2")
    public RegistroClimatico getNO2Maximo() {
        return getService().getNO2Maximo();
    }

    @GetMapping("/maximos/so2")
    public RegistroClimatico getSO2Maximo() {
        return getService().getSO2Maximo();
    }

    @GetMapping("/maximos/pm2_5")
    public RegistroClimatico getPM2_5Maximo() {
        return getService().getPM2_5Maximo();
    }

    @GetMapping("/maximos/pm10")
    public RegistroClimatico getPM10Maximo() {
        return getService().getPM10Maximo();
    }

    @GetMapping("/maximos/ruido")
    public RegistroClimatico getRuidoMaximo() {
        return getService().getRuidoMaximo();
    }

    @GetMapping("/maximos/radiacaouv")
    public RegistroClimatico getRadiacaoUVMaxima() {
        return getService().getRadiacaoUVMaxima();
    }

    @GetMapping("/medias/temperatura")
    public Double getTemperaturaMedia() {
        return getService().getTemperaturaMedia();
    }

    @GetMapping("/medias/umidade")
    public Double getUmidadeMedia() {
        return getService().getUmidadeMedia();
    }

    @GetMapping("/medias/co2")
    public Double getCO2Medio() {
        return getService().getCO2Medio();
    }

    @GetMapping("/medias/co")
    public Double getCOMedio() {
        return getService().getCOMedio();
    }

    @GetMapping("/medias/no2")
    public Double getNO2Medio() {
        return getService().getNO2Medio();
    }

    @GetMapping("/medias/so2")
    public Double getSO2Medio() {
        return getService().getSO2Medio();
    }

    @GetMapping("/medias/pm2_5")
    public Double getPM2_5Medio() {
        return getService().getPM2_5Medio();
    }

    @GetMapping("/medias/pm10")
    public Double getPM10Medio() {
        return getService().getPM10Medio();
    }

    @GetMapping("/medias/ruido")
    public Double getRuidoMedio() {
        return getService().getRuidoMedio();
    }

    @GetMapping("/medias/radiacaouv")
    public Double getRadiacaoUVMedia() {
        return getService().getRadiacaoUVMedia();
    }

    @GetMapping("/sensor/{id}/medias/temperatura")
    public Double getTemperaturaMediaPorSensor(@PathVariable int id) {
        return getService().getTemperaturaMediaPorSensor(id);
    }

    @GetMapping("/sensor/{id}/medias/umidade")
    public Double getUmidadeMediaPorSensor(@PathVariable int id) {
        return getService().getUmidadeMediaPorSensor(id);
    }

    @GetMapping("/sensor/{id}/medias/co2")
    public Double getCO2MedioPorSensor(@PathVariable int id) {
        return getService().getCO2MedioPorSensor(id);
    }

    @GetMapping("/sensor/{id}/medias/co")
    public Double getCOMedioPorSensor(@PathVariable int id) {
        return getService().getCOMedioPorSensor(id);
    }

    @GetMapping("/sensor/{id}/medias/no2")
    public Double getNO2MedioPorSensor(@PathVariable int id) {
        return getService().getNO2MedioPorSensor(id);
    }

    @GetMapping("/sensor/{id}/medias/so2")
    public Double getSO2MedioPorSensor(@PathVariable int id) {
        return getService().getSO2MedioPorSensor(id);
    }

    @GetMapping("/sensor/{id}/medias/pm2_5")
    public Double getPM2_5MedioPorSensor(@PathVariable int id) {
        return getService().getPM2_5MedioPorSensor(id);
    }

    @GetMapping("/sensor/{id}/medias/pm10")
    public Double getPM10MedioPorSensor(@PathVariable int id) {
        return getService().getPM10MedioPorSensor(id);
    }

    @GetMapping("/sensor/{id}/medias/ruido")
    public Double getRuidoMedioPorSensor(@PathVariable int id) {
        return getService().getRuidoMedioPorSensor(id);
    }

    @GetMapping("/sensor/{id}/medias/radiacaouv")
    public Double getRadiacaoUVMediaPorSensor(@PathVariable int id) {
        return getService().getRadiacaoUVMediaPorSensor(id);
    }
}